package org.folio.entlinks.service.consortium;

import static org.folio.entlinks.config.constants.CacheNames.CONSORTIUM_TENANTS_CACHE;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.ConsortiumTenantsClient;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class ConsortiumTenantsService {

  private static final int DEFAULT_REQUEST_LIMIT = 10000;

  private final UserTenantsService userTenantsService;
  private final ConsortiumTenantsClient consortiumTenantsClient;
  private final FolioExecutionContext context;

  /**
   * Get consortium tenants for tenantId.
   *
   * @return only consortium member tenants
  * */
  @Cacheable(cacheNames = CONSORTIUM_TENANTS_CACHE, key = "@folioExecutionContext.tenantId + ':' + #tenantId")
  public List<String> getConsortiumTenants(String tenantId) {
    try {
      return userTenantsService.getConsortiumId(tenantId)
        .map(consortiumId -> consortiumTenantsClient.getConsortiumTenants(consortiumId, DEFAULT_REQUEST_LIMIT))
        .map(ConsortiumTenantsClient.ConsortiumTenants::tenants)
        .map(this::getTenantsList)
        .orElse(Collections.emptyList());
    } catch (Exception e) {
      log.debug("Unexpected exception occurred while trying to get consortium tenants", e);
      return Collections.emptyList();
    }
  }

  /**
   * Check if current context tenant is a consortium central tenant.
   * */
  public boolean isCentralTenantContext() {
    return userTenantsService.getCentralTenant(context.getTenantId())
      .map(tenantId -> Objects.equals(tenantId, context.getTenantId()))
      .orElse(false);
  }

  private List<String> getTenantsList(List<ConsortiumTenantsClient.ConsortiumTenant> consortiumTenants) {
    return consortiumTenants.stream()
      .filter(consortiumTenant -> !consortiumTenant.isCentral())
      .map(ConsortiumTenantsClient.ConsortiumTenant::id)
      .toList();
  }
}
