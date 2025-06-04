package org.folio.entlinks.service.consortium.propagation;

import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class ConsortiumAuthorityPropagationService extends ConsortiumPropagationService<Authority> {

  private final AuthorityService authorityService;

  public ConsortiumAuthorityPropagationService(@Qualifier("consortiumAuthorityService") AuthorityService service,
                                               ConsortiumTenantsService tenantsService,
                                               SystemUserScopedExecutionService executionService) {
    super(tenantsService, executionService);
    this.authorityService = service;
  }

  protected void doPropagation(Authority authority, PropagationType propagationType) {
    authority.makeAsConsortiumShadowCopy();
    switch (propagationType) {
      case CREATE -> authorityService.create(authority);
      case UPDATE -> authorityService.update(authority, true);
      case DELETE -> authorityService.deleteById(authority.getId(), true);
      default -> throw new IllegalStateException("Unexpected value: " + propagationType);
    }
  }
}
