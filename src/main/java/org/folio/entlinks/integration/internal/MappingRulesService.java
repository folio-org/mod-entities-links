package org.folio.entlinks.integration.internal;

import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_MAPPING_METADATA_CACHE;
import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_MAPPING_RULES_CACHE;

import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.MappingMetadataClient;
import org.folio.entlinks.client.MappingRulesClient;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.integration.dto.MappingMetadata;
import org.folio.processing.mapping.defaultmapper.processor.parameters.MappingParameters;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class MappingRulesService {

  private final MappingRulesClient mappingRulesClient;
  private final MappingMetadataClient mappingMetadataClient;

  @Cacheable(cacheNames = AUTHORITY_MAPPING_RULES_CACHE,
             key = "@folioExecutionContext.tenantId",
             unless = "#result.isEmpty()")
  public Map<String, List<String>> getFieldTargetsMappingRelations() {
    log.info("Fetching authority mapping rules");
    var mappingRules = fetchMappingRules();
    return mappingRules.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, rulesList ->
        rulesList.getValue().stream().map(MappingRulesClient.MappingRule::target).toList()));
  }

  @Cacheable(cacheNames = AUTHORITY_MAPPING_METADATA_CACHE,
             key = "@folioExecutionContext.tenantId + ':' + #dataImportJobId")
  public MappingMetadata getMappingMetadata(String dataImportJobId) {
    try {
      var mappingMetadata = mappingMetadataClient.getMappingMetadata(dataImportJobId);
      var mappingParameters = new JsonObject(mappingMetadata.mappingParams()).mapTo(MappingParameters.class);
      var mappingRules = new JsonObject(mappingMetadata.mappingRules());
      return new MappingMetadata(mappingParameters, mappingRules);
    } catch (Exception e) {
      throw new FolioIntegrationException("Failed to fetch authority mapping metadata for dataImportJobId: "
                                          + dataImportJobId, e);
    }
  }

  private Map<String, List<MappingRulesClient.MappingRule>> fetchMappingRules() {
    try {
      return mappingRulesClient.fetchAuthorityMappingRules();
    } catch (Exception e) {
      throw new FolioIntegrationException("Failed to fetch authority mapping rules", e);
    }
  }
}
