package org.folio.entlinks.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.support.DatabaseHelper.AUTHORITY_ARCHIVE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_DATA_STAT_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_TABLE;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.AUTHORITY_IDS;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.TestDataUtils.AuthorityTestData.authoritySourceFile;
import static org.folio.support.base.TestConstants.CENTRAL_TENANT_ID;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.authorityExpireEndpoint;
import static org.folio.support.base.TestConstants.authorityTopic;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

@IntegrationTest
@DatabaseCleanup(tables = {
  AUTHORITY_SOURCE_FILE_CODE_TABLE,
  AUTHORITY_DATA_STAT_TABLE,
  AUTHORITY_TABLE,
  AUTHORITY_ARCHIVE_TABLE,
  AUTHORITY_SOURCE_FILE_TABLE},
    tenants = {CENTRAL_TENANT_ID, TENANT_ID})
class AuthorityControllerEcsIT extends IntegrationTestBase {
  private KafkaMessageListenerContainer<String, AuthorityDomainEvent> container;
  private BlockingQueue<ConsumerRecord<String, AuthorityDomainEvent>> consumerRecords;

  @BeforeAll
  static void prepare() {
    setUpConsortium(CENTRAL_TENANT_ID, List.of(TENANT_ID), true);
  }

  @BeforeEach
  void setUp(@Autowired KafkaProperties kafkaProperties) {
    consumerRecords = new LinkedBlockingQueue<>();
    container = createAndStartTestConsumer(
        authorityTopic(), consumerRecords, kafkaProperties, AuthorityDomainEvent.class);
  }

  @AfterEach
  void tearDown() {
    consumerRecords.clear();
    container.stop();
  }

  @ParameterizedTest
  @CsvSource({"consortium, 0", "test, 1"})
  @DisplayName("DELETE: Should delete existing authority archives by retention in settings "
      + "for Consortium and Member tenants")
  void expireAuthorityArchives_positive_shouldExpireExistingArchivesForConsortiumAndMemberTenant(
      String tenant, int expectedCount) {

    //mock retention period
    mockFailedSettingsRequest();

    //create authority records for consortium tenant
    var authority = createAuthorityForConsortium();

    var count = 1;
    awaitUntilAsserted(() -> {
      assertEquals(count, databaseHelper.countRows(AUTHORITY_TABLE, CENTRAL_TENANT_ID));
      assertEquals(count, databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID));
    });

    //delete records from authority table
    doDelete(authorityEndpoint(authority.getId()), tenantHeaders(CENTRAL_TENANT_ID));
    getConsumedEvent();

    // wait for the archive to be created
    var count1 = 1;
    awaitUntilAsserted(() -> {
      assertEquals(count1, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, CENTRAL_TENANT_ID, "deleted = true"));
      assertEquals(count1, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, TENANT_ID, "deleted = true"));
    });

    awaitUntilAsserted(() -> assertEquals(0, databaseHelper.countRows(AUTHORITY_TABLE, CENTRAL_TENANT_ID)));

    // update AuthorityArchive updated_date field
    var dateInPast = Timestamp.from(Instant.now().minus(8, ChronoUnit.DAYS));
    databaseHelper.updateAuthorityArchiveUpdateDate(CENTRAL_TENANT_ID, authority.getId(), dateInPast);
    databaseHelper.updateAuthorityArchiveUpdateDate(TENANT_ID, authority.getId(), dateInPast);

    // trigger endpoint
    doPost(authorityExpireEndpoint(), null, tenantHeaders(tenant));

    //check the archive records count in Central and Member tenants
    awaitUntilAsserted(() -> {
      assertEquals(expectedCount, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, CENTRAL_TENANT_ID,
          "deleted = true"));
      assertEquals(expectedCount, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, TENANT_ID, "deleted = true"));
    });
  }

  @ParameterizedTest
  @CsvSource({"consortium, 0, 1", "test, 1, 1"})
  @DisplayName("DELETE: Should not delete existing local record in Member tenant from the authority archives "
      + "by retention in settings")
  void expireAuthorityArchives_positive_shouldExpireExistingArchivesWithLocalRecordForMemberTenant(
      String tenant, int expectedConsortiumCount, int expectedMemberCount) {

    //mock retention period
    mockFailedSettingsRequest();
    createSourceFile();
    //create authority record for consortium tenant
    var authority1 = createAuthorityForConsortium();
    //create local authority record for Member tenant
    var authority2 = createAuthority();

    //delete records from authority table
    doDelete(authorityEndpoint(authority1.getId()), tenantHeaders(CENTRAL_TENANT_ID));
    getConsumedEvent();
    doDelete(authorityEndpoint(authority2.getId()), tenantHeaders(TENANT_ID));
    getConsumedEvent();

    // wait for the archive to be created
    awaitUntilAsserted(() -> {
      assertEquals(1, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, CENTRAL_TENANT_ID, "deleted = true"));
      assertEquals(2, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, TENANT_ID, "deleted = true"));
    });

    awaitUntilAsserted(() -> {
      assertEquals(0, databaseHelper.countRows(AUTHORITY_TABLE, CENTRAL_TENANT_ID));
      assertEquals(0, databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID));
    });

    // update AuthorityArchive updated_date field
    var dateInPast = Timestamp.from(Instant.now().minus(8, ChronoUnit.DAYS));
    databaseHelper.updateAuthorityArchiveUpdateDate(CENTRAL_TENANT_ID, authority1.getId(), dateInPast);
    databaseHelper.updateAuthorityArchiveUpdateDate(TENANT_ID, authority1.getId(), dateInPast);
    databaseHelper.updateAuthorityArchiveUpdateDate(TENANT_ID, authority2.getId(), dateInPast);

    // trigger endpoint
    doPost(authorityExpireEndpoint(), null, tenantHeaders(tenant));

    //check the archive records count in Central and Member tenants
    awaitUntilAsserted(() -> {
      assertEquals(expectedConsortiumCount, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, CENTRAL_TENANT_ID,
          "deleted = true"));
      assertEquals(expectedMemberCount, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, TENANT_ID,
          "deleted = true"));
    });
  }

  private void mockFailedSettingsRequest() {
    okapi.wireMockServer().stubFor(get(urlPathEqualTo("/settings/entries"))
        .withQueryParam("query", equalTo("(scope=authority-storage AND key=authority-archives-expiration)"))
        .withQueryParam("limit", equalTo("10000"))
        .willReturn(aResponse().withStatus(500)));
  }

  private Authority createAuthority() {
    var entity = authority(1, 0);
    databaseHelper.saveAuthority(TENANT_ID, entity);
    return entity;
  }

  private AuthorityDto createAuthorityForConsortium() {
    var dto = new AuthorityDto()
        .id(AUTHORITY_IDS[0])
        .version(0)
        .source("MARC")
        .naturalId("ns123456")
        .personalName("Nikola Tesla1");
    doPost(authorityEndpoint(), dto, tenantHeaders(CENTRAL_TENANT_ID));
    return dto;
  }

  private void createSourceFile() {
    var entity = authoritySourceFile(0);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, entity);

    entity.getAuthoritySourceFileCodes().forEach(code ->
        databaseHelper.saveAuthoritySourceFileCode(TENANT_ID, entity.getId(), code));
  }

  @SneakyThrows
  private void getConsumedEvent() {
    consumerRecords.poll(10, TimeUnit.SECONDS);
  }
}
