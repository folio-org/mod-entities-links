package org.folio.entlinks.integration.kafka;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.folio.support.MockingTestUtils.mockBatchFailedHandling;
import static org.folio.support.MockingTestUtils.mockBatchSuccessHandling;
import static org.folio.support.TestDataUtils.report;
import static org.folio.support.base.TestConstants.CENTRAL_TENANT_ID;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.testing.type.UnitTest;
import org.folio.spring.tools.batch.MessageBatchProcessor;
import org.folio.support.KafkaTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinkUpdateReportEventListenerTest {

  @Mock private SystemUserScopedExecutionService executionService;
  @Mock private MessageBatchProcessor messageBatchProcessor;
  @Mock private InstanceAuthorityLinkingService linkingService;

  @InjectMocks
  private LinkUpdateReportEventListener listener;

  @BeforeEach
  void setUp() {
    lenient().when(executionService.executeSystemUserScoped(any(), any())).thenAnswer(invocation -> {
      var argument = invocation.getArgument(1, Callable.class);
      return argument.call();
    });
  }

  // Test that multiple tenants processed in different batches and jobIds in different sub-batches
  @Test
  void shouldHandleEvent_positive() {
    var job1Id = UUID.randomUUID();
    var job2Id = UUID.randomUUID();
    var reports = List.of(
      report(TENANT_ID, job1Id),
      report(TENANT_ID, job2Id),
      report(CENTRAL_TENANT_ID, job1Id),
      report(CENTRAL_TENANT_ID, job1Id)
    );
    var consumerRecords = KafkaTestUtils.consumerRecords(reports);

    mockBatchSuccessHandling(messageBatchProcessor);

    listener.handleEvents(consumerRecords);

    verify(executionService).executeSystemUserScoped(eq(TENANT_ID), any());
    verify(executionService).executeSystemUserScoped(eq(CENTRAL_TENANT_ID), any());
    verify(messageBatchProcessor, times(2))
      .consumeBatchWithFallback(any(), any(), any(), any());

    verify(linkingService)
      .updateForReports(job1Id, singletonList(reports.get(0)));
    verify(linkingService)
      .updateForReports(job2Id, singletonList(reports.get(1)));
    verify(linkingService)
      .updateForReports(job1Id, List.of(reports.get(2), reports.get(3)));
  }

  @Test
  void shouldHandleEvent_singleTenantAndJob() {
    var jobId = UUID.randomUUID();
    var reports = List.of(
      report(TENANT_ID, jobId),
      report(TENANT_ID, jobId)
    );
    var consumerRecords = KafkaTestUtils.consumerRecords(reports);

    mockBatchSuccessHandling(messageBatchProcessor);

    listener.handleEvents(consumerRecords);

    verify(executionService).executeSystemUserScoped(eq(TENANT_ID), any());
    verify(messageBatchProcessor).consumeBatchWithFallback(any(), any(), any(), any());
    verify(linkingService).updateForReports(jobId, reports);
  }

  @Test
  void shouldHandleEmptyList() {
    var consumerRecords = KafkaTestUtils.consumerRecords(emptyList());

    listener.handleEvents(consumerRecords);

    verifyNoInteractions(executionService);
    verifyNoInteractions(messageBatchProcessor);
    verifyNoInteractions(linkingService);
  }

  @Test
  void shouldNotHandleEvent_negative_whenExceptionOccurred() {
    var report = report(insecure().nextAlphabetic(10), UUID.randomUUID());
    var consumerRecords = KafkaTestUtils.consumerRecords(singletonList(report));

    mockBatchFailedHandling(messageBatchProcessor, new RuntimeException("test message"));

    listener.handleEvents(consumerRecords);

    verifyNoInteractions(linkingService);
  }
}
