package org.folio.entlinks.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("consortia")
public interface ConsortiumTenantsClient {

  @GetMapping(value = "{consortiumId}/tenants", produces = APPLICATION_JSON_VALUE)
  ConsortiumTenants getConsortiumTenants(@RequestParam("consortiumId") String consortiumId);

  record ConsortiumTenants(List<ConsortiumTenant> tenants) { }

  record ConsortiumTenant(String id) { }
}
