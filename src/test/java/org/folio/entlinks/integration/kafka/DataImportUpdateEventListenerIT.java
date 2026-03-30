package org.folio.entlinks.integration.kafka;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.folio.support.FileTestUtils.readFile;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.base.TestConstants.DI_AUTHORITY_UPDATED_TOPIC;
import static org.folio.support.base.TestConstants.DI_ERROR_TOPIC;
import static org.folio.support.base.TestConstants.DI_UPDATED_TYPE;
import static org.folio.support.base.TestConstants.DI_UPDATE_AUTHORITY_PATH;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.dataImportAuthorityModifiedTopic;
import static org.folio.support.base.TestConstants.diAuthorityErrorTopic;
import static org.folio.support.base.TestConstants.diAuthorityUpdateTopic;
import static org.folio.support.base.TestConstants.diJobCanceledTopic;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.assertj.core.api.BDDSoftAssertions;
import org.awaitility.Durations;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.di.DataImportCanceledJobService;
import org.folio.rest.jaxrs.model.Event;
import org.folio.spring.integration.XOkapiHeaders;
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
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE,
}, tenants = {TENANT_ID})
class DataImportUpdateEventListenerIT extends IntegrationTestBase {

  private static final UUID AUTHORITY_UPDATE_ID = UUID.fromString("9e3745e6-2d48-4f22-825e-72f3338bfa36");
  private static final String DI_JOB_ID = "ee39dc50-6adb-44bb-b707-d85b14cbe201";

  private KafkaMessageListenerContainer<String, Event> container;
  private BlockingQueue<ConsumerRecord<String, Event>> consumerRecords;

  private @Autowired DataImportCanceledJobService dataImportCanceledJobService;

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @BeforeEach
  void setUp(@Autowired KafkaProperties kafkaProperties) {
    consumerRecords = new LinkedBlockingQueue<>();
    container = createAndStartTestConsumer(consumerRecords, kafkaProperties, Event.class,
      diAuthorityUpdateTopic(), diAuthorityErrorTopic());

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
  void shouldHandleDataImportAuthorityUpdateEvent_positive() {
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
    var updaterUserId = UUID.randomUUID();
    var headers = new HashMap<>(getDataImportKafkaHeaders(AUTHORITY_UPDATE_ID.toString()));
    //srs sends without this header
    headers.remove(XOkapiHeaders.USER_ID);
    headers.put("userId", updaterUserId.toString());
    sendKafkaMessage(dataImportAuthorityModifiedTopic(), AUTHORITY_UPDATE_ID.toString(), event, headers);

    // check sent DI update event
    var received = getReceivedEvent();
    var receivedHeaderKeys = Arrays.stream(received.headers().toArray())
      .map(Header::key)
      .collect(Collectors.toSet());

    var assertions = new BDDSoftAssertions();
    assertions.then(receivedHeaderKeys).as("headers")
      .contains(XOkapiHeaders.TENANT, XOkapiHeaders.USER_ID, XOkapiHeaders.URL);
    assertions.then(received).isNotNull();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_UPDATE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_AUTHORITY_UPDATED_TOPIC);
    assertions.assertAll();

    // verify updated authority
    var content = doGet(authorityEndpoint(AUTHORITY_UPDATE_ID)).andReturn().getResponse().getContentAsString();
    var authorityDto = objectMapper.readValue(content, AuthorityDto.class);
    assertNotNull(authorityDto);
    // check identifiers were added
    assertEquals(3, authorityDto.getIdentifiers().size());
    // verify metadata
    assertNotNull(authorityDto.getMetadata());
    assertEquals(UUID.fromString(USER_ID), authorityDto.getMetadata().getCreatedByUserId());
    assertEquals(updaterUserId, authorityDto.getMetadata().getUpdatedByUserId());
  }

  @SneakyThrows
  @Test
  void shouldHandleDataImportAuthorityUpdateEvent_positive_shouldSkipEventIfJobCanceled() {
    // create authority
    var authority = authority(0, 0);
    authority.setId(AUTHORITY_UPDATE_ID);
    databaseHelper.saveAuthority(TENANT_ID, authority);

    // send DI job canceled event
    sendKafkaMessage(diJobCanceledTopic(), DI_JOB_ID, new Object(),
      getDataImportCanceledJobKafkaHeaders(TENANT_ID, DI_JOB_ID));

    awaitUntilAsserted(() -> assertTrue(dataImportCanceledJobService.isJobCanceled(DI_JOB_ID, TENANT_ID)));

    // send DI authority created event
    var eventPayload = readFile(DI_UPDATE_AUTHORITY_PATH);
    var event = TestDataUtils.diAuthorityEvent(DI_UPDATED_TYPE,
      eventPayload, AUTHORITY_UPDATE_ID.toString(), TENANT_ID);
    sendKafkaMessage(dataImportAuthorityModifiedTopic(), AUTHORITY_UPDATE_ID.toString(), event,
      getDataImportKafkaHeaders(AUTHORITY_UPDATE_ID.toString()));

    // Allow time for the Kafka consumer to run; assert the record never updated
    await().during(5, SECONDS)
      .pollDelay(ONE_SECOND)
      .pollInterval(ONE_SECOND)
      .atMost(10, SECONDS)
      .untilAsserted(() -> doGet(authorityEndpoint(AUTHORITY_UPDATE_ID))
        .andExpect(jsonPath("_version", is(0)))
      );
  }

  @SneakyThrows
  @Test
  void shouldHandleDataImportAuthorityUpdateEvent_negative_noAuthority() {
    // check authority does not exist
    doGet(authorityEndpoint()).andExpect(jsonPath("totalRecords", is(0)));

    // send DI authority update event for non-existing authority
    var eventPayload = readFile(DI_UPDATE_AUTHORITY_PATH);
    var event = TestDataUtils.diAuthorityEvent(DI_UPDATED_TYPE,
      eventPayload, AUTHORITY_UPDATE_ID.toString(), TENANT_ID);
    sendKafkaMessage(dataImportAuthorityModifiedTopic(), AUTHORITY_UPDATE_ID.toString(), event,
      getDataImportKafkaHeaders(AUTHORITY_UPDATE_ID.toString()));

    // check sent DI error event
    var received = getReceivedEvent();

    var assertions = new BDDSoftAssertions();
    assertions.then(received).isNotNull();
    assertions.then(received.key()).as("key").isEqualTo(AUTHORITY_UPDATE_ID.toString());
    assertions.then(received.topic()).as("topic").contains(DI_ERROR_TOPIC);
    assertions.assertAll();
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, Event> getReceivedEvent() {
    return consumerRecords.poll(20, TimeUnit.SECONDS);
  }
}
