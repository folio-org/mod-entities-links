package org.folio.entlinks.controller.delegate;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.DataStatsMapper;
import org.folio.entlinks.domain.dto.AuthorityControlMetadata;
import org.folio.entlinks.domain.dto.AuthorityStatsDto;
import org.folio.entlinks.domain.dto.AuthorityStatsDtoCollection;
import org.folio.entlinks.domain.dto.LinkAction;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.service.authority.AuthorityService;
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
  private final AuthorityService authorityService;
  private final UserTenantsService userTenantsService;
  private final FolioExecutionContext context;

  public AuthorityStatsDtoCollection fetchAuthorityLinksStats(OffsetDateTime fromDate, OffsetDateTime toDate,
                                                              LinkAction action, Integer limit) {
    var authorityStatsCollection = new AuthorityStatsDtoCollection();
    var dataStatList = dataStatService.fetchDataStats(fromDate, toDate, action, limit + 1);

    log.debug("Retrieved data stat count {}", dataStatList.size());

    if (dataStatList.size() > limit) {
      var nextDate = fromTimestamp(dataStatList.get(limit).getUpdatedAt());
      authorityStatsCollection.setNext(nextDate);
      dataStatList = dataStatList.subList(0, limit);
    }

    var users = getUsers(dataStatList);
    var centralTenant = userTenantsService.getCentralTenant(context.getTenantId());
    var isCentralConsortiumTenant = centralTenant.isPresent() && centralTenant.get().equals(context.getTenantId());
    var isMemberConsortiumTenant = centralTenant.isPresent() && !isCentralConsortiumTenant;
    var authoritiesExistence = authoritiesExistenceForConsortiumMember(isMemberConsortiumTenant, dataStatList);
    var stats = dataStatList.stream()
      .map(source -> {
        var authorityDataStatDto = dataStatMapper.convertToDto(source);
        if (authorityDataStatDto != null) {
          fillSourceFiles(authorityDataStatDto);
          authorityDataStatDto.setMetadata(getMetadata(users, source));
          var shared = isCentralConsortiumTenant
            || isMemberConsortiumTenant
            && !authoritiesExistence.getOrDefault(source.getAuthorityId(), false);
          authorityDataStatDto.setShared(shared);
        }
        return authorityDataStatDto;
      })
      .toList();

    return authorityStatsCollection.stats(stats);
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

  private Map<UUID, Boolean> authoritiesExistenceForConsortiumMember(Boolean isConsortiumMember,
                                                                     List<AuthorityDataStat> dataStats) {
    if (!Boolean.TRUE.equals(isConsortiumMember)) {
      return Collections.emptyMap();
    }
    var authorityIds = dataStats.stream()
      .map(AuthorityDataStat::getAuthorityId)
      .collect(Collectors.toSet());
    return authorityService.authoritiesExist(authorityIds);
  }
}
