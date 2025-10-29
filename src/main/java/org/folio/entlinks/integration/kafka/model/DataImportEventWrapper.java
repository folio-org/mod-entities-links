package org.folio.entlinks.integration.kafka.model;

import java.util.Map;
import org.folio.DataImportEventPayload;

public record DataImportEventWrapper(DataImportEventPayload payload, Map<String, String> headers, String tenant) {

}
