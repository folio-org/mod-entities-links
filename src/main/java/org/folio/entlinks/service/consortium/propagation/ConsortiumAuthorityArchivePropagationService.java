package org.folio.entlinks.service.consortium.propagation;

import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.service.authority.AuthorityArchiveService;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class ConsortiumAuthorityArchivePropagationService extends ConsortiumPropagationService<AuthorityArchive> {

  private static final String ILLEGAL_PROPAGATION_MSG =
      "Propagation type '%s' is not supported for authority archives.";

  private final AuthorityArchiveService authorityArchiveService;

  public ConsortiumAuthorityArchivePropagationService(AuthorityArchiveService authorityArchiveService,
                                                      ConsortiumTenantsService tenantsService,
                                                      SystemUserScopedExecutionService executionService) {
    super(tenantsService, executionService);
    this.authorityArchiveService = authorityArchiveService;
  }

  protected void doPropagation(AuthorityArchive archive, PropagationType propagationType) {
    archive.makeAsConsortiumShadowCopy();
    switch (propagationType) {
      case DELETE -> authorityArchiveService.delete(archive);
      case CREATE, UPDATE -> throw new IllegalArgumentException(ILLEGAL_PROPAGATION_MSG.formatted(propagationType));
      default -> throw new IllegalStateException("Unexpected value: " + propagationType);
    }
  }
}
