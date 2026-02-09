package org.folio.entlinks.controller;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Durations;
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
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
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
        consumerRecords, kafkaProperties, AuthorityDomainEvent.class, authorityTopic());
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
  void expireAuthorityArchives_positive_shouldExpireExistingArchivesForConsortiumAndMemberTenant(String tenant,
                                                                                                 int expectedCount) {
    //create authority records for consortium tenant
    var authority = createAuthorityForConsortium(2);
    var authorityId = authority.getId();

    // await for the record to be created in both tenants
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
            tryGet(authorityEndpoint(authorityId), tenantHeaders(CENTRAL_TENANT_ID))
                .andExpect(status().isOk()));
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authorityId), tenantHeaders(TENANT_ID))
            .andExpect(status().isOk()));

    //delete record from authority table
    doDelete(authorityEndpoint(authorityId), tenantHeaders(CENTRAL_TENANT_ID));
    getConsumedEvent();

    // wait for the archive to be created
    awaitUntilAsserted(() ->
        assertEquals(authorityId.toString(), databaseHelper.getAuthorityArchive(CENTRAL_TENANT_ID, authorityId)));
    awaitUntilAsserted(() ->
        assertEquals(authorityId.toString(), databaseHelper.getAuthorityArchive(TENANT_ID, authorityId)));

    // verify authority record is deleted
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authorityId), tenantHeaders(CENTRAL_TENANT_ID))
            .andExpect(status().isNotFound()));

    // update AuthorityArchive updated_date field
    var dateInPast = Timestamp.from(Instant.now().minus(8, ChronoUnit.DAYS));
    databaseHelper.updateAuthorityArchiveUpdateDate(CENTRAL_TENANT_ID, authorityId, dateInPast);
    databaseHelper.updateAuthorityArchiveUpdateDate(TENANT_ID, authorityId, dateInPast);

    // trigger endpoint
    doPost(authorityExpireEndpoint(), null, tenantHeaders(tenant));

    //check the archive records count in Central and Member tenants
    awaitUntilAsserted(() -> {
      assertEquals(expectedCount, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, CENTRAL_TENANT_ID,
        "deleted = true"));
      assertEquals(expectedCount, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, TENANT_ID, "deleted = true"));
    });
  }

  //@Disabled("MODELINKS-386")
  @ParameterizedTest
  @CsvSource({"consortium, 0, 1", "test, 1, 1"})
  @DisplayName("DELETE: Should not delete existing local record in Member tenant from the authority archives "
               + "by retention in settings")
  void expireAuthorityArchives_positive_shouldExpireExistingArchivesWithLocalRecordForMemberTenant(
    String tenant, int expectedConsortiumCount, int expectedMemberCount) {

    //mock retention period
    createSourceFile();
    //create authority record for consortium tenant
    var shared = createAuthorityForConsortium(0);
    var sharedId = shared.getId();
    //create local authority record for Member tenant
    var local = createAuthority();
    var localId = local.getId();

    // await for the record to be created in both tenants
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(sharedId), tenantHeaders(CENTRAL_TENANT_ID))
            .andExpect(status().isOk()));
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(localId), tenantHeaders(TENANT_ID))
            .andExpect(status().isOk()));

    //delete records from authority table
    doDelete(authorityEndpoint(sharedId), tenantHeaders(CENTRAL_TENANT_ID));
    getConsumedEvent();
    doDelete(authorityEndpoint(localId), tenantHeaders(TENANT_ID));
    getConsumedEvent();

    // wait for the archive to be created
    awaitUntilAsserted(() ->
        assertEquals(sharedId.toString(),
            databaseHelper.getAuthorityArchive(CENTRAL_TENANT_ID, sharedId)));

    awaitUntilAsserted(() ->
        assertEquals(sharedId.toString(),
            databaseHelper.getAuthorityArchive(TENANT_ID, sharedId)));
    awaitUntilAsserted(() ->
        assertEquals(localId.toString(),
            databaseHelper.getAuthorityArchive(TENANT_ID, localId)));

    // verify authority record is deleted in both tenants
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(sharedId), tenantHeaders(CENTRAL_TENANT_ID))
            .andExpect(status().isNotFound()));

    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(sharedId), tenantHeaders(TENANT_ID))
            .andExpect(status().isNotFound()));
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(localId), tenantHeaders(TENANT_ID))
            .andExpect(status().isNotFound()));

    // update AuthorityArchive updated_date field
    var dateInPast = Timestamp.from(Instant.now().minus(8, ChronoUnit.DAYS));
    databaseHelper.updateAuthorityArchiveUpdateDate(CENTRAL_TENANT_ID, sharedId, dateInPast);
    databaseHelper.updateAuthorityArchiveUpdateDate(TENANT_ID, sharedId, dateInPast);
    databaseHelper.updateAuthorityArchiveUpdateDate(TENANT_ID, localId, dateInPast);

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

  private Authority createAuthority() {
    var entity = authority(1, 0);
    databaseHelper.saveAuthority(TENANT_ID, entity);
    return entity;
  }

  private AuthorityDto createAuthorityForConsortium(int index) {
    var dto = new AuthorityDto()
      .id(AUTHORITY_IDS[index])
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
