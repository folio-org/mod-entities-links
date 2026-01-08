package org.folio.entlinks.integration.di;

import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.CHUNK_ID_HEADER;
import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.JOB_EXECUTION_ID_HEADER;
import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.RECORD_ID_HEADER;
import static org.folio.rest.util.OkapiConnectionParams.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.PERMISSIONS;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;
import static org.folio.spring.tools.kafka.FolioKafkaProperties.TENANT_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.config.properties.ApplicationMetadata;
import org.folio.entlinks.exception.KafkaEventPublishingException;
import org.folio.processing.events.services.publisher.EventPublisher;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.spring.tools.config.properties.FolioEnvironment;
import org.folio.spring.tools.kafka.KafkaUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Spring-managed implementation of EventPublisher for publishing data import events to Kafka.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class DataImportEventPublisher implements EventPublisher {

  private static final String DEFAULT_NAMESPACE = "Default";

  private final ObjectMapper objectMapper;
  private final KafkaTemplate<String, Event> kafkaTemplate;
  private final ApplicationMetadata applicationMetadata;

  @Override
  public CompletableFuture<Event> publish(DataImportEventPayload payload) {
    final long startTime = System.currentTimeMillis();
    var eventName = payload.getEventType();
    var tenant = payload.getTenant();
    var jobExecutionId = payload.getJobExecutionId();
    var topicName = KafkaUtils.getTenantTopicNameWithNamespace(eventName,
      FolioEnvironment.getFolioEnvName(),
      tenant,
      DEFAULT_NAMESPACE);

    log.info("=== DataImportEventPublisher.publish() START === [eventType: {}, tenant: {}, jobExecutionId: {}, "
        + "topic: {}]",
      eventName, tenant, jobExecutionId, topicName);

    var event = prepareEvent(payload);
    log.info("Event prepared [eventId: {}, eventType: {}, jobExecutionId: {}]", event.getId(), eventName,
      jobExecutionId);

    var producerRecord = new ProducerRecord<String, Event>(topicName, event);
    prepareHeaders(payload, producerRecord);
    log.info("Headers prepared, about to call kafkaTemplate.send() [topic: {}, jobExecutionId: {}]",
      topicName, jobExecutionId);

    long beforeSendTime = System.currentTimeMillis();
    return kafkaTemplate.send(producerRecord)
      .handle((recordMetadata, ex) -> {
        long duration = System.currentTimeMillis() - startTime;
        long sendDuration = System.currentTimeMillis() - beforeSendTime;

        if (ex != null) {
          log.error("=== DataImportEventPublisher.publish() FAILED === [eventType: {}, topic: {}, "
              + "jobExecutionId: {}, totalDuration: {}ms, sendDuration: {}ms, error: {}]",
            eventName, topicName, jobExecutionId, duration, sendDuration, ex.getMessage(), ex);
          return null;
        }

        log.info("=== DataImportEventPublisher.publish() SUCCESS === [eventType: {}, topic: {}, "
            + "jobExecutionId: {}, partition: {}, offset: {}, totalDuration: {}ms, sendDuration: {}ms]",
          eventName, topicName, jobExecutionId,
          recordMetadata.getRecordMetadata().partition(),
          recordMetadata.getRecordMetadata().offset(),
          duration, sendDuration);
        return event;
      });
  }

  private Event prepareEvent(DataImportEventPayload payload) {
    try {
      return new Event()
        .withId(UUID.randomUUID().toString())
        .withEventPayload(objectMapper.writeValueAsString(payload))
        .withEventMetadata(new EventMetadata()
          .withEventTTL(1)
          .withTenantId(payload.getTenant())
          .withPublishedBy(applicationMetadata.getFullApplicationName())
          .withPublishedDate(new Date()));
    } catch (JsonProcessingException e) {
      throw new KafkaEventPublishingException(e);
    }
  }

  private void prepareHeaders(DataImportEventPayload payload, ProducerRecord<String, Event> producerRecord) {
    var messageHeaders = producerRecord.headers();
    getHeaders(payload).forEach(messageHeaders::add);
  }

  private List<RecordHeader> getHeaders(DataImportEventPayload payload) {
    if (payload == null) {
      return Collections.emptyList();
    }
    var headers = new ArrayList<RecordHeader>();

    addHeaderIfPresent(headers, TOKEN, payload.getToken());
    addHeaderIfPresent(headers, URL, payload.getOkapiUrl());
    addHeaderIfPresent(headers, TENANT, payload.getTenant());
    addHeaderIfPresent(headers, TENANT_ID, payload.getTenant());
    addHeaderIfPresent(headers, JOB_EXECUTION_ID_HEADER, payload.getJobExecutionId());

    var context = payload.getContext();
    if (context != null) {
      addHeaderIfPresent(headers, PERMISSIONS, context.get(PERMISSIONS));
      addHeaderIfPresent(headers, USER_ID_HEADER, context.get(USER_ID_HEADER));
      addHeaderIfPresent(headers, RECORD_ID_HEADER, context.get(RECORD_ID_HEADER));
      addHeaderIfPresent(headers, CHUNK_ID_HEADER, context.get(CHUNK_ID_HEADER));
    }

    return headers;
  }

  private void addHeaderIfPresent(List<RecordHeader> headers, String key, String value) {
    Optional.ofNullable(value).ifPresent(it -> headers.add(toRecordHeader(key, it)));
  }

  private RecordHeader toRecordHeader(String key, String value) {
    return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
  }
}
