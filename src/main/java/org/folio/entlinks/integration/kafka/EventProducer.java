package org.folio.entlinks.integration.kafka;

import static org.folio.spring.tools.kafka.KafkaUtils.getTenantTopicName;
import static org.folio.spring.tools.kafka.KafkaUtils.toKafkaHeaders;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.codehaus.plexus.util.StringUtils;
import org.folio.entlinks.integration.dto.event.BaseEvent;
import org.folio.entlinks.utils.DateUtils;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

@Log4j2
@RequiredArgsConstructor
public class EventProducer<T> {

  private final KafkaTemplate<String, T> template;
  private final String topicName;

  @Autowired
  private FolioExecutionContext context;

  public void sendMessage(String key, T msgBody, Object... headers) {
    log.debug("Sending event to Kafka [topic: {}]", topicName);
    log.debug("Sending event to Kafka [topic: {}, body: {}]", topicName, msgBody);
    if (headers.length % 2 != 0) {
      throw new IllegalArgumentException(
        String.format("Wrong number of %s header key and value pairs are provided", headers.length));
    }
    log.info("sendMessage: headers {}", headers);
    var headersMap = new HashMap<String, Collection<String>>();
    for (int i = 0; i < headers.length; i += 2) {
      headersMap.put(headers[i].toString(), List.of(headers[i + 1].toString()));
    }
    var producerRecord = toProducerRecord(key, msgBody, headersMap);
    template.send(producerRecord);
  }

  public void sendMessages(List<T> msgBodies) {
    if (log.isTraceEnabled()) {
      log.trace("Sending events to Kafka [topic: {}, bodies: {}]", topicName, msgBodies);
    } else {
      log.info("Sending events to Kafka [topic: {}, number: {}]", topicName, msgBodies.size());
    }
    msgBodies.stream()
      .map(this::toProducerRecord)
      .forEach(template::send);
  }

  private ProducerRecord<String, T> toProducerRecord(T msgBody) {
    return toProducerRecord(null, msgBody, Collections.emptyMap());
  }

  private ProducerRecord<String, T> toProducerRecord(String key, T msgBody,
                                                     Map<String, Collection<String>> headersMap) {
    if (msgBody instanceof BaseEvent baseEvent) {
      baseEvent.setTenant(context.getTenantId());
      baseEvent.setTs(DateUtils.currentTsInString());
    }

    ProducerRecord<String, T> producerRecord;
    if (StringUtils.isBlank(key)) {
      producerRecord = new ProducerRecord<>(topicName(), msgBody);
    } else {
      producerRecord = new ProducerRecord<>(topicName(), key, msgBody);
    }
    log.info("context.getOkapiHeaders(): {}", context.getOkapiHeaders());
    toKafkaHeaders(context.getOkapiHeaders())
      .forEach(header -> producerRecord.headers().add(header));
    log.info("headersMap: {}", headersMap);
    toKafkaHeaders(headersMap)
      .forEach(header -> producerRecord.headers().add(header));
    log.info("producerRecord.headers: {}", producerRecord.headers());
    return producerRecord;
  }

  public void sendMessagesWithSystemUserId(List<T> msgBodies, Map<String, Collection<String>> headersMap) {
    if (log.isTraceEnabled()) {
      log.trace("Sending events to Kafka [topic: {}, bodies: {}]", topicName, msgBodies);
    } else {
      log.info("Sending events to Kafka [topic: {}, number: {}]", topicName, msgBodies.size());
    }
    var userId = context.getUserId();
    log.info("context.getUserId: {}", userId);
    msgBodies.stream()
        .map(record -> toProducerRecord(null, record, headersMap))
        .forEach(template::send);
  }

  private String topicName() {
    return getTenantTopicName(topicName, context.getTenantId());
  }
}
