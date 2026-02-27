package org.folio.entlinks.integration.kafka;

import static java.util.Collections.singletonList;
import static org.folio.entlinks.domain.dto.LinkUpdateReport.StatusEnum.FAIL;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.linksInstanceAuthorityStatsTopic;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.folio.support.base.TestConstants.linksStatsInstanceEndpoint;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.TestDataUtils.Link;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.INSTANCE_AUTHORITY_LINK_TABLE,
  DatabaseHelper.AUTHORITY_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE})
class LinkUpdateReportEventListenerIT extends IntegrationTestBase {

  private static final UUID INSTANCE_ID = UUID.fromString("fea1c418-ba1f-438e-85bb-c6ae1011bf5c");

  private Integer linkId;

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @BeforeEach
  void setUp() {
    var link = Link.of(0, 1, TestDataUtils.NATURAL_IDS[0]);
    var sourceFile = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, sourceFile);
    var authority = TestDataUtils.AuthorityTestData.authority(0, 0);
    databaseHelper.saveAuthority(TENANT_ID, authority);

    doPut(linksInstanceEndpoint(), linksDtoCollection(linksDto(INSTANCE_ID, link)), INSTANCE_ID);
    this.linkId = doGetAndReturn(linksInstanceEndpoint(), InstanceLinkDtoCollection.class, INSTANCE_ID)
      .getLinks().getFirst().getId();
  }

  @Test
  @SneakyThrows
  void shouldHandleEvent_positive() {
    // prepare and send link update report event
    var failCause = "test";
    var event = new LinkUpdateReport()
      .tenant(TENANT_ID)
      .jobId(UUID.randomUUID())
      .instanceId(INSTANCE_ID)
      .status(FAIL)
      .linkIds(singletonList(linkId))
      .failCause(failCause);
    sendKafkaMessage(linksInstanceAuthorityStatsTopic(), event.getJobId().toString(), event);

    assertLinksUpdated(failCause);
  }

  @SneakyThrows
  private void assertLinksUpdated(String failCause) {
    var now = OffsetDateTime.now();
    awaitUntilAsserted(() ->
        doGet(linksStatsInstanceEndpoint(LinkStatus.ERROR, now.minusDays(1), now))
            .andExpect(jsonPath("$.stats", hasSize(1)))
            .andExpect(jsonPath("$.stats[0].errorCause", is(failCause)))
    );
  }
}
