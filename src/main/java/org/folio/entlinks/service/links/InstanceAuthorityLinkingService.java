package org.folio.entlinks.service.links;

import static org.apache.commons.collections4.MapUtils.isEmpty;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.folio.entlinks.utils.DateUtils.toTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.entity.projection.LinkCountView;
import org.folio.entlinks.domain.repository.InstanceLinkJdbcRepository;
import org.folio.entlinks.domain.repository.InstanceLinkRepository;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.service.authority.AuthorityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceAuthorityLinkingService {

  private final InstanceLinkRepository instanceLinkRepository;
  private final AuthorityService authorityService;
  private final InstanceLinkJdbcRepository instanceLinkJdbcRepository;

  public List<InstanceAuthorityLink> getLinksByInstanceId(UUID instanceId) {
    log.info("Loading links for [instanceId: {}]", instanceId);
    return instanceLinkRepository.findByInstanceId(instanceId).stream()
        .map(view -> {
          var link = view.getLink();
          link.setAuthorityNaturalId(view.getAuthorityNaturalId());
          return link;
        })
        .toList();
  }

  /**
   * Get paginated links for the given authorityId. Authority naturalId is not populated in the returned links.
   * */
  public Page<InstanceAuthorityLink> getLinksByAuthorityId(UUID authorityId, Pageable pageable) {
    log.info("Loading links for [authorityId: {}, page size: {}, page num: {}]", authorityId,
      pageable.getPageSize(), pageable.getOffset());
    return instanceLinkRepository.findByAuthorityId(authorityId, pageable);
  }

  public List<InstanceAuthorityLink> getLinksByIds(List<Integer> ids) {
    log.info("Retrieving links by ids [{}]", ids);
    var longIds = ids.stream()
      .filter(Objects::nonNull)
      .mapToLong(Integer::longValue)
      .boxed()
      .toList();
    return instanceLinkRepository.findAllById(longIds);
  }

  @Transactional
  public void updateLinks(UUID instanceId, List<InstanceAuthorityLink> incomingLinks) {
    if (log.isDebugEnabled()) {
      log.debug("updateLinks:: for [instanceId: {}, links: {}]", instanceId, incomingLinks);
    } else {
      log.info("updateLinks:: for [instanceId: {}, links amount: {}]", instanceId, incomingLinks.size());
    }

    var authorityIds = incomingLinks.stream()
        .map(InstanceAuthorityLink::getAuthorityId)
        .collect(Collectors.toSet());

    verifyAuthoritiesExist(authorityIds);

    var existedLinks = getLinksByInstanceId(instanceId);
    var linksToDelete = subtract(existedLinks, incomingLinks);
    var linksToSave = getLinksToSave(incomingLinks, existedLinks, linksToDelete);
    instanceLinkRepository.deleteAllInBatch(linksToDelete);
    instanceLinkRepository.saveAll(linksToSave);
  }

  public Map<UUID, Integer> countLinksByAuthorityIds(Set<UUID> authorityIds) {
    if (log.isDebugEnabled()) {
      log.info("Count links for [authority ids: {}]", authorityIds);
    } else {
      log.info("Count links for [authority ids amount: {}]", authorityIds.size());
    }
    return instanceLinkRepository.countLinksByAuthorityIds(authorityIds).stream()
      .collect(Collectors.toMap(LinkCountView::getId, LinkCountView::getTotalLinks));
  }

  @Transactional
  public void deleteByAuthorityIdIn(Set<UUID> authorityIds) {
    if (log.isDebugEnabled()) {
      log.info("Delete links for [authority ids: {}]", authorityIds);
    } else {
      log.info("Delete links for [authority ids amount: {}]", authorityIds.size());
    }
    instanceLinkRepository.deleteByAuthorityIds(authorityIds);
  }

  @Transactional
  public void setActualStatusByAuthorityIds(Collection<UUID> authorityIds) {
    if (log.isDebugEnabled()) {
      log.info("Set actual status for links with [authorityIds: {}]", authorityIds);
    } else {
      log.info("Set actual status for links with [authorityIds amount: {}]", authorityIds.size());
    }
    instanceLinkRepository.updateStatusByAuthorityIds(authorityIds, InstanceAuthorityLinkStatus.ACTUAL, null);
  }

  public List<InstanceAuthorityLink> getLinks(LinkStatus status, OffsetDateTime fromDate,
                                              OffsetDateTime toDate, int limit) {
    log.info("Fetching links for [status: {}, fromDate: {}, toDate: {}, limit: {}]",
        status, fromDate, toDate, limit);

    var linkStatus = status == null ? null : InstanceAuthorityLinkStatus.valueOf(status.getValue());
    var linkFromDate = fromDate == null ? null : toTimestamp(fromDate);
    var linkToDate = toDate == null ? null : toTimestamp(toDate);
    var pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("updatedAt")));

    return instanceLinkRepository.findLinksWithAuthorityNaturalId(linkStatus, linkFromDate, linkToDate, pageable)
        .stream()
        .map(view -> {
          var link = view.getLink();
          link.setAuthorityNaturalId(view.getAuthorityNaturalId());
          return link;
        })
        .toList();
  }

  /**
   * Authorities are not propagated to member tenants.
   * So in case some links are for local bib and shared authority - this method is needed to fill in naturalIds
   * for authorities from consortium central tenant.
   * */
  public void setNaturalIdForSharedAuthority(List<InstanceAuthorityLink> links) {
    var authorityIdsWithoutNaturalId = links.stream()
      .filter(link -> link.getAuthorityNaturalId() == null)
      .map(InstanceAuthorityLink::getAuthorityId).toList();

    if (authorityIdsWithoutNaturalId.isEmpty()) {
      return;
    }

    var naturalIdsData = authorityService.findNaturalIdsByIdInAndDeletedFalseForCentralIfOnMember(
      authorityIdsWithoutNaturalId);

    if (isEmpty(naturalIdsData)) {
      return;
    }

    links.forEach(link ->
      Optional.ofNullable(naturalIdsData.get(link.getAuthorityId()))
        .ifPresent(link::setAuthorityNaturalId));
  }

  @Transactional
  public void updateForReports(UUID jobId, List<LinkUpdateReport> reports) {
    log.info("updateForReports:: [jobId: {}, reports count: {}]", jobId, reports.size());
    log.debug("updateForReports:: [reports: {}]", reports);

    reports.forEach(report -> {
      var linkIds = report.getLinkIds();
      var status = mapReportStatus(report);
      log.debug("Update links status for [status: {}, linkIds: {}, jobId: {}]", status, linkIds, jobId);
      if (CollectionUtils.isNotEmpty(linkIds)) {
        var links = getLinksByIds(linkIds);

        links.forEach(link -> {
          link.setStatus(status);
          link.setErrorCause(StringUtils.trimToNull(report.getFailCause()));
        });

        saveAll(report.getInstanceId(), links);
      }
    });
  }

  private void saveAll(UUID instanceId, List<InstanceAuthorityLink> links) {
    log.info("Save links for [instanceId: {}, links amount: {}]", instanceId, links.size());
    log.debug("Save links for [instanceId: {}, links: {}]", instanceId, links);

    instanceLinkRepository.saveAll(links);
  }

  /**
   * Verification of authorities existence on links update needed since there's no foreign key constraint.
   * */
  private void verifyAuthoritiesExist(Set<UUID> authorityIds) {
    var authoritiesExist = authorityService.authoritiesExist(authorityIds);
    var sharedAuthoritiesExist = authorityService.authoritiesExistForCentralIfOnMember(authorityIds);

    if (isNotEmpty(sharedAuthoritiesExist)) {
      sharedAuthoritiesExist.forEach((key, value) ->
        authoritiesExist.merge(key, value, Boolean::logicalOr));
    }

    var missingIds = authoritiesExist.entrySet().stream()
      .filter(entry -> !entry.getValue())
      .map(Map.Entry::getKey)
      .toList();
    if (!missingIds.isEmpty()) {
      log.warn("verifyAuthoritiesExist:: authorities not found for [ids: {}]", missingIds);
      throw new AuthorityNotFoundException(missingIds);
    }
  }

  /**
   * Find existing links that need to be updated so existing ids are used.
   * Add incoming links that need to be created.
   * */
  private List<InstanceAuthorityLink> getLinksToSave(List<InstanceAuthorityLink> incomingLinks,
                                                     List<InstanceAuthorityLink> existedLinks,
                                                     List<InstanceAuthorityLink> linksToDelete) {
    var linksToCreate = subtract(incomingLinks, existedLinks);
    var linksToUpdate = subtract(existedLinks, linksToDelete);
    var linksToSave = new ArrayList<>(linksToCreate);
    linksToSave.addAll(linksToUpdate);
    return linksToSave;
  }

  private List<InstanceAuthorityLink> subtract(Collection<InstanceAuthorityLink> source,
                                               Collection<InstanceAuthorityLink> target) {
    return new LinkedHashSet<>(source).stream()
      .filter(t -> target.stream().noneMatch(link -> link.isSameLink(t)))
      .toList();
  }

  private InstanceAuthorityLinkStatus mapReportStatus(LinkUpdateReport report) {
    return switch (report.getStatus()) {
      case SUCCESS -> InstanceAuthorityLinkStatus.ACTUAL;
      case FAIL -> InstanceAuthorityLinkStatus.ERROR;
    };
  }
}
