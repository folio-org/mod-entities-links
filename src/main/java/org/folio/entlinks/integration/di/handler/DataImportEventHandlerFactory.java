package org.folio.entlinks.integration.di.handler;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.folio.processing.events.services.handler.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Factory for managing data import event handlers.
 * This factory provides a centralized registry of all event handlers as Spring beans.
 * New handlers are automatically discovered through dependency injection.
 */
@Getter
@Component
@RequiredArgsConstructor
public class DataImportEventHandlerFactory {

  private final List<EventHandler> eventHandlers;
}
