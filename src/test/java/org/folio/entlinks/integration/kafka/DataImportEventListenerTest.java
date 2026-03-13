package org.folio.entlinks.integration.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.integration.di.DataImportCanceledJobService;
import org.folio.entlinks.integration.di.DataImportEventService;
import org.folio.entlinks.integration.kafka.model.DataImportEventWrapper;
import org.folio.spring.scope.FolioExecutionContextService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DataImportEventListenerTest {

  private static final String TENANT_ID = "test-tenant";
  private static final String ANOTHER_TENANT_ID = "another-tenant";
  private static final String JOB_EXECUTION_ID = "job-123";
  private static final String KEY = "record-key";

  @Mock
  private FolioExecutionContextService executionService;
  @Mock
  private DataImportCanceledJobService canceledJobService;
  @Mock
  private DataImportEventService eventService;

  @InjectMocks
  private DataImportEventListener listener;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(executionService, canceledJobService, eventService);
  }

  @Test
  void handleEvents_positive_singleEventProcessedUnderTenantContext() {
    var payload = mock(DataImportEventPayload.class);
    var event = eventWrapper(payload, TENANT_ID);

    when(executionService.execute(eq(TENANT_ID), eq(event.getHeadersMap()),
        ArgumentMatchers.<Callable<List<CompletableFuture<Void>>>>any()))
      .thenAnswer(invokeCallable());
    when(eventService.processEvent(payload)).thenReturn(CompletableFuture.completedFuture(null));

    listener.handleEvents(List.of(event));

    verify(executionService).execute(eq(TENANT_ID), eq(event.getHeadersMap()),
      ArgumentMatchers.<Callable<List<CompletableFuture<Void>>>>any());
    verify(eventService).processEvent(payload);
  }

  @Test
  void handleEvents_positive_eventsGroupedAndProcessedByTenant() {
    var payload1 = mock(DataImportEventPayload.class);
    var payload2 = mock(DataImportEventPayload.class);
    var event1 = eventWrapper(payload1, TENANT_ID);
    var event2 = eventWrapper(payload2, ANOTHER_TENANT_ID);

    when(executionService.execute(eq(TENANT_ID), eq(event1.getHeadersMap()),
        ArgumentMatchers.<Callable<List<CompletableFuture<Void>>>>any()))
      .thenAnswer(invokeCallable());
    when(executionService.execute(eq(ANOTHER_TENANT_ID), eq(event2.getHeadersMap()),
        ArgumentMatchers.<Callable<List<CompletableFuture<Void>>>>any()))
      .thenAnswer(invokeCallable());
    when(eventService.processEvent(payload1)).thenReturn(CompletableFuture.completedFuture(null));
    when(eventService.processEvent(payload2)).thenReturn(CompletableFuture.completedFuture(null));

    listener.handleEvents(List.of(event1, event2));

    verify(executionService).execute(eq(TENANT_ID), eq(event1.getHeadersMap()),
      ArgumentMatchers.<Callable<List<CompletableFuture<Void>>>>any());
    verify(executionService).execute(eq(ANOTHER_TENANT_ID), eq(event2.getHeadersMap()),
      ArgumentMatchers.<Callable<List<CompletableFuture<Void>>>>any());
    verify(eventService).processEvent(payload1);
    verify(eventService).processEvent(payload2);
  }

  @Test
  void handleDataImportCanceledEvents_positive_registersJobWhenBothHeadersPresent() {
    listener.handleDataImportCanceledEvents(KEY, TENANT_ID, JOB_EXECUTION_ID);

    verify(canceledJobService).registerCanceledJob(JOB_EXECUTION_ID, TENANT_ID);
  }

  @Test
  void handleDataImportCanceledEvents_negative_doesNotRegisterWhenTenantIdIsNull() {
    listener.handleDataImportCanceledEvents(KEY, null, JOB_EXECUTION_ID);

    verify(canceledJobService, never()).registerCanceledJob(any(), any());
  }

  @Test
  void handleDataImportCanceledEvents_negative_doesNotRegisterWhenJobExecutionIdIsNull() {
    listener.handleDataImportCanceledEvents(KEY, TENANT_ID, null);

    verify(canceledJobService, never()).registerCanceledJob(any(), any());
  }

  private static DataImportEventWrapper eventWrapper(DataImportEventPayload payload, String tenantId) {
    return new DataImportEventWrapper(payload, Map.of("X-Okapi-Tenant", tenantId), tenantId);
  }

  private static Answer<Object> invokeCallable() {
    return invocation -> {
      var callable = invocation.getArgument(2, Callable.class);
      return callable.call();
    };
  }
}
