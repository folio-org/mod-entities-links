package org.folio.entlinks.controller.delegate;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.DataStatsMapper;
import org.folio.entlinks.domain.dto.AuthorityControlMetadata;
import org.folio.entlinks.domain.dto.AuthorityStatsDto;
import org.folio.entlinks.domain.dto.AuthorityStatsDtoCollection;
import org.folio.entlinks.domain.dto.LinkAction;
import org.folio.entlinks.domain.entity.AuthorityBase;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.consortium.UserTenantsService;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.entlinks.utils.DateUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.UsersClient;
import org.folio.spring.model.ResultList;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class InstanceAuthorityStatServiceDelegate {

  private static final String NOT_SPECIFIED_SOURCE_FILE = "Not specified";
  private final AuthorityDataStatService dataStatService;
  private final DataStatsMapper dataStatMapper;
  private final UsersClient usersClient;
  private final AuthoritySourceFileRepository sourceFileRepository;
  private final ConsortiumTenantsService tenantsService;
  private final FolioExecutionContext context;
  private final UserTenantsService userTenantsService;

  public AuthorityStatsDtoCollection fetchAuthorityLinksStats(OffsetDateTime fromDate, OffsetDateTime toDate,
                                                              LinkAction action, Integer limit) {
    var authorityStatsCollection = new AuthorityStatsDtoCollection();
    var dataStatList = dataStatService.fetchDataStats(fromDate, toDate, action, limit + 1);
    var centralTenant = userTenantsService.getCentralTenant(context.getTenantId());
    var isMemberTenant = centralTenant.isPresent() && !centralTenant.get().equals(context.getTenantId());
    Set<UUID> sharedAuthorityIds = new HashSet<>();
    if (isMemberTenant) {
      var centralTenantStats = dataStatService.findActualByActionAndDate(fromDate, toDate, action, limit + 1,
          centralTenant.get());
      if (!centralTenantStats.isEmpty()) {
        // collect shared authority ids
        sharedAuthorityIds.addAll(centralTenantStats.stream()
            .map(AuthorityDataStat::getAuthority)
            .map(AuthorityBase::getId)
            .collect(Collectors.toSet()));
        removeDuplications(dataStatList, centralTenantStats);
      }
    }

    log.debug("Retrieved data stat count {}", dataStatList.size());

    if (dataStatList.size() > limit) {
      var nextDate = fromTimestamp(dataStatList.get(limit).getUpdatedAt());
      authorityStatsCollection.setNext(nextDate);
      dataStatList = dataStatList.subList(0, limit);
    }

    var users = getUsers(dataStatList);
    var isCentralTenant = tenantsService.isCentralTenantContext();
    var stats = dataStatList.stream()
      .map(source -> {
        var authorityDataStatDto = dataStatMapper.convertToDto(source);

        if (authorityDataStatDto != null) {
          fillSourceFiles(authorityDataStatDto);
          authorityDataStatDto.setMetadata(getMetadata(users, source));
          var shared = isCentralTenant || isMemberTenant
              && !sharedAuthorityIds.isEmpty()
              && sharedAuthorityIds.contains(source.getAuthority().getId());
          authorityDataStatDto.setShared(shared);
        }
        return authorityDataStatDto;
      })
        .toList();

    return authorityStatsCollection.stats(stats);
  }

  private void removeDuplications(List<AuthorityDataStat> authorityDataStats,
                                  List<AuthorityDataStat> centralTenantStats) {

    for (AuthorityDataStat centralStat : centralTenantStats) {
      boolean isDuplicate = authorityDataStats.stream().anyMatch(stat ->
          Objects.equals(stat.getAuthority().getId(), centralStat.getAuthority().getId())
              && Objects.equals(stat.getAction(), centralStat.getAction())
              && Objects.equals(stat.getHeadingOld(), centralStat.getHeadingOld())
              && Objects.equals(stat.getHeadingNew(), centralStat.getHeadingNew())
              && Objects.equals(stat.getHeadingTypeOld(), centralStat.getHeadingTypeOld())
              && Objects.equals(stat.getHeadingTypeNew(), centralStat.getHeadingTypeNew())
              && Objects.equals(stat.getAuthorityNaturalIdOld(), centralStat.getAuthorityNaturalIdOld())
              && Objects.equals(stat.getAuthorityNaturalIdNew(), centralStat.getAuthorityNaturalIdNew())
              && Objects.equals(stat.getAuthoritySourceFileOld(), centralStat.getAuthoritySourceFileOld())
              && Objects.equals(stat.getAuthoritySourceFileNew(), centralStat.getAuthoritySourceFileNew())
      );
      if (!isDuplicate) {
        authorityDataStats.add(centralStat);
      }
    }
  }

  private AuthorityControlMetadata getMetadata(ResultList<UsersClient.User> userResultList, AuthorityDataStat source) {
    UUID startedByUserId = source.getStartedByUserId();
    AuthorityControlMetadata metadata = new AuthorityControlMetadata();
    metadata.setStartedByUserId(startedByUserId);
    metadata.setStartedAt(DateUtils.fromTimestamp(source.getStartedAt()));
    metadata.setCompletedAt(DateUtils.fromTimestamp(source.getCompletedAt()));
    if (userResultList == null || userResultList.getResult() == null) {
      return metadata;
    }

    var user = userResultList.getResult()
      .stream()
      .filter(u -> UUID.fromString(u.getId()).equals(startedByUserId))
      .findFirst().orElse(null);
    if (user == null) {
      return metadata;
    }

    metadata.setStartedByUserFirstName(user.getPersonal().firstName());
    metadata.setStartedByUserLastName(user.getPersonal().lastName());
    return metadata;
  }

  private ResultList<UsersClient.User> getUsers(List<AuthorityDataStat> dataStatList) {
    String query = getUsersQueryString(dataStatList);
    return query.isEmpty() ? ResultList.empty() : usersClient.query(query);
  }

  private String getUsersQueryString(List<AuthorityDataStat> dataStatList) {
    var userIds = dataStatList.stream()
      .map(AuthorityDataStat::getStartedByUserId)
      .filter(Objects::nonNull)
      .map(UUID::toString)
      .distinct()
      .collect(Collectors.joining(" or "));
    return userIds.isEmpty() ? "" : "id=(" + userIds + ")";
  }

  private String getSourceFileName(String uuid) {
    if (isNotBlank(uuid)) {
      var sourceFile = sourceFileRepository.findById(UUID.fromString(uuid)).orElse(null);
      if (sourceFile != null) {
        return sourceFile.getName();
      }
    }
    return NOT_SPECIFIED_SOURCE_FILE;
  }

  private void fillSourceFiles(AuthorityStatsDto authorityDataStatDto) {
    var sourceFileIdOld = authorityDataStatDto.getSourceFileOld();
    var sourceFileIdNew = authorityDataStatDto.getSourceFileNew();
    authorityDataStatDto.setSourceFileOld(getSourceFileName(sourceFileIdOld));
    authorityDataStatDto.setSourceFileNew(getSourceFileName(sourceFileIdNew));
  }
}
