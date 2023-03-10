package org.folio.entlinks.service.links;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.folio.entlinks.utils.DateUtils.toTimestamp;

import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.entity.projection.LinkCountView;
import org.folio.entlinks.domain.repository.InstanceLinkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceAuthorityLinkingService {

  private static final String SEEK_FIELD = "updatedAt";

  private final InstanceLinkRepository instanceLinkRepository;
  private final AuthorityDataService authorityDataService;

  public List<InstanceAuthorityLink> getLinksByInstanceId(UUID instanceId) {
    log.info("Loading links for [instanceId: {}]", instanceId);
    return instanceLinkRepository.findByInstanceId(instanceId);
  }

  public Page<InstanceAuthorityLink> getLinksByAuthorityId(UUID authorityId, Pageable pageable) {
    log.info("Loading links for [authorityId: {}, page size: {}, page num: {}]", authorityId,
      pageable.getPageSize(), pageable.getOffset());
    return instanceLinkRepository.findByAuthorityId(authorityId, pageable);
  }

  public List<InstanceAuthorityLink> getLinksByIds(List<Integer> ids) {
    log.info("Retrieving links by ids [{}]", ids);
    var longIds = ids.stream()
      .mapToLong(Integer::longValue)
      .boxed()
      .toList();
    return instanceLinkRepository.findAllById(longIds);
  }

  @Transactional
  public void updateLinks(UUID instanceId, List<InstanceAuthorityLink> incomingLinks) {
    if (log.isDebugEnabled()) {
      log.debug("Update links for [instanceId: {}, links: {}]", instanceId, incomingLinks);
    } else {
      log.info("Update links for [instanceId: {}, links amount: {}]", instanceId, incomingLinks.size());
    }

    var authorityDataSet = incomingLinks.stream()
      .map(InstanceAuthorityLink::getAuthorityData)
      .collect(Collectors.toSet());

    var existedAuthorityData = authorityDataService.saveAll(authorityDataSet);

    for (InstanceAuthorityLink incomingLink : incomingLinks) {
      var linkAuthorityData = incomingLink.getAuthorityData();
      var authorityData = existedAuthorityData.get(linkAuthorityData.getId());
      incomingLink.setAuthorityData(authorityData);
    }
    var existedLinks = instanceLinkRepository.findByInstanceId(instanceId);

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
  public void updateStatus(UUID authorityId, InstanceAuthorityLinkStatus status, String errorCause) {
    log.info("Update links [authority id: {}, status: {}, errorCause: {}]", authorityId, status, errorCause);
    instanceLinkRepository.updateStatusAndErrorCauseByAuthorityId(status, trimToNull(errorCause), authorityId);
  }

  @Transactional
  public void deleteByAuthorityIdIn(Set<UUID> authorityIds) {
    if (log.isDebugEnabled()) {
      log.info("Delete links for [authority ids: {}]", authorityIds);
    } else {
      log.info("Delete links for [authority ids amount: {}]", authorityIds.size());
    }
    instanceLinkRepository.deleteByAuthorityIds(authorityIds);
    authorityDataService.markDeleted(authorityIds);
  }

  @Transactional
  public void saveAll(UUID instanceId, List<InstanceAuthorityLink> links) {
    log.info("Save links for [instanceId: {}, links amount: {}]", instanceId, links.size());
    log.debug("Save links for [instanceId: {}, links: {}]", instanceId, links);

    instanceLinkRepository.saveAll(links);
  }

  public List<InstanceAuthorityLink> getLinks(LinkStatus status, OffsetDateTime fromDate,
                                              OffsetDateTime toDate, int limit) {
    log.info("Fetching links for [status: {}, fromDate: {}, toDate: {}, limit: {}]",
      status, fromDate, toDate, limit);

    var linkStatus = status == null ? null : InstanceAuthorityLinkStatus.valueOf(status.getValue());
    var linkFromDate = fromDate == null ? null : toTimestamp(fromDate);
    var linkToDate = toDate == null ? null : toTimestamp(toDate);
    var pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc(SEEK_FIELD)));

    var specification = getSpecFromStatusAndDates(linkStatus, linkFromDate, linkToDate);
    return instanceLinkRepository.findAll(specification, pageable).getContent();
  }

  private List<InstanceAuthorityLink> getLinksToSave(List<InstanceAuthorityLink> incomingLinks,
                                                     List<InstanceAuthorityLink> existedLinks,
                                                     List<InstanceAuthorityLink> linksToDelete) {
    var linksToCreate = subtract(incomingLinks, existedLinks);
    var linksToUpdate = subtract(existedLinks, linksToDelete);
    updateLinksData(incomingLinks, linksToUpdate);
    var linksToSave = new ArrayList<>(linksToCreate);
    linksToSave.addAll(linksToUpdate);
    return linksToSave;
  }

  private void updateLinksData(List<InstanceAuthorityLink> incomingLinks, List<InstanceAuthorityLink> linksToUpdate) {
    linksToUpdate
      .forEach(link -> incomingLinks.stream().filter(l -> l.isSameLink(link)).findFirst()
        .ifPresent(l ->
          link.getAuthorityData().setNaturalId(l.getAuthorityData().getNaturalId())
        ));
  }

  private List<InstanceAuthorityLink> subtract(Collection<InstanceAuthorityLink> source,
                                               Collection<InstanceAuthorityLink> target) {
    return new LinkedHashSet<>(source).stream()
      .filter(t -> target.stream().noneMatch(link -> link.isSameLink(t)))
      .toList();
  }

  private Specification<InstanceAuthorityLink> getSpecFromStatusAndDates(
    InstanceAuthorityLinkStatus status, Timestamp from, Timestamp to) {

    return (root, query, builder) -> {
      var predicates = new LinkedList<>();

      if (status != null) {
        predicates.add(builder.equal(root.get("status"), status));
      }
      if (from != null) {
        predicates.add(builder.greaterThanOrEqualTo(root.get(SEEK_FIELD), from));
      }
      if (to != null) {
        predicates.add(builder.lessThanOrEqualTo(root.get(SEEK_FIELD), to));
      }

      return builder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
