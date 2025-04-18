package org.folio.entlinks.integration.kafka;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.entlinks.utils.HeaderUtils.extractHeaderValue;
import static org.folio.spring.tools.config.RetryTemplateConfiguration.DEFAULT_KAFKA_RETRY_TEMPLATE_NAME;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.message.FormattedMessageFactory;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.folio.entlinks.service.messaging.authority.InstanceAuthorityLinkUpdateService;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.tools.batch.MessageBatchProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class AuthorityEventListener {

  private final InstanceAuthorityLinkUpdateService instanceAuthorityLinkUpdateService;
  private final SystemUserScopedExecutionService executionService;
  private final MessageBatchProcessor messageBatchProcessor;

  @KafkaListener(id = "mod-entities-links-authority-listener",
                 containerFactory = "authorityListenerFactory",
                 topicPattern = "#{folioKafkaProperties.listener['authority'].topicPattern}",
                 groupId = "#{folioKafkaProperties.listener['authority'].groupId}",
                 concurrency = "#{folioKafkaProperties.listener['authority'].concurrency}")
  public void handleEvents(List<ConsumerRecord<String, AuthorityDomainEvent>> consumerRecords) {
    log.info("Processing authorities from Kafka events [number of records: {}]", consumerRecords.size());

    consumerRecords.stream()
      .collect(Collectors.groupingBy(consumerRecord ->
        extractHeaderValue(XOkapiHeaders.USER_ID, consumerRecord.headers())))
      .forEach(this::handleAuthorityEventsForUser);
  }

  private void handleAuthorityEventsForUser(String userId, List<ConsumerRecord<String, AuthorityDomainEvent>> records) {
    var headers = isBlank(userId)
      ? Collections.<String, Collection<String>>emptyMap()
      : Map.<String, Collection<String>>of(XOkapiHeaders.USER_ID, singletonList(userId));
    records.stream()
      .map(consumerRecord -> {
        var value = consumerRecord.value();
        value.setId(UUID.fromString(consumerRecord.key()));
        return value;
      })
      .collect(Collectors.groupingBy(AuthorityDomainEvent::getTenant))
      .forEach((tenant, events) -> handleAuthorityEventsForTenant(tenant, headers, events));
  }

  private void handleAuthorityEventsForTenant(String tenant, Map<String, Collection<String>> headers,
                                              List<AuthorityDomainEvent> events) {
    executionService.executeSystemUserScoped(tenant, headers, () -> {
      log.info("Triggering updates for authority records [number of records: {}, tenant: {}]", events.size(), tenant);
      messageBatchProcessor.consumeBatchWithFallback(events, DEFAULT_KAFKA_RETRY_TEMPLATE_NAME,
        instanceAuthorityLinkUpdateService::handleAuthoritiesChanges, this::logFailedEvent);
      return null;
    });
  }

  private void logFailedEvent(AuthorityDomainEvent event, Exception e) {
    if (event == null) {
      log.warn("Failed to process authority event [event: null]", e);
      return;
    }

    log.warn(() -> new FormattedMessageFactory().newMessage(
      "Failed to process authority event [eventType: {}, id: {}, tenant: {}]", event.getType(), event.getId(),
      event.getTenant()), e);
  }
}
