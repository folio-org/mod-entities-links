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
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  tenants = TENANT_ID)
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

  @Test
  void expireAuthorityArchives_positive() {
    mockExpirationSettingsRequest();
    createSourceFile();
    var authority = createAuthority();

    //delete authority via api
    doDelete(authorityEndpoint(authority.getId()), tenantHeaders(TENANT_ID));
    getConsumedEvent();

    // wait for authority deletion and archive creation
    awaitUntilAsserted(() -> {
      assertEquals(0, databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID));
    });
    awaitUntilAsserted(() -> {
      assertEquals(1, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, TENANT_ID, "deleted = true"));
    });

    // update AuthorityArchive updated_date field
    var dateInPast = Timestamp.from(Instant.now().minus(8, ChronoUnit.DAYS));
    databaseHelper.updateAuthorityArchiveUpdateDate(TENANT_ID, authority.getId(), dateInPast);

    // trigger expiration endpoint
    doPost(authorityExpireEndpoint(), null, tenantHeaders(TENANT_ID));

    awaitUntilAsserted(() -> {
      assertEquals(0, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, TENANT_ID,
        "deleted = true"));
    });
  }

  private void mockExpirationSettingsRequest() {
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
