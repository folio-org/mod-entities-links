package org.folio.entlinks.service.authority;

import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileJdbcRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.spring.FolioExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("propagationAuthoritySourceFileService")
@Log4j2
public class PropagationAuthoritySourceFileService extends AuthoritySourceFileService {

  private final AuthoritySourceFileJdbcRepository jdbcRepository;
  private final AuthoritySourceFileRepository repository;

  public PropagationAuthoritySourceFileService(AuthoritySourceFileRepository repository,
                                               AuthoritySourceFileJdbcRepository jdbcRepository,
                                               AuthorityRepository authorityRepository,
                                               AuthoritySourceFileMapper mapper,
                                               JdbcTemplate jdbcTemplate,
                                               FolioExecutionContext folioExecutionContext) {
    super(repository, jdbcRepository, authorityRepository, mapper, jdbcTemplate, folioExecutionContext);
    this.jdbcRepository = jdbcRepository;
    this.repository = repository;
  }

  /**
   * Creates shadow copy of Authority Source File in the member tenant's database schema.
   *
   * @param entity Authority Source File created in the consortium central tenant
   * @return Authority Source File created in the member tenant.
   *   Note: As this method creates a shadow copy of already created authority source file, it assumes no
   *   validation/initialization is required and the entity passed to the method had this validation/initialization.
   */
  @Override
  @Transactional
  public AuthoritySourceFile create(AuthoritySourceFile entity) {
    log.debug("create:: Attempting to create AuthoritySourceFile [entity: {}]", entity);

    jdbcRepository.insert(entity);

    return entity;
  }

  /**
   * Updates shadow copy of Authority Source File in the member tenant's database schema.
   *
   * @param id              the ID of the Authority Source File to update
   * @param modified        Authority Source File updated by central tenant
   * @param publishConsumer the consumer to publish changes
   * @return updated Authority Source File
   *   Note: As this method updates existing shadow copy of already updated authority source file, it assumes no
   *   validation is required and updated entity passed to the method is passed this validation.
   */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(
    retryFor = OptimisticLockingException.class,
    maxAttempts = 2,
    backoff = @Backoff(delay = 500))
  public AuthoritySourceFile update(UUID id, AuthoritySourceFile modified,
                                    BiConsumer<AuthoritySourceFile, AuthoritySourceFile> publishConsumer) {
    log.debug("update:: Attempting to update AuthoritySourceFile [id: {}, modified: {}]", id, modified);

    var existingEntity = repository.findById(id).orElseThrow(() -> new AuthoritySourceFileNotFoundException(id));
    if (modified.getVersion() < existingEntity.getVersion()) {
      throw OptimisticLockingException.optimisticLockingOnUpdate(
        id, existingEntity.getVersion(), modified.getVersion());
    }

    jdbcRepository.update(modified, existingEntity.getVersion());

    if (publishConsumer != null) {
      publishConsumer.accept(modified, existingEntity);
    }

    return modified;
  }

  @Override
  public void deleteById(UUID id) {
    log.debug("deleteById:: Attempt to delete AuthoritySourceFile by [id: {}]", id);
    validateOnDelete(id);

    jdbcRepository.delete(id);
  }
}
