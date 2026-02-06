package org.folio.entlinks.integration.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.config.properties.ApplicationMetadata;
import org.folio.entlinks.exception.KafkaEventPublishingException;
import org.folio.rest.jaxrs.model.Event;
import org.folio.spring.testing.type.UnitTest;
import org.folio.spring.tools.config.properties.FolioEnvironment;
import org.folio.spring.tools.kafka.KafkaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.RoutingKafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DataImportEventPublisherTest {

  private static final String TENANT_ID = "test";
  private static final String TOPIC_NAME = "test-topic";
  private static final String APP_NAME = "test-app";
  private static final String ENV_NAME = "test-env";
  private static final String EVENT_PAYLOAD = "event-payload";

  @Mock
  private JsonMapper jsonMapper;
  @Mock
  private RoutingKafkaTemplate kafkaTemplate;
  @Mock
  private ApplicationMetadata applicationMetadata;
  @InjectMocks
  private DataImportEventPublisher publisher;

  @BeforeEach
  void setUp() {
    try (MockedStatic<FolioEnvironment> env = mockStatic(FolioEnvironment.class);
         MockedStatic<KafkaUtils> kafka = mockStatic(KafkaUtils.class)) {
      env.when(FolioEnvironment::getFolioEnvName).thenReturn(ENV_NAME);
      kafka.when(() -> KafkaUtils.getTenantTopicNameWithNamespace(any(), any(), any(), any()))
          .thenReturn(TOPIC_NAME);
    }
  }

  @Test
  @SneakyThrows
  void publish_eventPublishedSuccessfully() {
    var payload = new DataImportEventPayload().withTenant(TENANT_ID).withContext(new HashMap<>());
    var event = new Event();
    var sendResult = new SendResult<Object, Object>(
        new ProducerRecord<>(TOPIC_NAME, "key", event),
        new RecordMetadata(new TopicPartition(TOPIC_NAME, 0), 0, 0, 0, 0, 0)
    );
    when(jsonMapper.writeValueAsString(any())).thenReturn(EVENT_PAYLOAD);
    when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(sendResult));
    when(applicationMetadata.getFullApplicationName()).thenReturn(APP_NAME);

    // Act
    var resultFuture = publisher.publish(payload);

    // Assert
    var result = resultFuture.get();
    assertThat(result).isNotNull();
    assertThat(result.getEventPayload()).isEqualTo(EVENT_PAYLOAD);
  }

  @Test
  @SneakyThrows
  void publish_serializationFails() {
    var payload = new DataImportEventPayload().withTenant(TENANT_ID);
    when(jsonMapper.writeValueAsString(any())).thenThrow(new JacksonException("error") {
    });

    // Act & Assert
    assertThrows(KafkaEventPublishingException.class, () -> publisher.publish(payload));
  }

  @Test
  @SneakyThrows
  void publish_kafkaSendFails()  {
    var payload = new DataImportEventPayload().withTenant(TENANT_ID).withContext(new HashMap<>());
    var future = CompletableFuture.failedFuture(new RuntimeException("Kafka send failed"));
    when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
    when(jsonMapper.writeValueAsString(any())).thenReturn(EVENT_PAYLOAD);
    when(applicationMetadata.getFullApplicationName()).thenReturn(APP_NAME);

    // Act
    var resultFuture = publisher.publish(payload);

    // Assert
    var result = resultFuture.get();
    assertThat(result).isNull();
  }
}
