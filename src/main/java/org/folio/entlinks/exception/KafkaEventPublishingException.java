package org.folio.entlinks.exception;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Exception for situations when an event cannot be published to Kafka.
 */
public class KafkaEventPublishingException extends RuntimeException {

  public KafkaEventPublishingException(JsonProcessingException e) {
    super("Failed to publish event to Kafka topic", e);
  }
}
