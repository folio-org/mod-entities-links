package org.folio.entlinks.integration.kafka.deserializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.integration.kafka.model.DataImportEventWrapper;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.testing.type.UnitTest;
import org.folio.spring.tools.kafka.FolioKafkaProperties;
import org.junit.jupiter.api.Test;

@UnitTest
class ConsumerRecordToWrapperConverterTest {

  private static final String TOPIC = "test-topic";
  private static final String OKAPI_URL = "http://okapi:9130";

  private final ConsumerRecordToWrapperConverter converter = new ConsumerRecordToWrapperConverter();

  @Test
  void toMessage_positive_extractsOkapiUrlFromPayloadWhenNotInKafkaHeaders() {
    var payload = payload(OKAPI_URL);
    var consumerRecord = consumerRecord(payload);

    var message = converter.toMessage(consumerRecord, null, null, null);

    var wrapper = (DataImportEventWrapper) message.getPayload();
    assertThat(wrapper.getHeadersMap()).containsKey(XOkapiHeaders.URL);
    assertThat(wrapper.getHeadersMap().get(XOkapiHeaders.URL)).containsExactly(OKAPI_URL);
  }

  @Test
  void toMessage_positive_prefersKafkaHeaderOkapiUrlOverPayload() {
    var kafkaOkapiUrl = "http://kafka-okapi:9130";
    var payload = payload("http://payload-okapi:9130");
    var consumerRecord = consumerRecord(payload);
    consumerRecord.headers().add(header(XOkapiHeaders.URL, kafkaOkapiUrl));

    var message = converter.toMessage(consumerRecord, null, null, null);

    var wrapper = (DataImportEventWrapper) message.getPayload();
    assertThat(wrapper.getHeadersMap().get(XOkapiHeaders.URL)).containsExactly(kafkaOkapiUrl);
  }

  @Test
  void toMessage_positive_mapsUserIdToOkapiUserIdHeader() {
    var payload = payload(null);
    var consumerRecord = consumerRecord(payload);
    consumerRecord.headers().add(header(XOkapiHeaders.USER_ID, USER_ID));

    var message = converter.toMessage(consumerRecord, null, null, null);

    var wrapper = (DataImportEventWrapper) message.getPayload();
    assertThat(wrapper.getHeadersMap()).containsKey(XOkapiHeaders.USER_ID);
    assertThat(wrapper.getHeadersMap().get(XOkapiHeaders.USER_ID)).containsExactly(USER_ID);
  }

  @Test
  void toMessage_positive_setsTenantFromFolioTenantIdHeader() {
    var payload = payload(null);
    var consumerRecord = consumerRecord(payload);

    var message = converter.toMessage(consumerRecord, null, null, null);

    var wrapper = (DataImportEventWrapper) message.getPayload();
    assertThat(wrapper.tenant()).isEqualTo(TENANT_ID);
  }

  @Test
  void toMessage_positive_addsContextHeadersToPayloadContext() {
    var payload = payload(null);
    var consumerRecord = consumerRecord(payload);
    consumerRecord.headers().add(header("recordId", "rec-1"));
    consumerRecord.headers().add(header("chunkId", "chunk-1"));
    consumerRecord.headers().add(header("jobExecutionId", "job-1"));

    converter.toMessage(consumerRecord, null, null, null);

    assertThat(payload.getContext())
      .containsEntry("recordId", "rec-1")
      .containsEntry("chunkId", "chunk-1")
      .containsEntry("jobExecutionId", "job-1");
  }

  @Test
  void toMessage_negative_doesNotAddOkapiUrlWhenPayloadUrlIsNull() {
    var payload = payload(null);
    var consumerRecord = consumerRecord(payload);

    var message = converter.toMessage(consumerRecord, null, null, null);

    var wrapper = (DataImportEventWrapper) message.getPayload();
    assertThat(wrapper.getHeadersMap()).doesNotContainKey(XOkapiHeaders.URL);
  }

  private static DataImportEventPayload payload(String okapiUrl) {
    return new DataImportEventPayload()
      .withOkapiUrl(okapiUrl)
      .withContext(new HashMap<>());
  }

  private static ConsumerRecord<String, DataImportEventPayload> consumerRecord(DataImportEventPayload payload) {
    var consumerRecord = new ConsumerRecord<String, DataImportEventPayload>(TOPIC, 0, 0L, null, payload);
    consumerRecord.headers().add(header(FolioKafkaProperties.TENANT_ID, TENANT_ID));
    return consumerRecord;
  }

  private static RecordHeader header(String key, String value) {
    return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
  }
}
