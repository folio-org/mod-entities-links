package org.folio.entlinks.integration.internal;

import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_MAPPING_RULES_CACHE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.client.MappingRulesClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MappingRulesService {

  private final MappingRulesClient client;

  @Cacheable(cacheNames = AUTHORITY_MAPPING_RULES_CACHE,
             key = "@folioExecutionContext.tenantId",
             unless = "#result.isEmpty()")
  public Map<String, List<String>> getFieldTargetsMappingRelations() {
    var mappingRules = client.fetchAuthorityMappingRules();
    return mappingRules.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, stringListEntry ->
        stringListEntry.getValue().stream().map(MappingRulesClient.MappingRule::target).toList()));
  }
}