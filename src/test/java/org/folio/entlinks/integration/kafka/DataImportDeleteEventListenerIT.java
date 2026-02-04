package org.folio.entlinks.integration.kafka;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.folio.support.FileTestUtils.readFile;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.base.TestConstants.DI_COMPLETED_TOPIC;
import static org.folio.support.base.TestConstants.DI_DELETED_TYPE;
import static org.folio.support.base.TestConstants.DI_DELETE_AUTHORITY_PATH;
import static org.folio.support.base.TestConstants.DI_ERROR_TOPIC;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.dataImportAuthorityDeletedTopic;
import static org.folio.support.base.TestConstants.diAuthorityCompletedTopic;
import static org.folio.support.base.TestConstants.diAuthorityErrorTopic;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.BDDSoftAssertions;
import org.awaitility.Durations;
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
class DataImportDeleteEventListenerIT extends IntegrationTestBase {

  private static final UUID AUTHORITY_DELETE_ID = UUID.fromString("8e3745e6-2d48-4f22-825e-72f3338bfa25");

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
        diAuthorityCompletedTopic(), diAuthorityCompletedTopic(), diAuthorityErrorTopic());

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
  void shouldHandleDataImportAuthorityDeletedEvent_positive() {
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
        geKafkaHeaders(AUTHORITY_DELETE_ID.toString()));

    // check sent DI completed event
    var received = getReceivedEvent();

    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_DELETE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_COMPLETED_TOPIC);
    assertions.assertAll();
  }

  @SneakyThrows
  @Test
  void shouldHandleDataImportAuthorityDeletedEvent_negative_noAuthorityToDelete() {
    // check authority does not exist
    doGet(authorityEndpoint())
        .andExpect(jsonPath("totalRecords", is(0)));

    // send DI authority deleted event for non-existing authority
    var eventPayload = readFile(DI_DELETE_AUTHORITY_PATH);
    var event = TestDataUtils.diAuthorityEvent(DI_DELETED_TYPE,
        eventPayload, AUTHORITY_DELETE_ID.toString(), TENANT_ID);
    sendKafkaMessage(dataImportAuthorityDeletedTopic(), AUTHORITY_DELETE_ID.toString(), event,
        geKafkaHeaders(AUTHORITY_DELETE_ID.toString()));

    // check sent DI error event
    var received = getReceivedEvent();

    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_DELETE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_ERROR_TOPIC);
    assertions.assertAll();
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, Event> getReceivedEvent() {
    return consumerRecords.poll(20, TimeUnit.SECONDS);
  }
}
