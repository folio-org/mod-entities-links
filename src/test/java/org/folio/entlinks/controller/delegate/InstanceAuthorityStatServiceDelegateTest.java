package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.client.AuthoritySourceFileClient.AuthoritySourceFile;
import org.folio.entlinks.controller.converter.AuthorityDataStatMapper;
import org.folio.entlinks.domain.dto.AuthorityDataStatActionDto;
import org.folio.entlinks.domain.dto.AuthorityDataStatDto;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.integration.internal.AuthoritySourceFilesService;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.spring.test.type.UnitTest;
import org.folio.spring.tools.client.UsersClient;
import org.folio.support.TestDataUtils;
import org.folio.support.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityStatServiceDelegateTest {

  private static final UUID USER_ID_1 = UUID.randomUUID();
  private static final UUID USER_ID_2 = UUID.randomUUID();
  private static final String BASE_URL = "baseUrl";
  private static final String SOURCE_FILE_NAME = "sourceFileName";
  private static final LocalDateTime NOW = LocalDateTime.now();
  private static final OffsetDateTime FROM_DATE = OffsetDateTime.of(NOW.with(LocalTime.MIN), ZoneOffset.UTC);
  private static final OffsetDateTime TO_DATE = OffsetDateTime.of(NOW.with(LocalTime.MAX), ZoneOffset.UTC);
  private static final AuthorityDataStatActionDto DATA_STAT_ACTION_DTO = AuthorityDataStatActionDto.UPDATE_HEADING;
  private static final int LIMIT_SIZE = 2;

  private @Mock AuthorityDataStatService statService;
  private @Mock AuthoritySourceFilesService sourceFilesService;
  private @Mock AuthorityDataStatMapper mapper;
  private @Mock UsersClient usersClient;
  private @InjectMocks InstanceAuthorityStatServiceDelegate delegate;


  @BeforeEach
  void setUp() {
    delegate = new InstanceAuthorityStatServiceDelegate(statService, mapper, usersClient);
    var statData = TestUtils.dataStatList(USER_ID_1, USER_ID_2);
    var users = TestUtils.usersList(List.of(USER_ID_1, USER_ID_2));

    when(statService.fetchDataStats(FROM_DATE, TO_DATE, DATA_STAT_ACTION_DTO, 3)).thenReturn(statData);
    when(usersClient.query(anyString())).thenReturn(users);

    AuthorityDataStat authorityDataStat1 = statData.get(0);
    AuthorityDataStat authorityDataStat2 = statData.get(1);
    var userList = users.getResult();
    when(mapper.convertToDto(authorityDataStat1))
      .thenReturn(TestUtils.getStatDataDto(authorityDataStat1, userList.get(0)));
    when(mapper.convertToDto(authorityDataStat2))
      .thenReturn(TestUtils.getStatDataDto(authorityDataStat2, userList.get(0)));
  }

  @Test
  void fetchStats() {
    //  GIVEN
    AuthoritySourceFile sourceFile = new AuthoritySourceFile(USER_ID_1, BASE_URL, SOURCE_FILE_NAME);
    Map<UUID, AuthoritySourceFile> expectedMap = new HashMap<>();
    expectedMap.put(sourceFile.id(), sourceFile);

    //  WHEN
    when(sourceFilesService.fetchAuthoritySources()).thenReturn(expectedMap);



    var userIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    var statData = List.of(
      TestDataUtils.authorityDataStat(userIds.get(0), AuthorityDataStatAction.UPDATE_HEADING),
      TestDataUtils.authorityDataStat(userIds.get(1), AuthorityDataStatAction.UPDATE_HEADING)
    );
    var users = TestDataUtils.usersList(userIds);

    var fromDate = OffsetDateTime.of(2022, 10, 10, 15, 30, 30, 0, ZoneOffset.UTC);
    var toDate = OffsetDateTime.now();
    var dataStatActionDto = AuthorityDataStatActionDto.UPDATE_HEADING;

    when(statService.fetchDataStats(fromDate, toDate, dataStatActionDto, 3)).thenReturn(statData);
    when(usersClient.query(anyString())).thenReturn(users);

    AuthorityDataStat authorityDataStat1 = statData.get(0);
    AuthorityDataStat authorityDataStat2 = statData.get(1);
    var userList = users.getResult();
    when(mapper.convertToDto(authorityDataStat1))
      .thenReturn(TestDataUtils.getStatDataDto(authorityDataStat1, userList.get(0)));
    when(mapper.convertToDto(authorityDataStat2))
      .thenReturn(TestDataUtils.getStatDataDto(authorityDataStat2, userList.get(0)));

    var authorityChangeStatDtoCollection = delegate.fetchAuthorityLinksStats(
      FROM_DATE, TO_DATE, DATA_STAT_ACTION_DTO, LIMIT_SIZE
    );

    //  THEN
    assertNotNull(authorityChangeStatDtoCollection);
    assertNotNull(authorityChangeStatDtoCollection.getStats());
    assertEquals(LIMIT_SIZE, authorityChangeStatDtoCollection.getStats().size());
    var resultStatDtos = authorityChangeStatDtoCollection.getStats();
    for (AuthorityDataStatDto statDto : resultStatDtos) {
      assertNotNull(statDto.getAction());
      assertNotNull(statDto.getAuthorityId());
      assertNotNull(statDto.getHeadingNew());
      assertNotNull(statDto.getHeadingOld());
      assertNotNull(statDto.getHeadingTypeNew());
      assertNotNull(statDto.getHeadingTypeOld());
      assertNotNull(statDto.getLbFailed());
      assertNotNull(statDto.getLbTotal());
      assertNotNull(statDto.getLbUpdated());
      assertNotNull(statDto.getMetadata());
      assertNotNull(statDto.getMetadata().getStartedByUserId());
      assertNotNull(statDto.getMetadata().getStartedByUserFirstName());
      assertNotNull(statDto.getMetadata().getStartedByUserLastName());
      assertNotNull(statDto.getMetadata().getStartedAt());
      assertNotNull(statDto.getMetadata().getCompletedAt());
      assertNotNull(statDto.getNaturalIdNew());
      assertNotNull(statDto.getNaturalIdOld());
      assertNotNull(sourceFile.name());
      assertNotNull(statDto.getSourceFileOld());
    }

    var resultUserIds = authorityChangeStatDtoCollection.getStats()
      .stream()
      .map(org.folio.entlinks.domain.dto.AuthorityDataStatDto::getMetadata)
      .map(org.folio.entlinks.domain.dto.Metadata::getStartedByUserId)
      .toList();
    assertNull(authorityChangeStatDtoCollection.getNext());
    assertThat(List.of(USER_ID_1, USER_ID_2)).containsAll(resultUserIds);
  }

  @Test
  void fetchStats_withoutSourceFile() {
    //  GIVEN
    Map<UUID, AuthoritySourceFile> expectedMap = new HashMap<>();

    //  WHEN
    when(sourceFilesService.fetchAuthoritySources()).thenReturn(expectedMap);
    var authorityChangeStatDtoCollection = delegate
      .fetchAuthorityLinksStats(FROM_DATE, TO_DATE, DATA_STAT_ACTION_DTO, LIMIT_SIZE);

    //  THEN
    assertNotNull(authorityChangeStatDtoCollection);
    assertNotNull(authorityChangeStatDtoCollection.getStats());
    assertEquals(LIMIT_SIZE, authorityChangeStatDtoCollection.getStats().size());

    var resultUserIds = authorityChangeStatDtoCollection.getStats()
      .stream()
      .map(org.folio.entlinks.domain.dto.AuthorityDataStatDto::getMetadata)
      .map(org.folio.entlinks.domain.dto.Metadata::getStartedByUserId)
      .toList();
    assertNull(authorityChangeStatDtoCollection.getNext());
    assertThat(List.of(USER_ID_1, USER_ID_2)).containsAll(resultUserIds);
  }
}
