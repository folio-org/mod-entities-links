package org.folio.entlinks.integration.kafka;

import static java.util.UUID.randomUUID;
import static org.folio.rspec.domain.dto.SpecificationUpdatedEvent.UpdateExtent.FULL;
import static org.folio.rspec.domain.dto.SpecificationUpdatedEvent.UpdateExtent.PARTIAL;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.concurrent.Callable;
import org.folio.entlinks.service.tenant.MarcSpecificationUpdateService;
import org.folio.rspec.domain.dto.SpecificationUpdatedEvent;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SpecificationChangedEventListenerTest {

  private @InjectMocks SpecificationChangedEventListener listener;
  private @Mock SystemUserScopedExecutionService executionService;
  private @Mock MarcSpecificationUpdateService updateService;

  @BeforeEach
  void setUp() {
    lenient().when(executionService.executeSystemUserScoped(eq(TENANT_ID), any()))
      .thenAnswer(invocation -> ((Callable<?>) invocation.getArgument(1)).call());
  }

  @Test
  void handleEvent_positive_skipPartialUpdateEvents() {
    var event = new SpecificationUpdatedEvent(randomUUID(), TENANT_ID, PARTIAL);

    listener.handleEvent(event);

    verifyNoInteractions(executionService);
    verifyNoInteractions(updateService);
  }

  @Test
  void handleEvent_positive_callUpdateService() {
    var event = new SpecificationUpdatedEvent(randomUUID(), TENANT_ID, FULL);

    listener.handleEvent(event);

    verify(executionService).executeSystemUserScoped(eq(TENANT_ID), any());
    verify(updateService).sendSpecificationRequests();
  }
}
