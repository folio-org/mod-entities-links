package org.folio.entlinks.integration.kafka;

import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.JOB_EXECUTION_ID_HEADER;
import static org.folio.kafka.headers.FolioKafkaHeaders.TENANT_ID;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.integration.di.DataImportCanceledJobService;
import org.folio.entlinks.integration.di.DataImportEventService;
import org.folio.entlinks.integration.kafka.model.DataImportEventWrapper;
import org.folio.spring.scope.FolioExecutionContextService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class DataImportEventListener {

  private final FolioExecutionContextService executionService;
  private final DataImportCanceledJobService canceledJobService;
  private final DataImportEventService eventService;

  @KafkaListener(id = "mod-entities-links-data-import-listener",
                 containerFactory = "diListenerFactory",
                 topicPattern = "#{folioKafkaProperties.listener['data-import'].topicPattern}",
                 groupId = "#{folioKafkaProperties.listener['data-import'].groupId}",
                 concurrency = "#{folioKafkaProperties.listener['data-import'].concurrency}")
  public void handleEvents(List<DataImportEventWrapper> consumerRecords) {
    log.info("Processing data-import event [number of records: {}]", consumerRecords.size());

    var eventByTenant = consumerRecords.stream()
      .collect(Collectors.groupingBy(DataImportEventWrapper::tenant));
    var allFutures = new ArrayList<CompletableFuture<Void>>();

    for (var entry : eventByTenant.entrySet()) {
      var tenant = entry.getKey();
      var records = entry.getValue();
      var futures = executionService.execute(tenant, records.getFirst().getHeadersMap(),
        () -> records.stream()
          .map(diEvent -> eventService.processEvent(diEvent.payload()))
          .toList());
      allFutures.addAll(futures);
    }

    CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
  }

  @KafkaListener(id = "mod-entities-links-data-import-canceled-listener",
                 containerFactory = "diCanceledListenerFactory",
                 topicPattern = "#{folioKafkaProperties.listener['data-import-canceled'].topicPattern}",
                 groupId = "#{folioKafkaProperties.listener['data-import-canceled'].groupId}",
                 concurrency = "#{folioKafkaProperties.listener['data-import-canceled'].concurrency}")
  public void handleDataImportCanceledEvents(
    @Header(name = RECEIVED_KEY, required = false) String key,
    @Header(name = TENANT_ID, required = false) String tenantId,
    @Header(name = JOB_EXECUTION_ID_HEADER, required = false) String jobExecutionId) {
    log.info("Processing data-import canceled event for key={}.", key);
    if (tenantId == null || jobExecutionId == null) {
      log.warn("Cannot process data-import canceled event. tenantId or jobExecutionId header is missing.");
      return;
    }
    canceledJobService.registerCanceledJob(jobExecutionId, tenantId);
  }
}
