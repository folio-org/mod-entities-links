package org.folio.entlinks.controller;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.folio.entlinks.domain.dto.LinkAction.UPDATE_HEADING;
import static org.folio.support.DatabaseHelper.AUTHORITY_ARCHIVE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_DATA_STAT_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_TABLE;
import static org.folio.support.DatabaseHelper.INSTANCE_AUTHORITY_LINK_TABLE;
import static org.folio.support.MatchUtils.statsMatch;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.TestDataUtils.NATURAL_IDS;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.TestDataUtils.stats;
import static org.folio.support.base.TestConstants.CENTRAL_TENANT_ID;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.UPDATER_USER_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.authorityStatsEndpoint;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.folio.support.base.TestConstants.linksStatsInstanceEndpoint;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.awaitility.Durations;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.TestDataUtils;
import org.folio.support.TestDataUtils.Link;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
@DatabaseCleanup(tables = {
  AUTHORITY_SOURCE_FILE_CODE_TABLE,
  AUTHORITY_DATA_STAT_TABLE,
  INSTANCE_AUTHORITY_LINK_TABLE,
  AUTHORITY_TABLE,
  AUTHORITY_ARCHIVE_TABLE,
  AUTHORITY_SOURCE_FILE_TABLE},
  tenants = {CENTRAL_TENANT_ID, TENANT_ID})
class InstanceAuthorityLinkStatisticsConsortiumIT extends IntegrationTestBase {

  private static final UUID CENTRAL_AUTHORITY_ID = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");
  private static final UUID MEMBER_AUTHORITY_ID = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f326e");
  private static final UUID CENTRAL_INSTANCE_ID = UUID.fromString("68de093d-8c0d-44c2-b3a8-79393f6cb196");
  private static final UUID MEMBER_INSTANCE_ID = UUID.fromString("e083463e-96d4-4fa0-8ee1-13bfd4f674cf");
  private static final String CENTRAL_INSTANCE_TITLE = "title4";
  private static final String MEMBER_INSTANCE_TITLE = "title2";

  private static final OffsetDateTime TO_DATE = OffsetDateTime.of(LocalDateTime.now().plusHours(1), ZoneOffset.UTC);
  private static final OffsetDateTime FROM_DATE = TO_DATE.minusMonths(1);

  private BibStatsDtoCollection memberStats;
  private BibStatsDtoCollection centralStats;

  @BeforeAll
  static void prepare() {
    setUpConsortium(CENTRAL_TENANT_ID, List.of(TENANT_ID), true);
  }

