package org.folio.entlinks.integration.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.dto.event.DomainEvent;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.folio.spring.tools.kafka.KafkaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@UnitTest
@ExtendWith(MockitoExtension.class)
class EventProducerTest {

  private static final String TOPIC = "TEST_TOPIC";
  private static final String TENANT_ID = "TEST_TENANT";

  @Mock
  private KafkaTemplate<String, DomainEvent<?>> template;

  @Mock
  private FolioExecutionContext context;

  @Captor
  private ArgumentCaptor<ProducerRecord<String, DomainEvent<?>>> captor;

  private EventProducer<DomainEvent<?>> eventProducer;

  @BeforeEach
  void setup() {
    eventProducer = new EventProducer<>(template, TOPIC);
    ReflectionTestUtils.setField(eventProducer, "context", context);
  }

  @Test
  void shouldSendMessageToTenantCollectionTopic() {
    ReflectionTestUtils.setField(KafkaUtils.class, "TENANT_COLLECTION_TOPICS_ENABLED", true);
    ReflectionTestUtils.setField(KafkaUtils.class, "tenantCollectionTopicQualifier", "COLLECTION");
    when(context.getTenantId()).thenReturn(TENANT_ID);
    var messageId = UUID.randomUUID();
    var payload = new AuthorityDto().id(messageId);
    var domainEvent = DomainEvent.createEvent(messageId, payload, TENANT_ID);
    final var expectedTopicName = "folio.COLLECTION." + TOPIC;

    eventProducer.sendMessage(messageId.toString(), domainEvent, "headerKey", "headerVal");

    verify(template).send(captor.capture());
    var capturedRecord = captor.getValue();
    assertNotNull(capturedRecord);
    assertEquals(expectedTopicName, capturedRecord.topic());
    assertEquals(domainEvent, capturedRecord.value());
  }
}
