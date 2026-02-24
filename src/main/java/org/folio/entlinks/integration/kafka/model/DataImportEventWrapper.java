package org.folio.entlinks.integration.kafka.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.folio.DataImportEventPayload;

public record DataImportEventWrapper(DataImportEventPayload payload, Map<String, String> headers, String tenant) {

  public Map<String, Collection<String>> getHeadersMap() {
    return headers.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, o -> List.of(o.getValue())));
  }
}
