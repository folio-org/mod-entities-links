package org.folio.entlinks.integration.di;

import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.CHUNK_ID_HEADER;
import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.JOB_EXECUTION_ID_HEADER;
import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.RECORD_ID_HEADER;
import static org.folio.processing.events.utils.EventUtils.extractRecordId;
import static org.folio.rest.util.OkapiConnectionParams.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.PERMISSIONS;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;
import static org.folio.spring.tools.kafka.FolioKafkaProperties.TENANT_ID;

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
import org.springframework.kafka.core.RoutingKafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring-managed implementation of EventPublisher for publishing data import events to Kafka.
 *
 * <p>Uses {@link RoutingKafkaTemplate} to automatically route sends to different Kafka producers
 * based on topic patterns. This prevents concurrent metadata fetch interference when Spring Kafka's
 * producer blocks synchronously to fetch topic metadata for different topics.</p>
 *
 * <p>EventManager response events (DI_COMPLETED, DI_ERROR) and handler events
 * (DI_INVENTORY_AUTHORITY_UPDATED, etc.) are routed to separate producers automatically.</p>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class DataImportEventPublisher implements EventPublisher {

  private static final String DEFAULT_NAMESPACE = "Default";

  private final JsonMapper jsonMapper;
  private final RoutingKafkaTemplate kafkaTemplate;
  private final ApplicationMetadata applicationMetadata;

  @Override
  public CompletableFuture<Event> publish(DataImportEventPayload payload) {
    var eventName = payload.getEventType();
    var topicName = KafkaUtils.getTenantTopicNameWithNamespace(eventName,
      FolioEnvironment.getFolioEnvName(),
      payload.getTenant(),
      DEFAULT_NAMESPACE);

    log.debug("Publishing event [eventType: {}, topic: {}]", eventName, topicName);

    var event = prepareEvent(payload);

    // Create ProducerRecord with headers
    var producerRecord = new ProducerRecord<Object, Object>(topicName, extractRecordId(payload), event);
    prepareHeaders(payload, producerRecord);

    return kafkaTemplate.send(producerRecord)
      .handle((recordMetadata, ex) -> {
        if (ex != null) {
          log.error("Failed to publish event [eventType: {}, topic: {}]", eventName, topicName, ex);
          return null;
        }
        log.debug("Published event [eventType: {}, topic: {}, partition: {}, offset: {}]",
          eventName, topicName, recordMetadata.getRecordMetadata().partition(),
          recordMetadata.getRecordMetadata().offset());
        return event;
      });
  }

  private Event prepareEvent(DataImportEventPayload payload) {
    try {
      return new Event()
        .withId(UUID.randomUUID().toString())
        .withEventPayload(jsonMapper.writeValueAsString(payload))
        .withEventMetadata(new EventMetadata()
          .withEventTTL(1)
          .withTenantId(payload.getTenant())
          .withPublishedBy(applicationMetadata.getFullApplicationName())
          .withPublishedDate(new Date()));
    } catch (JacksonException e) {
      throw new KafkaEventPublishingException(e);
    }
  }

  private void prepareHeaders(DataImportEventPayload payload, ProducerRecord<Object, Object> producerRecord) {
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
