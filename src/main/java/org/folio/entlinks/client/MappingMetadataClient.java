package org.folio.entlinks.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("mapping-metadata")
public interface MappingMetadataClient {

  @GetExchange(value = "/{jobExecutionId}", accept = APPLICATION_JSON_VALUE)
  MappingMetadata getMappingMetadata(@PathVariable String jobExecutionId);

  record MappingMetadata(String jobExecutionId, String mappingRules, String mappingParams) { }
}
