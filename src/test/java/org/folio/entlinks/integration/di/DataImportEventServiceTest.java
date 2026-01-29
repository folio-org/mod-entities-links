package org.folio.entlinks.integration.di;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.integration.di.handler.DataImportEventHandlerFactory;
import org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils;
import org.folio.processing.events.EventManager;
import org.folio.processing.events.services.handler.EventHandler;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DataImportEventServiceTest {

  @Mock
  private DataImportEventHandlerFactory handlerFactory;
  @Mock
  private DataImportEventPublisher eventPublisher;
  @Mock
  private EventHandler eventHandler;
  @InjectMocks
  private DataImportEventService service;

  @Test
  void initialize_shouldRegisterEventHandlersAndPublisher() {
    when(handlerFactory.getEventHandlers()).thenReturn(List.of(eventHandler));
    try (MockedStatic<EventManager> eventManager = mockStatic(EventManager.class)) {

      // Act
      service.initialize();

      // Verify
      eventManager.verify(() -> EventManager.registerEventHandler(eventHandler));
      eventManager.verify(() -> EventManager.registerCustomKafkaEventPublisher(eventPublisher));
    }
  }

  @Test
  void processEvent_shouldHandleEventSuccessfully() {
    var payload = new DataImportEventPayload();
    try (MockedStatic<EventManager> eventManager = mockStatic(EventManager.class);
         MockedStatic<DataImportEventHandlerUtils> logger = mockStatic(DataImportEventHandlerUtils.class)) {
      eventManager.when(() -> EventManager.handleEvent(any(), any()))
          .thenReturn(CompletableFuture.completedFuture(payload));

      // Act
      service.processEvent(payload);

      // Verify
      eventManager.verify(() -> EventManager.handleEvent(any(), any()), times(1));
      logger.verify(() -> DataImportEventHandlerUtils.logDataImport(any(Logger.class), eq(Level.INFO), anyString(),
          eq(payload)), times(2));
    }
  }

  @Test
  void processEvent_shouldHandleEventWithException() {
    var payload = new DataImportEventPayload();
    var future = CompletableFuture.failedFuture(new RuntimeException("test exception"));
    try (MockedStatic<EventManager> eventManager = mockStatic(EventManager.class);
         MockedStatic<DataImportEventHandlerUtils> logger = mockStatic(DataImportEventHandlerUtils.class)) {
      eventManager.when(() -> EventManager.handleEvent(any(), any())).thenReturn(future);

      // Act
      service.processEvent(payload);

      // Verify
      eventManager.verify(() -> EventManager.handleEvent(any(), any()), times(1));
      logger.verify(() -> DataImportEventHandlerUtils.logDataImport(any(Logger.class), eq(Level.INFO), anyString(),
          eq(payload)), times(1));
      logger.verify(() -> DataImportEventHandlerUtils.logDataImport(any(Logger.class), anyString(),
          any(), any(Throwable.class)), times(1));
    }
  }
}
