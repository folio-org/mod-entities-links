package org.folio.entlinks.integration.di;

import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.logDataImport;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.integration.di.handler.DataImportEventHandlerFactory;
import org.folio.processing.events.EventManager;
import org.folio.rest.jaxrs.model.ProfileSnapshotWrapper;
import org.springframework.stereotype.Service;

/**
 * Service for handling data import events.
 * This service wraps the EventManager and registers handlers and publishers on initialization.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class DataImportEventService {

  private final DataImportEventHandlerFactory handlerFactory;
  private final DataImportEventPublisher eventPublisher;

  /**
   * Initializes the event handling system by registering all handlers and the publisher.
   * This is called automatically by Spring after bean construction.
   */
  @PostConstruct
  public void initialize() {
    log.info("Initializing Data Import Event Service");
    registerEventHandlers();
    registerEventPublisher();
    log.info("Data Import Event Service initialized with {} handlers", handlerFactory.getEventHandlers().size());
  }

  /**
   * Processes a data import event through the event handling pipeline.
   *
   * @param payload the event payload to process
   * @return a CompletableFuture that completes when event processing is done
   */
  public CompletableFuture<Void> processEvent(DataImportEventPayload payload) {
    var eventType = payload.getEventType();
    logDataImport(log, Level.INFO, "Processing data-import %s event".formatted(eventType), payload);
    return EventManager.handleEvent(payload, new ProfileSnapshotWrapper())
      .handle((diPayload, throwable) -> {
        if (throwable != null) {
          logDataImport(log, "Process data-import %s event failed".formatted(eventType), diPayload, throwable);
        } else {
          logDataImport(log, Level.INFO, "Data-import %s event processed".formatted(eventType), diPayload);
        }
        return null;
      });
  }

  /**
   * Registers all event handlers from the factory with the EventManager.
   */
  private void registerEventHandlers() {
    var handlers = handlerFactory.getEventHandlers();
    handlers.forEach(handler -> {
      EventManager.registerEventHandler(handler);
      log.debug("Registered event handler: {}", handler.getClass().getSimpleName());
    });
  }

  /**
   * Registers the event publisher with the EventManager.
   */
  private void registerEventPublisher() {
    EventManager.registerCustomKafkaEventPublisher(eventPublisher);
    log.debug("Registered event publisher: {}", eventPublisher.getClass().getSimpleName());
  }
}
