package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.client.UsersClient;
import org.folio.entlinks.controller.converter.AuthorityDataStatMapper;
import org.folio.entlinks.domain.dto.AuthorityDataStatActionDto;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.spring.test.type.UnitTest;
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

  private @Mock AuthorityDataStatService statService;
  private @Mock AuthorityDataStatMapper mapper;
  private @Mock UsersClient usersClient;

  private @InjectMocks InstanceAuthorityStatServiceDelegate delegate;

  @BeforeEach
  void setUp() {
    delegate = new InstanceAuthorityStatServiceDelegate(statService, mapper, usersClient);
  }

  @Test
  void fetchStats() {
    var userIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    var statData = TestUtils.dataStatList(userIds.get(0), userIds.get(1));
    var users = TestUtils.usersList(userIds);

    var fromDate = OffsetDateTime.of(2022, 10, 10, 15, 30, 30, 0, ZoneOffset.UTC);
    var toDate = OffsetDateTime.now();
    var dataStatActionDto = AuthorityDataStatActionDto.UPDATE_HEADING;

    when(statService.fetchDataStats(fromDate, toDate, dataStatActionDto, 2)).thenReturn(statData);
    when(usersClient.query(anyString())).thenReturn(users);
    AuthorityDataStat authorityDataStat1 = statData.get(0);
    AuthorityDataStat authorityDataStat2 = statData.get(1);
    when(mapper.convertToDto(authorityDataStat1)).thenReturn(TestUtils.getStatDataDto(authorityDataStat1));
    when(mapper.convertToDto(authorityDataStat2)).thenReturn(TestUtils.getStatDataDto(authorityDataStat2));

    var authorityChangeStatDtoCollection = delegate.fetchAuthorityLinksStats(
      fromDate,
      toDate,
      dataStatActionDto,
      2
    );

    var resultUserIds = authorityChangeStatDtoCollection.getStats()
      .stream()
      .map(org.folio.entlinks.domain.dto.AuthorityDataStatDto::getMetadata)
      .map(org.folio.entlinks.domain.dto.Metadata::getStartedByUserId)
      .toList();

    assertNotNull(authorityChangeStatDtoCollection);
    assertNotNull(authorityChangeStatDtoCollection.getStats());
    assertThat(userIds).containsAll(resultUserIds);
  }
}