  @BeforeEach
  void setup() {
    var sourceFile = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    databaseHelper.saveAuthoritySourceFile(CENTRAL_TENANT_ID, sourceFile);
    var centralAuthority = authority(0, 0);
    centralAuthority.setId(CENTRAL_AUTHORITY_ID);
    databaseHelper.saveAuthority(CENTRAL_TENANT_ID, centralAuthority);

    databaseHelper.saveAuthoritySourceFile(TENANT_ID, sourceFile);
    var localAuthority = authority(1, 0);
    localAuthority.setId(MEMBER_AUTHORITY_ID);
    databaseHelper.saveAuthority(TENANT_ID, localAuthority);

    var localLink = new Link(MEMBER_AUTHORITY_ID, TestDataUtils.Link.TAGS[1], NATURAL_IDS[1]);
    var localLinkOnSharedAuthority = new Link(CENTRAL_AUTHORITY_ID, TestDataUtils.Link.TAGS[2], NATURAL_IDS[0]);
    var localLinks = linksDto(MEMBER_INSTANCE_ID, localLink, localLinkOnSharedAuthority);
    doPut(linksInstanceEndpoint(), linksDtoCollection(localLinks),
      tenantHeaders(TENANT_ID), MEMBER_INSTANCE_ID).andReturn();
    this.memberStats = stats(localLinks, null, null, MEMBER_INSTANCE_TITLE);
    var sharedLink = new Link(CENTRAL_AUTHORITY_ID, TestDataUtils.Link.TAGS[0], NATURAL_IDS[0]);
    var sharedLinks = linksDto(CENTRAL_INSTANCE_ID, sharedLink);
    doPut(linksInstanceEndpoint(), linksDtoCollection(sharedLinks),
      tenantHeaders(CENTRAL_TENANT_ID), CENTRAL_INSTANCE_ID).andReturn();
    this.centralStats = stats(sharedLinks, null, null, CENTRAL_INSTANCE_TITLE);
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_positive_member() {
    performAuthorityUpdateScenario();
    doGet(authorityStatsEndpoint(UPDATE_HEADING, FROM_DATE, TO_DATE, 2), tenantHeaders(TENANT_ID))
      .andExpect(status().is2xxSuccessful())
      .andExpect(jsonPath("$.stats[0].shared", is(false)))
      .andExpect(jsonPath("$.stats[0].authorityId", is(MEMBER_AUTHORITY_ID.toString())))
      .andExpect(jsonPath("$.stats[1].shared", is(true)))
      .andExpect(jsonPath("$.stats[1].authorityId", is(CENTRAL_AUTHORITY_ID.toString())));
  }

  @Test
  @SneakyThrows
  void getAuthDataStat_positive_central() {
    performAuthorityUpdateScenario();
    doGet(authorityStatsEndpoint(UPDATE_HEADING, FROM_DATE, TO_DATE, 2), tenantHeaders(CENTRAL_TENANT_ID))
      .andExpect(status().is2xxSuccessful())
      .andExpect(jsonPath("$.stats[0].shared", is(true)))
      .andExpect(jsonPath("$.stats[0].authorityId", is(CENTRAL_AUTHORITY_ID.toString())));
  }

  @Test
  @SneakyThrows
  void getLinkedBibUpdateStats_positive_member() {
    performLinkedBibStatsScenario(false, memberStats);
  }

  @Test
  @SneakyThrows
  void getLinkedBibUpdateStats_positive_central() {
    performLinkedBibStatsScenario(true, centralStats);
  }

  @SneakyThrows
  private void performLinkedBibStatsScenario(boolean centralTenant, BibStatsDtoCollection expectedStats) {
    var tenant = centralTenant ? CENTRAL_TENANT_ID : TENANT_ID;

    perform(getStatsRequest(tenant))
      .andExpect(statsMatch(hasSize(expectedStats.getStats().size())))
      .andExpect(statsMatch(expectedStats));
  }

  private void performAuthorityUpdateScenario() {
    sendAuthorityUpdateEvent(CENTRAL_TENANT_ID, CENTRAL_AUTHORITY_ID);
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
      assertEquals(1, databaseHelper.countRows(AUTHORITY_DATA_STAT_TABLE, CENTRAL_TENANT_ID))
    );

    sendAuthorityUpdateEvent(TENANT_ID, MEMBER_AUTHORITY_ID);
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
      assertEquals(2, databaseHelper.countRows(AUTHORITY_DATA_STAT_TABLE, TENANT_ID))
    );
  }

  private MockHttpServletRequestBuilder getStatsRequest(String tenant) {
    var toDate = OffsetDateTime.now().plusDays(1);
    var fromDate = toDate.minusDays(2);
    var builder = get(linksStatsInstanceEndpoint(LinkStatus.ACTUAL, fromDate, toDate));
    var headers = tenantHeaders(tenant);
    return builder.headers(headers);
  }

  @SneakyThrows
  private void sendAuthorityUpdateEvent(String tenant, UUID authorityId) {
    HttpHeaders headers = tenantHeaders(tenant);
    var content = doGet(authorityEndpoint(authorityId), headers).andReturn().getResponse().getContentAsString();
    var authorityDto = objectMapper.readValue(content, AuthorityDto.class);
    authorityDto.setPersonalName(authorityDto.getPersonalName() + " updated");
    authorityDto.setVersion(2);
    headers.put(XOkapiHeaders.USER_ID, List.of(UPDATER_USER_ID));
    tryPut(authorityEndpoint(authorityDto.getId()), authorityDto, headers)
      .andExpect(status().isNoContent());
  }
}

