package org.folio.entlinks.integration.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.integration.di.DataImportEventService;
import org.folio.entlinks.integration.kafka.model.DataImportEventWrapper;
import org.folio.spring.scope.FolioExecutionContextService;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
@Profile("dev") //todo: remove
public class DataImportEventListener {

  private final FolioExecutionContextService executionService;
  private final DataImportEventService eventService;

  @KafkaListener(id = "mod-entities-links-data-import-listener",
                 containerFactory = "diListenerFactory",
                 topicPattern = "#{folioKafkaProperties.listener['data-import'].topicPattern}",
                 groupId = "#{folioKafkaProperties.listener['data-import'].groupId}",
                 concurrency = "#{folioKafkaProperties.listener['data-import'].concurrency}")
  public void handleEvents(List<DataImportEventWrapper> consumerRecords) {
    long startTime = System.currentTimeMillis();
    int recordCount = consumerRecords.size();

    log.info("====== DataImportEventListener.handleEvents() START ====== [records: {}]", recordCount);

    var eventByTenant = consumerRecords.stream()
      .collect(Collectors.groupingBy(DataImportEventWrapper::tenant));

    log.info("Grouped by tenant [tenantCount: {}, recordCount: {}]", eventByTenant.size(), recordCount);

    List<CompletableFuture<Void>> allFutures = new ArrayList<>();
    
    for (var entry : eventByTenant.entrySet()) {
      var tenant = entry.getKey();
      var records = entry.getValue();

      log.info("Processing tenant group [tenant: {}, recordsInGroup: {}]", tenant, records.size());

      var futures = executionService.execute(tenant, records.getFirst().getHeadersMap(),
        () -> records.stream()
          .map(diEvent -> {
            log.info("Processing individual event [tenant: {}, eventType: {}, jobExecutionId: {}]",
              tenant, diEvent.payload().getEventType(), diEvent.payload().getJobExecutionId());
            return eventService.processEvent(diEvent.payload());
          })
          .toList());
      allFutures.addAll(futures);

      log.info("Futures created for tenant [tenant: {}, futuresCount: {}]", tenant, futures.size());
    }

    log.info("About to wait for all futures [totalFutures: {}]", allFutures.size());
    long beforeJoin = System.currentTimeMillis();

    CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();

    long joinDuration = System.currentTimeMillis() - beforeJoin;
    long totalDuration = System.currentTimeMillis() - startTime;

    log.info("====== DataImportEventListener.handleEvents() COMPLETE ====== [records: {}, totalDuration: {}ms, joinDuration: {}ms]",
      recordCount, totalDuration, joinDuration);
  }
}
