package org.folio.entlinks.service.authority;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.exception.ConsortiumIllegalActionException;
import org.springframework.stereotype.Service;

@Service("consortiumAuthorityService")
public class ConsortiumAuthorityService extends AuthorityService {

  private final AuthorityRepository repository;

  public ConsortiumAuthorityService(AuthorityRepository repository) {
    super(repository);
    this.repository = repository;
  }

  @Override
  protected Authority updateInner(Authority modified, boolean forced,
                                  BiConsumer<Authority, Authority> authorityConsumer) {
    if (!forced) {
      validate(modified.getId(), "Update");
    }
    return super.updateInner(modified, forced, authorityConsumer);
  }

  @Override
  protected void deleteByIdInner(UUID id, boolean forced, Consumer<Authority> authorityCallback) {
    if (!forced) {
      validate(id, "Delete");
    }
    super.deleteByIdInner(id, forced, authorityCallback);
  }

  private void validate(UUID id, String actionName) {
    var authorityOptional = repository.findByIdAndDeletedFalse(id)
      .flatMap(authority -> authority.isConsortiumShadowCopy() ? Optional.empty() : Optional.of(authority));
    if (authorityOptional.isEmpty()) {
      throw new ConsortiumIllegalActionException(actionName);
    }
  }
}
