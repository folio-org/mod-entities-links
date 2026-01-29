package org.folio.entlinks.integration.kafka;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.folio.support.FileTestUtils.readFile;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.base.TestConstants.DI_AUTHORITY_CREATED_POST_PROCESSING_TOPIC;
import static org.folio.support.base.TestConstants.DI_AUTHORITY_UPDATED_TOPIC;
import static org.folio.support.base.TestConstants.DI_COMPLETED_TOPIC;
import static org.folio.support.base.TestConstants.DI_CREATED_TYPE;
import static org.folio.support.base.TestConstants.DI_DELETED_TYPE;
import static org.folio.support.base.TestConstants.DI_ERROR_TOPIC;
import static org.folio.support.base.TestConstants.DI_UPDATED_TYPE;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.dataImportAuthorityCreatedTopic;
import static org.folio.support.base.TestConstants.dataImportAuthorityDeletedTopic;
import static org.folio.support.base.TestConstants.dataImportAuthorityModifiedTopic;
import static org.folio.support.base.TestConstants.diAuthorityCompletedTopic;
import static org.folio.support.base.TestConstants.diAuthorityCreatedPostProcessingTopic;
import static org.folio.support.base.TestConstants.diAuthorityErrorTopic;
import static org.folio.support.base.TestConstants.diAuthorityUpdateTopic;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
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
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.AUTHORITY_DATA_STAT_TABLE,
  DatabaseHelper.AUTHORITY_TABLE,
  DatabaseHelper.AUTHORITY_ARCHIVE_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE,
}, tenants = {TENANT_ID})
class DataImportEventListenerIT extends IntegrationTestBase {

