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
    log.info("Processing data-import event [number of records: {}]", consumerRecords.size());
    
    var eventByTenant = consumerRecords.stream()
      .collect(Collectors.groupingBy(DataImportEventWrapper::tenant));
    List<CompletableFuture<Void>> allFutures = new ArrayList<>();
    
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
}
