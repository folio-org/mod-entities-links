package org.folio.entlinks.exception;

import tools.jackson.core.JacksonException;

/**
 * Exception for situations when an event cannot be published to Kafka.
 */
public class KafkaEventPublishingException extends RuntimeException {

  public KafkaEventPublishingException(JacksonException e) {
    super("Failed to publish event to Kafka topic", e);
  }
}
