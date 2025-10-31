package org.folio.entlinks.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("mapping-metadata")
public interface MappingMetadataClient {

  @GetMapping(value = "/{jobExecutionId}", produces = APPLICATION_JSON_VALUE)
  MappingMetadata getMappingMetadata(@PathVariable String jobExecutionId);

  record MappingMetadata(String jobExecutionId, String mappingRules, String mappingParams) { }
}
