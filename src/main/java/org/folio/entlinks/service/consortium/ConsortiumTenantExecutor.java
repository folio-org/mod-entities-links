package org.folio.entlinks.service.consortium;

import java.util.Objects;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class ConsortiumTenantExecutor {

  private final UserTenantsService userTenantsService;
  private final FolioExecutionContext folioExecutionContext;
  private final SystemUserScopedExecutionService scopedExecutionService;

  /**
   * Executes the given operation within the context of the central tenant of the consortium.
   * Returns null when called from a non-consortium tenant.
   * */
  public <T> T executeAsCentralTenant(Supplier<T> operation) {
    return executeAsCentralTenantInternal(operation, false);
  }

  /**
   * Executes the given operation within the context of the central tenant of the consortium.
   * Executes only when called from a consortium member tenant context.
   * Returns null otherwise. F.e. when called from a central tenant context or a non-consortium tenant.
   * */
  public <T> T executeAsCentralTenantForMember(Supplier<T> operation) {
    return executeAsCentralTenantInternal(operation, true);
  }

  private <T> T executeAsCentralTenantInternal(Supplier<T> operation, boolean mustBeForMember) {
    var tenantId = folioExecutionContext.getTenantId();
    var centralTenantId = userTenantsService.getCentralTenant(tenantId);

    if (centralTenantId.isEmpty()) {
      log.warn("Tenant: {} is not in consortia", tenantId);
      return null;
    } else if (mustBeForMember && Objects.equals(tenantId, centralTenantId.get())) {
      log.warn("Tenant: {} is a central tenant, expected a member tenant", tenantId);
      return null;
    }

    log.info("Changing context from {} to {}", tenantId, centralTenantId.get());
    return scopedExecutionService.executeSystemUserScoped(centralTenantId.get(), operation::get);
  }
}
