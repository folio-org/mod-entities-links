package org.folio.entlinks.service.authority;

import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.repository.AuthorityJdbcRepository;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.ConsortiumIllegalActionException;
import org.springframework.stereotype.Service;

@Service("consortiumAuthorityService")
public class ConsortiumAuthorityService extends AuthorityService {

  private final AuthorityRepository repository;

  public ConsortiumAuthorityService(AuthorityRepository repository,
                                    AuthorityJdbcRepository jdbcRepository,
                                    AuthoritySourceFileRepository sourceFileRepository) {
    super(repository, jdbcRepository, sourceFileRepository);
    this.repository = repository;
  }

  @Override
  protected AuthorityUpdateResult updateInner(Authority modified, boolean forced) {
    if (!forced) {
      validate(modified.getId(), "Update");
    }
    return super.updateInner(modified, forced);
  }

  @Override
  protected Authority deleteByIdInner(UUID id, boolean forced) {
    if (!forced) {
      validate(id, "Delete");
    }
    return super.deleteByIdInner(id, forced);
  }

  private void validate(UUID id, String actionName) {
    var authorityOptional = repository.findByIdAndDeletedFalse(id)
      .flatMap(authority -> authority.isConsortiumShadowCopy() ? Optional.empty() : Optional.of(authority));
    if (authorityOptional.isEmpty()) {
      throw new ConsortiumIllegalActionException(actionName);
    }
  }
}
