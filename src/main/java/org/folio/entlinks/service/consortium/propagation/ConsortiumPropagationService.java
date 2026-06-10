package org.folio.entlinks.service.consortium.propagation;

import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextService;
import org.springframework.scheduling.annotation.Async;

@Log4j2
public abstract class ConsortiumPropagationService<T> {

  private final ConsortiumTenantsService tenantsService;
  private final FolioExecutionContextService executionService;
  private final FolioExecutionContext folioExecutionContext;

  protected ConsortiumPropagationService(ConsortiumTenantsService tenantsService,
                                         FolioExecutionContextService executionService,
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
      var consortiumTenants = tenantsService.getConsortiumTenants(tenantId);
      log.debug("Find consortium tenants for propagation: {}, context: {}", consortiumTenants, tenantId);
      for (var consortiumTenant : consortiumTenants) {
        executionService.execute(consortiumTenant, folioExecutionContext, () -> doPropagation(entity, propagationType));
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
