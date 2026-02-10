package org.folio.entlinks.integration.internal;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.entlinks.client.InstanceStorageClient;
import org.folio.entlinks.client.InstanceStorageClient.InventoryInstanceDto;
import org.folio.entlinks.client.InstanceStorageClient.InventoryInstanceDtoCollection;
import org.folio.entlinks.config.properties.InstanceStorageProperties;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceStorageService {

  private static final String CQL_TEMPLATE = "id==(%s)";
  private static final String CQL_DELIMITER = " or ";
  private final InstanceStorageProperties instanceStorageProperties;
  private final InstanceStorageClient client;

  public Map<String, Pair<String, String>> getInstanceData(List<String> instanceIds) {
    int batchSize = instanceStorageProperties.getBatchSize();
    log.info("Fetching instance data [count: {}, with batch size: {}]", instanceIds.size(), batchSize);
    log.trace("Fetching instance data for [instance ids: {}]", instanceIds);
    return Lists.partition(instanceIds, batchSize).stream()
      .map(ids -> fetchInstances(buildCql(ids), ids.size()))
      .filter(Optional::isPresent)
      .map(collection -> collection.get().instances())
      .flatMap(Collection::stream)
      .collect(Collectors.toMap(InventoryInstanceDto::id, dto -> Pair.of(dto.title(), dto.source())));
  }

  private String buildCql(List<String> instanceIds) {
    var instanceIdsString = String.join(CQL_DELIMITER, instanceIds);
    return String.format(CQL_TEMPLATE, instanceIdsString);
  }

  private Optional<InventoryInstanceDtoCollection> fetchInstances(String query, int limit) {
    try {
      log.info("Fetching instances for query: {}, limit: {}", query, limit);
      return Optional.of(client.getInstanceStorageInstances(query, limit));
    } catch (Exception e) {
      log.warn("Failed to fetch instances for query: {}, limit: {}", query, limit, e);
      return Optional.empty();
    }
  }
}
