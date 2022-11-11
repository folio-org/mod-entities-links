package org.folio.entlinks.integration;

import static org.folio.spring.tools.config.RetryTemplateConfiguration.DEFAULT_KAFKA_RETRY_TEMPLATE_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.message.FormattedMessageFactory;
import org.folio.entlinks.model.projection.LinkCountView;
import org.folio.entlinks.repository.InstanceLinkRepository;
import org.folio.entlinks.service.AuthorityChangeHandlingService;
import org.folio.qm.domain.dto.InventoryEvent;
import org.folio.spring.tools.batch.MessageBatchProcessor;
import org.folio.spring.tools.systemuser.SystemUserScopedExecutionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class AuthorityEventListener {

  private final InstanceLinkRepository repository;
  private final SystemUserScopedExecutionService executionService;
  private final AuthorityChangeHandlingService authorityChangeHandlingService;
  private final MessageBatchProcessor messageBatchProcessor;

  @KafkaListener(
    id = "mod-entities-links-authority-listener",
    containerFactory = "authorityListenerFactory",
    topicPattern = "#{folioKafkaProperties.listener['authority'].topicPattern}",
    groupId = "#{folioKafkaProperties.listener['authority'].groupId}",
    concurrency = "#{folioKafkaProperties.listener['authority'].concurrency}")
  public void handleEvents(List<ConsumerRecord<String, InventoryEvent>> consumerRecords) {
    log.info("Processing authorities from Kafka events [number of records: {}]", consumerRecords.size());

    var inventoryEvents = consumerRecords.stream()
      .map(ConsumerRecord::value)
      .collect(Collectors.groupingBy(InventoryEvent::getTenant));

    inventoryEvents.forEach(this::handleAuthorityEventsForTenant);
  }

  private void handleAuthorityEventsForTenant(String tenant, List<InventoryEvent> events) {
    executionService.executeSystemUserScoped(tenant, () -> {
      var batch = retainAuthoritiesWithLinks(events);
      log.info("Triggering updates for authority records [number of records: {}]", batch.size());
      messageBatchProcessor.consumeBatchWithFallback(batch, DEFAULT_KAFKA_RETRY_TEMPLATE_NAME,
        authorityChangeHandlingService::handleAuthoritiesChanges, this::logFailedEvent);
      return null;
    });
  }

  private List<InventoryEvent> retainAuthoritiesWithLinks(List<InventoryEvent> inventoryEvents) {
    var events = new ArrayList<>(inventoryEvents);
    var incomingAuthorityIds = events.stream()
      .map(InventoryEvent::getId)
      .toList();

    var authorityWithLinksIds = repository.countLinksByAuthorityIds(incomingAuthorityIds).stream()
      .map(LinkCountView::getId)
      .toList();
    events.removeIf(event -> {
      log.debug("Skip message. Authority record [id: {}] doesn't have links", event.getId());
      return !authorityWithLinksIds.contains(event.getId());
    });
    return events;
  }

  private void logFailedEvent(InventoryEvent event, Exception e) {
    if (event == null) {
      log.warn("Failed to process authority event [event: null]", e);
      return;
    }

    log.warn(() -> new FormattedMessageFactory()
      .newMessage("Failed to process authority event [eventType: {}, id: {}, tenant: {}]",
        event.getType(), event.getId(), event.getTenant()), e);
  }
}
