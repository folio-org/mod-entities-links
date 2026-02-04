package org.folio.entlinks.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Map;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("mapping-rules")
public interface MappingRulesClient {

  @GetExchange(value = "/marc-authority", accept = APPLICATION_JSON_VALUE)
  Map<String, List<MappingRule>> fetchAuthorityMappingRules();

  record MappingRule(String target) { }
}
