package org.folio.entlinks.integration.kafka;

import static java.util.UUID.randomUUID;
import static org.folio.rspec.domain.dto.Family.MARC;
import static org.folio.rspec.domain.dto.FamilyProfile.AUTHORITY;
import static org.folio.rspec.domain.dto.FamilyProfile.BIBLIOGRAPHIC;
import static org.folio.rspec.domain.dto.SpecificationUpdatedEvent.UpdateExtent.FULL;
import static org.folio.rspec.domain.dto.SpecificationUpdatedEvent.UpdateExtent.PARTIAL;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.concurrent.Callable;
import org.folio.entlinks.integration.internal.MarcSpecificationUpdateService;
import org.folio.rspec.domain.dto.SpecificationUpdatedEvent;
import org.folio.spring.scope.FolioExecutionContextService;
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
  private @Mock FolioExecutionContextService executionService;
  private @Mock MarcSpecificationUpdateService updateService;

  @BeforeEach
  void setUp() {
    lenient().when(executionService.execute(eq(TENANT_ID), anyMap(), any(Callable.class)))
      .thenAnswer(invocation -> ((Callable<?>) invocation.getArgument(2)).call());
  }

  @Test
  void handleEvent_positive_skipPartialUpdateEvents() {
    var event = new SpecificationUpdatedEvent(randomUUID(), TENANT_ID, MARC, BIBLIOGRAPHIC, PARTIAL);

    listener.handleEvent(event);

    verifyNoInteractions(executionService);
    verifyNoInteractions(updateService);
  }

  @Test
  void handleEvent_positive_skipFullAuthorityUpdateEvents() {
    var event = new SpecificationUpdatedEvent(randomUUID(), TENANT_ID, MARC, AUTHORITY, FULL);

    listener.handleEvent(event);

    verifyNoInteractions(executionService);
    verifyNoInteractions(updateService);
  }

  @Test
  void handleEvent_positive_callUpdateService() {
    var event = new SpecificationUpdatedEvent(randomUUID(), TENANT_ID, MARC, BIBLIOGRAPHIC, FULL);

    listener.handleEvent(event);

    verify(executionService).execute(eq(TENANT_ID), anyMap(), any(Callable.class));
    verify(updateService).sendSpecificationRequests();
  }
}