  private static final UUID AUTHORITY_CREATE_ID = UUID.fromString("7e3745e6-2d48-4f22-825e-72f3338bfa14");
  private static final UUID AUTHORITY_DELETE_ID = UUID.fromString("8e3745e6-2d48-4f22-825e-72f3338bfa25");
  private static final UUID AUTHORITY_UPDATE_ID = UUID.fromString("9e3745e6-2d48-4f22-825e-72f3338bfa36");
  private static final String PATH = "classpath:di-authority/";
  private static final String DI_CREATE_AUTHORITY_PATH = PATH + "create.json";
  private static final String DI_UPDATE_AUTHORITY_PATH = PATH + "update.json";
  private static final String DI_DELETE_AUTHORITY_PATH = PATH + "delete.json";
  private KafkaMessageListenerContainer<String, Event> container;
  private BlockingQueue<ConsumerRecord<String, Event>> consumerRecords;

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @BeforeEach
  void setUp() {
    var sourceFile1 = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, sourceFile1);
  }

  @AfterEach
  void tearDown() {
    consumerRecords.clear();
    container.stop();
  }

  @SneakyThrows
  @Test
  void shouldHandleDataImportAuthorityCreatedEvent_positive(@Autowired KafkaProperties kafkaProperties) {
    startConsumer(kafkaProperties, diAuthorityCreatedPostProcessingTopic());
    // send DI authority created event
    var eventPayload = readFile(DI_CREATE_AUTHORITY_PATH);
    var event = TestDataUtils.diAuthorityEvent(DI_CREATED_TYPE,
        eventPayload, AUTHORITY_CREATE_ID.toString(), TENANT_ID);
    sendKafkaMessage(dataImportAuthorityCreatedTopic(), AUTHORITY_CREATE_ID.toString(), event,
        Map.of("folio.tenantId", TENANT_ID,
            "x-okapi-url", okapi.getOkapiUrl(),
            "recordId", AUTHORITY_CREATE_ID.toString(),
            "jobExecutionId", UUID.randomUUID().toString()));

    // wait until authority is created
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        doGet(authorityEndpoint(AUTHORITY_CREATE_ID))
            .andExpect(status().isOk())
    );
    // verify created authority
    var content = doGet(authorityEndpoint(AUTHORITY_CREATE_ID)).andReturn().getResponse().getContentAsString();
    var authorityDto = objectMapper.readValue(content, AuthorityDto.class);
    assertNotNull(authorityDto);

    // check sent event fields
    var received = getReceivedEvent(DI_AUTHORITY_CREATED_POST_PROCESSING_TOPIC, AUTHORITY_CREATE_ID.toString());
    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_CREATE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_AUTHORITY_CREATED_POST_PROCESSING_TOPIC);
    assertions.assertAll();
  }

  @SneakyThrows
  @Test
  void shouldHandleDataImportAuthorityCreatedEvent_negative_duplicateAuthority(
      @Autowired KafkaProperties kafkaProperties) {
    startConsumer(kafkaProperties, diAuthorityErrorTopic());
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
        Map.of("folio.tenantId", TENANT_ID,
            "x-okapi-url", okapi.getOkapiUrl(),
            "recordId", AUTHORITY_CREATE_ID.toString(),
            "jobExecutionId", UUID.randomUUID().toString()));

    // check sent DI error event
    var received = getReceivedEvent(DI_ERROR_TOPIC, AUTHORITY_CREATE_ID.toString());
    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_CREATE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_ERROR_TOPIC);
    assertions.then(received.value().getEventPayload())
        .contains("ERROR: duplicate key value violates unique constraint");
    assertions.assertAll();
  }

  @SneakyThrows
  @Test
  void shouldHandleDataImportAuthorityDeletedEvent_positive(@Autowired KafkaProperties kafkaProperties) {
    startConsumer(kafkaProperties, diAuthorityCompletedTopic());
    // create authority
    var authority = authority(0, 0);
    authority.setId(AUTHORITY_DELETE_ID);
    databaseHelper.saveAuthority(TENANT_ID, authority);

    // wait until authority is created
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        doGet(authorityEndpoint(AUTHORITY_DELETE_ID))
            .andExpect(status().isOk())
    );

    // send DI authority deleted event for existing authority
    var eventPayload = readFile(DI_DELETE_AUTHORITY_PATH);
    var event = TestDataUtils.diAuthorityEvent(DI_DELETED_TYPE,
        eventPayload, AUTHORITY_DELETE_ID.toString(), TENANT_ID);
    sendKafkaMessage(dataImportAuthorityDeletedTopic(), AUTHORITY_DELETE_ID.toString(), event,
        Map.of("folio.tenantId", TENANT_ID,
            "x-okapi-url", okapi.getOkapiUrl(),
            "recordId", AUTHORITY_DELETE_ID.toString(),
            "jobExecutionId", UUID.randomUUID().toString()));

    // check sent DI completed event
    var received = getReceivedEvent(DI_COMPLETED_TOPIC, AUTHORITY_DELETE_ID.toString());
    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_DELETE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_COMPLETED_TOPIC);
    assertions.assertAll();
  }

  @SneakyThrows
  @Test
  void shouldHandleDataImportAuthorityDeletedEvent_negative_noAuthorityToDelete(
      @Autowired KafkaProperties kafkaProperties) {
    startConsumer(kafkaProperties, diAuthorityErrorTopic());
    // check authority does not exist
    doGet(authorityEndpoint())
        .andExpect(jsonPath("totalRecords", is(0)));

    // send DI authority deleted event for non-existing authority
    var eventPayload = readFile(DI_DELETE_AUTHORITY_PATH);
    var event = TestDataUtils.diAuthorityEvent(DI_DELETED_TYPE,
        eventPayload, AUTHORITY_DELETE_ID.toString(), TENANT_ID);
    sendKafkaMessage(dataImportAuthorityDeletedTopic(), AUTHORITY_DELETE_ID.toString(), event,
        Map.of("folio.tenantId", TENANT_ID,
            "x-okapi-url", okapi.getOkapiUrl(),
            "recordId", AUTHORITY_DELETE_ID.toString(),
            "jobExecutionId", UUID.randomUUID().toString()));

    // check sent DI error event
    var received = getReceivedEvent(DI_ERROR_TOPIC, AUTHORITY_DELETE_ID.toString());
    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_DELETE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_ERROR_TOPIC);
    assertions.assertAll();
  }

  @SneakyThrows
  @Test
  void shouldHandleDataImportAuthorityUpdateEvent_positive(@Autowired KafkaProperties kafkaProperties) {
    startConsumer(kafkaProperties, diAuthorityUpdateTopic());
    // create authority
    var authority = authority(0, 0);
    authority.setId(AUTHORITY_UPDATE_ID);
    //check initial authority has no identifiers
    assertNull(authority.getIdentifiers());

    databaseHelper.saveAuthority(TENANT_ID, authority);

    // wait until authority is created
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        doGet(authorityEndpoint(AUTHORITY_UPDATE_ID))
            .andExpect(status().isOk())
    );

    // send DI authority update event for existing authority
    var eventPayload = readFile(DI_UPDATE_AUTHORITY_PATH);
    var event = TestDataUtils.diAuthorityEvent(DI_UPDATED_TYPE,
        eventPayload, AUTHORITY_UPDATE_ID.toString(), TENANT_ID);
    sendKafkaMessage(dataImportAuthorityModifiedTopic(), AUTHORITY_UPDATE_ID.toString(), event,
        Map.of("folio.tenantId", TENANT_ID,
            "x-okapi-url", okapi.getOkapiUrl(),
            "recordId", AUTHORITY_UPDATE_ID.toString(),
            "jobExecutionId", UUID.randomUUID().toString()));

    // verify updated authority
    var content = doGet(authorityEndpoint(AUTHORITY_UPDATE_ID)).andReturn().getResponse().getContentAsString();
    var authorityDto = objectMapper.readValue(content, AuthorityDto.class);
    assertNotNull(authorityDto);
    //check identifiers were added
    assertEquals(3, authorityDto.getIdentifiers().size());

    // check sent DI update event
    var received = getReceivedEvent(DI_AUTHORITY_UPDATED_TOPIC, AUTHORITY_UPDATE_ID.toString());
    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_UPDATE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_AUTHORITY_UPDATED_TOPIC);
    assertions.assertAll();
  }

  @SneakyThrows
  @Test
  void shouldHandleDataImportAuthorityUpdateEvent_negative_noAuthority(@Autowired KafkaProperties kafkaProperties) {
    startConsumer(kafkaProperties, diAuthorityErrorTopic());
    // check authority does not exist
    doGet(authorityEndpoint()).andExpect(jsonPath("totalRecords", is(0)));

    // send DI authority update event for non-existing authority
    var eventPayload = readFile(DI_UPDATE_AUTHORITY_PATH);
    var event = TestDataUtils.diAuthorityEvent(DI_UPDATED_TYPE,
        eventPayload, AUTHORITY_UPDATE_ID.toString(), TENANT_ID);
    sendKafkaMessage(dataImportAuthorityModifiedTopic(), AUTHORITY_UPDATE_ID.toString(), event,
        Map.of("folio.tenantId", TENANT_ID,
            "x-okapi-url", okapi.getOkapiUrl(),
            "recordId", AUTHORITY_UPDATE_ID.toString(),
            "jobExecutionId", UUID.randomUUID().toString()));

    // check sent DI error event
    var received = getReceivedEvent(DI_ERROR_TOPIC, AUTHORITY_UPDATE_ID.toString());
    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_UPDATE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_ERROR_TOPIC);
    assertions.assertAll();
  }

  @SneakyThrows
  private ConsumerRecord<String, Event> getReceivedEvent(String expectedTopic, String expectedKey) {
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
    while (System.currentTimeMillis() < deadline) {
      var consumerRecord = consumerRecords.poll(500, TimeUnit.MILLISECONDS);
      if (consumerRecord == null) {
        continue;
      }
      if (consumerRecord.topic().contains(expectedTopic)
          && consumerRecord.key().equals(expectedKey)) {
        return consumerRecord;
      }
    }
    return null;
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, Event> getReceivedEvent() {
    return consumerRecords.poll(10, TimeUnit.SECONDS);
  }

  private void startConsumer(KafkaProperties kafkaProperties, String... topics) {
    consumerRecords = new LinkedBlockingQueue<>();
    container = createAndStartTestConsumer(
        consumerRecords,
        kafkaProperties,
        Event.class,
        topics
    );
  }
}
