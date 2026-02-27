package org.folio.entlinks.service.consortium.propagation;

import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.scheduling.annotation.Async;

@Log4j2
public abstract class ConsortiumPropagationService<T> {

  private final ConsortiumTenantsService tenantsService;
  private final SystemUserScopedExecutionService executionService;
  private final FolioExecutionContext folioExecutionContext;

  protected ConsortiumPropagationService(ConsortiumTenantsService tenantsService,
                                         SystemUserScopedExecutionService executionService,
                                         FolioExecutionContext folioExecutionContext) {
    this.tenantsService = tenantsService;
    this.executionService = executionService;
    this.folioExecutionContext = folioExecutionContext;
  }

  @Async
  public void propagate(T entity, PropagationType propagationType,
                        String tenantId) {
    log.info("Try to propagate [entity: {}, propagationType: {}, context: {}]", entity.getClass().getSimpleName(),
      propagationType, tenantId);
    log.debug("Try to propagate [entity: {}, propagationType: {}, context: {}]", entity, propagationType, tenantId);
    try {
      var userId = Optional.ofNullable(folioExecutionContext.getUserId())
        .map(UUID::toString)
        .orElse(null);
      var consortiumTenants = tenantsService.getConsortiumTenants(tenantId);
      log.debug("Find consortium tenants for propagation: {}, context: {}", consortiumTenants, tenantId);
      for (var consortiumTenant : consortiumTenants) {
        executionService.executeAsyncSystemUserScoped(consortiumTenant, userId,
          () -> doPropagation(entity, propagationType));
      }
    } catch (FolioIntegrationException e) {
      log.warn("Skip propagation. Exception: ", e);
    }
  }

  protected abstract void doPropagation(T entity,
                                        PropagationType propagationType);

  public enum PropagationType {
    CREATE, UPDATE, DELETE
  }
}
