package org.folio.entlinks.controller.delegate;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthorityDataStatMapper;
import org.folio.entlinks.domain.dto.AuthorityControlMetadata;
import org.folio.entlinks.domain.dto.DataStatsDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.integration.internal.AuthoritySourceFilesService;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.entlinks.utils.DateUtils;
import org.folio.spring.tools.client.UsersClient;
import org.folio.spring.tools.model.ResultList;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class InstanceAuthorityStatServiceDelegate {

  private static final String NOT_SPECIFIED_SOURCE_FILE = "Not specified";
  private final AuthorityDataStatService dataStatService;
  private final AuthoritySourceFilesService sourceFilesService;
  private final AuthorityDataStatMapper dataStatMapper;
  private final UsersClient usersClient;

  public DataStatsDtoCollection fetchAuthorityLinksStats(OffsetDateTime fromDate, OffsetDateTime toDate,
                                                         AuthorityDataStatAction action, Integer limit) {
    List<AuthorityDataStat> dataStatList = dataStatService.fetchDataStats(fromDate, toDate, action, limit + 1);

    Optional<AuthorityDataStat> last = Optional.empty();
    if (dataStatList.size() > limit) {
      last = Optional.of(dataStatList.get(limit));
      last.ifPresent(dataStatList::remove);
    }

    String query = getUsersQueryString(dataStatList);
    ResultList<UsersClient.User> userResultList =
      query.isEmpty() ? ResultList.of(0, Collections.emptyList()) : usersClient.query(query);
    var stats = dataStatList.stream()
      .map(source -> {
        AuthorityControlMetadata metadata = getMetadata(userResultList, source);
        var authorityDataStatDto = dataStatMapper.convertToDto(source);

        if (authorityDataStatDto != null) {
          var sourceFileIdOld = authorityDataStatDto.getSourceFileOld();
          var sourceFileIdNew = authorityDataStatDto.getSourceFileNew();
          authorityDataStatDto.setSourceFileOld(getSourceFileName(sourceFileIdOld));
          authorityDataStatDto.setSourceFileNew(getSourceFileName(sourceFileIdNew));
          authorityDataStatDto.setMetadata(metadata);
        }
        return authorityDataStatDto;
      })
      .toList();

    return new DataStatsDtoCollection()
      .stats(stats)
      .next(last.map(authorityDataStat -> DateUtils.fromTimestamp(authorityDataStat.getStartedAt()))
        .orElse(null));
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
      .filter(u -> UUID.fromString(u.id()).equals(startedByUserId))
      .findFirst().orElse(null);
    if (user == null) {
      return metadata;
    }

    metadata.setStartedByUserFirstName(user.personal().firstName());
    metadata.setStartedByUserLastName(user.personal().lastName());
    return metadata;
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
      var sourceFile = sourceFilesService.fetchAuthoritySources().get(UUID.fromString(uuid));
      if (sourceFile != null) {
        return sourceFile.name();
      }
    }
    return NOT_SPECIFIED_SOURCE_FILE;
  }
}
