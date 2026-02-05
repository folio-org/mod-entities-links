package org.folio.entlinks.integration.kafka;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.folio.support.FileTestUtils.readFile;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.base.TestConstants.DI_AUTHORITY_CREATED_POST_PROCESSING_TOPIC;
import static org.folio.support.base.TestConstants.DI_CREATED_TYPE;
import static org.folio.support.base.TestConstants.DI_CREATE_AUTHORITY_PATH;
import static org.folio.support.base.TestConstants.DI_ERROR_TOPIC;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.dataImportAuthorityCreatedTopic;
import static org.folio.support.base.TestConstants.diAuthorityCreatedPostProcessingTopic;
import static org.folio.support.base.TestConstants.diAuthorityErrorTopic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.BDDSoftAssertions;
import org.awaitility.Durations;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.rest.jaxrs.model.Event;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.base.IntegrationTestBase;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.AUTHORITY_DATA_STAT_TABLE,
  DatabaseHelper.AUTHORITY_TABLE,
  DatabaseHelper.AUTHORITY_ARCHIVE_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE,
})
class DataImportCreateEventListenerIT extends IntegrationTestBase {

  private static final UUID AUTHORITY_CREATE_ID = UUID.fromString("7e3745e6-2d48-4f22-825e-72f3338bfa14");

  private KafkaMessageListenerContainer<String, Event> container;
  private BlockingQueue<ConsumerRecord<String, Event>> consumerRecords;

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @BeforeEach
  void setUp(@Autowired KafkaProperties kafkaProperties) {
    consumerRecords = new LinkedBlockingQueue<>();
    container = createAndStartTestConsumer(consumerRecords, kafkaProperties, Event.class,
        diAuthorityCreatedPostProcessingTopic(), diAuthorityErrorTopic());

    var sourceFile = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, sourceFile);
  }

  @AfterEach
  void tearDown() {
    consumerRecords.clear();
    container.stop();
  }

  @SneakyThrows
  @Test
  void shouldHandleDataImportAuthorityCreatedEvent_positive() {
    // send DI authority created event
    var eventPayload = readFile(DI_CREATE_AUTHORITY_PATH);
    var event = TestDataUtils.diAuthorityEvent(DI_CREATED_TYPE,
        eventPayload, AUTHORITY_CREATE_ID.toString(), TENANT_ID);
    sendKafkaMessage(dataImportAuthorityCreatedTopic(), AUTHORITY_CREATE_ID.toString(), event,
        geKafkaHeaders(AUTHORITY_CREATE_ID.toString()));

    // check sent event fields
    var received = getReceivedEvent();

    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();
    var authorityPayload = received.value().getEventPayload();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_CREATE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_AUTHORITY_CREATED_POST_PROCESSING_TOPIC);
    assertions.then(authorityPayload).as("eventPayload").contains(AUTHORITY_CREATE_ID.toString());
    assertions.assertAll();

    // wait until authority is created
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        doGet(authorityEndpoint(AUTHORITY_CREATE_ID))
            .andExpect(status().isOk())
    );
    // verify created authority
    var content = doGet(authorityEndpoint(AUTHORITY_CREATE_ID)).andReturn().getResponse().getContentAsString();
    var authorityDto = objectMapper.readValue(content, AuthorityDto.class);
    assertNotNull(authorityDto);
    assertEquals(AUTHORITY_CREATE_ID, authorityDto.getId());
  }

  @SneakyThrows
  @Test
  void shouldHandleDataImportAuthorityCreatedEvent_negative_duplicateAuthority() {
    // create authority
    var authority = authority(0, 0);
    authority.setId(AUTHORITY_CREATE_ID);
    databaseHelper.saveAuthority(TENANT_ID, authority);

    // wait until authority is created
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        doGet(authorityEndpoint(AUTHORITY_CREATE_ID))
            .andExpect(status().isOk())
    );
    // send DI authority created event for existing authority
    var eventPayload = readFile(DI_CREATE_AUTHORITY_PATH);
    var event = TestDataUtils.diAuthorityEvent(DI_CREATED_TYPE,
        eventPayload, AUTHORITY_CREATE_ID.toString(), TENANT_ID);
    sendKafkaMessage(dataImportAuthorityCreatedTopic(), AUTHORITY_CREATE_ID.toString(), event,
        geKafkaHeaders(AUTHORITY_CREATE_ID.toString()));

    // check sent DI error event
    var received = getReceivedEvent();

    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_CREATE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_ERROR_TOPIC);
    assertions.then(received.value().getEventPayload())
        .contains("ERROR: duplicate key value violates unique constraint");
    assertions.assertAll();
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, Event> getReceivedEvent() {
    return consumerRecords.poll(20, TimeUnit.SECONDS);
  }
}
