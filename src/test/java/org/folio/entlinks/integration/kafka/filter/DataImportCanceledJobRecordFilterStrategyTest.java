package org.folio.entlinks.integration.kafka.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.integration.di.DataImportCanceledJobService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DataImportCanceledJobRecordFilterStrategyTest {

  private static final String JOB_ID = "test-job-id";
  private static final String TENANT_ID = "test-tenant";

  @Mock
  private DataImportCanceledJobService dataImportCanceledJobService;
  @Mock
  private ConsumerRecord<String, DataImportEventPayload> consumerRecord;

  @InjectMocks
  private DataImportCanceledJobRecordFilterStrategy filterStrategy;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(dataImportCanceledJobService, consumerRecord);
  }

  @Test
  void filter_positive_jobIsCanceled() {
    // Arrange
    var payload = new DataImportEventPayload().withJobExecutionId(JOB_ID).withTenant(TENANT_ID);
    when(consumerRecord.value()).thenReturn(payload);
    when(dataImportCanceledJobService.isJobCanceled(JOB_ID, TENANT_ID)).thenReturn(true);

    // Act
    var result = filterStrategy.filter(consumerRecord);

    // Assert
    assertTrue(result);
    verify(dataImportCanceledJobService).isJobCanceled(JOB_ID, TENANT_ID);
  }

  @Test
  void filter_negative_jobIsNotCanceled() {
    // Arrange
    var payload = new DataImportEventPayload().withJobExecutionId(JOB_ID).withTenant(TENANT_ID);
    when(consumerRecord.value()).thenReturn(payload);
    when(dataImportCanceledJobService.isJobCanceled(JOB_ID, TENANT_ID)).thenReturn(false);

    // Act
    var result = filterStrategy.filter(consumerRecord);

    // Assert
    assertFalse(result);
    verify(dataImportCanceledJobService).isJobCanceled(JOB_ID, TENANT_ID);
  }

  @Test
  void ignoreEmptyBatch_positive_returnsTrue() {
    assertTrue(filterStrategy.ignoreEmptyBatch());
  }
}
