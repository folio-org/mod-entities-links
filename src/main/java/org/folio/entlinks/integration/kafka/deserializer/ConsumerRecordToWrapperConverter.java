package org.folio.entlinks.integration.kafka.deserializer;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.integration.kafka.model.DataImportEventWrapper;
import org.folio.spring.tools.kafka.FolioKafkaProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class ConsumerRecordToWrapperConverter implements RecordMessageConverter {

  @Override
  public @NotNull Message<?> toMessage(ConsumerRecord<?, ?> consumerRecord, Acknowledgment acknowledgment,
                                       Consumer<?, ?> consumer, Type payloadType) {
    DataImportEventPayload payload = (DataImportEventPayload) consumerRecord.value();

    Map<String, String> headers = new HashMap<>();
    for (Header h : consumerRecord.headers()) {
      headers.put(h.key(), new String(h.value(), StandardCharsets.UTF_8));
    }

    DataImportEventWrapper wrapper = new DataImportEventWrapper(payload, headers,
      headers.get(FolioKafkaProperties.TENANT_ID));

    return MessageBuilder.withPayload(wrapper)
      .setHeader(KafkaHeaders.RECEIVED_TOPIC, consumerRecord.topic())
      .setHeader(KafkaHeaders.RECEIVED_PARTITION, consumerRecord.partition())
      .setHeader(KafkaHeaders.KEY, consumerRecord.key())
      .setHeader(KafkaHeaders.OFFSET, consumerRecord.offset())
      .setHeader(KafkaHeaders.TIMESTAMP, consumerRecord.timestamp())
      .build();
  }

  @Override
  public ProducerRecord<?, ?> fromMessage(Message<?> message, String defaultTopic) {
    throw new UnsupportedOperationException("not implemented");
  }
}
