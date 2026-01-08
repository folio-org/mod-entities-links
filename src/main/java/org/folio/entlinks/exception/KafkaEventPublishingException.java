package org.folio.entlinks.exception;

/**
 * Exception for situations when an event cannot be published to Kafka.
 */
public class KafkaEventPublishingException extends RuntimeException {

  public KafkaEventPublishingException(Throwable cause) {
    super("Failed to publish event to Kafka topic", cause);
  }
}
