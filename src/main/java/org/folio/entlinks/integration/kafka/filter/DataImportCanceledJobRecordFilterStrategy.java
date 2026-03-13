package org.folio.entlinks.integration.kafka.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.integration.di.DataImportCanceledJobService;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class DataImportCanceledJobRecordFilterStrategy implements RecordFilterStrategy<String, DataImportEventPayload> {

  private final DataImportCanceledJobService dataImportCanceledJobService;

  @Override
  public boolean filter(ConsumerRecord<String, DataImportEventPayload> consumerRecord) {
    var dataImportEventPayload = consumerRecord.value();
    var tenantId = dataImportEventPayload.getTenant();
    var jobId = dataImportEventPayload.getJobExecutionId();
    var jobCanceled = dataImportCanceledJobService.isJobCanceled(jobId, tenantId);
    if (jobCanceled) {
      log.debug("Filtering out data import event: the job [jobId {}, tenantId {}] is canceled", jobId, tenantId);
    }
    return jobCanceled;
  }

  @Override
  public boolean ignoreEmptyBatch() {
    return true;
  }
}
