package org.folio.entlinks.service.authority;

import static org.folio.entlinks.domain.entity.AuthoritySourceFileSource.FOLIO;
import static org.folio.entlinks.domain.entity.AuthoritySourceFileSource.LOCAL;
import static org.folio.entlinks.utils.ServiceUtils.initId;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthoritySourceFileHridException;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.data.OffsetRequest;
import org.folio.tenant.domain.dto.Parameter;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Service("authoritySourceFileService")
@AllArgsConstructor
@Log4j2
public class AuthoritySourceFileService implements AuthoritySourceFileServiceI {

  private static final String AUTHORITY_SEQUENCE_NAME_TEMPLATE = "hrid_authority_local_file_%s_seq";
  private final AuthoritySourceFileRepository repository;
  private final AuthorityRepository authorityRepository;
  private final AuthoritySourceFileMapper mapper;
  private final JdbcTemplate jdbcTemplate;
  private final FolioModuleMetadata moduleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  @Override
  public Page<AuthoritySourceFile> getAll(Integer offset, Integer limit, String cql) {
    log.debug("getAll:: Attempts to find all AuthoritySourceFile by [offset: {}, limit: {}, cql: {}]", offset, limit,
      cql);

    if (StringUtils.isBlank(cql)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(cql, new OffsetRequest(offset, limit));
  }

  @Override
  public AuthoritySourceFile getById(UUID id) {
    log.debug("getById:: Loading AuthoritySourceFile by ID [id: {}]", id);

    return repository.findById(id).orElseThrow(() -> new AuthoritySourceFileNotFoundException(id));
  }

  @Override
  public AuthoritySourceFile findById(UUID id) {
    log.debug("findById:: Querying for AuthoritySourceFile by ID [id: {}]", id);

    if (id == null) {
      return null;
    }

    return repository.findById(id).orElse(null);
  }

  @Override
  public AuthoritySourceFile findByName(String name) {
    log.debug("findById:: Querying for AuthoritySourceFile by name [name: {}]", name);

    if (StringUtils.isBlank(name)) {
      return null;
    }

    return repository.findByName(name).orElse(null);
  }

  @Override
  @Transactional
  public AuthoritySourceFile create(AuthoritySourceFile entity) {
    log.debug("create:: Attempting to create AuthoritySourceFile [entity: {}]", entity);

    validateOnCreate(entity);

    initOnCreate(entity);

    return repository.save(entity);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(
    retryFor = OptimisticLockingException.class,
    maxAttempts = 2,
    backoff = @Backoff(delay = 500))
  public AuthoritySourceFile update(UUID id, AuthoritySourceFile modified) {
    return updateInner(id, modified, null);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(
    retryFor = OptimisticLockingException.class,
    maxAttempts = 2,
    backoff = @Backoff(delay = 500))
  public AuthoritySourceFile update(UUID id, AuthoritySourceFile modified,
                                    BiConsumer<AuthoritySourceFile, AuthoritySourceFile> publishConsumer) {
    return updateInner(id, modified, publishConsumer);
  }

  @Override
  public void deleteById(UUID id) {
    log.debug("deleteById:: Attempt to delete AuthoritySourceFile by [id: {}]", id);
    var authoritySourceFile = repository.findById(id)
      .orElseThrow(() -> new AuthoritySourceFileNotFoundException(id));
    if (!FOLIO.equals(authoritySourceFile.getSource())) {
      repository.deleteById(id);
    } else {
      throw new RequestBodyValidationException("Cannot delete Authority source file with source 'folio'",
        List.of(new Parameter("source").value(authoritySourceFile.getSource().name())));
    }
  }

  @Override
  public String nextHrid(UUID id) {
    log.debug("nextHrid:: Attempting to get next AuthoritySourceFile HRID [id: {}]", id);
    var sourceFile = getById(id);
    var sequenceName = sourceFile.getSequenceName();
    var codes = sourceFile.getAuthoritySourceFileCodes();
    if (StringUtils.isBlank(sequenceName) || codes.size() != 1) {
      throw new AuthoritySourceFileHridException(id);
    }
    try {
      long nextVal = repository.getNextSequenceNumber(sequenceName);
      return codes.iterator().next().getCode() + nextVal;
    } catch (DataAccessException e) {
      throw new AuthoritySourceFileHridException(id, e);
    }
  }

  @Override
  public boolean authoritiesExistForSourceFile(UUID sourceFileId) {
    return authorityRepository.existsAuthorityByAuthoritySourceFileId(sourceFileId);
  }

  @Override
  public boolean authoritiesExistForSourceFile(UUID sourceFileId, String tenantId, String tableName) {
    if (sourceFileId == null || tenantId == null) {
      return false;
    }

    var command = String.format("select exists (select true from %s.%s a where a.source_file_id='%s' limit 1)",
        moduleMetadata.getDBSchemaName(tenantId), tableName, sourceFileId);
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(command, Boolean.class));
  }

  private void validateOnCreate(AuthoritySourceFile entity) {
    entity.getAuthoritySourceFileCodes().forEach(this::validateSourceFileCode);
  }

  private AuthoritySourceFile updateInner(UUID id, AuthoritySourceFile modified,
                                          BiConsumer<AuthoritySourceFile, AuthoritySourceFile> publishConsumer) {
    log.debug("update:: Attempting to update AuthoritySourceFile [id: {}]", id);

    validateOnUpdate(id, modified);

    var existingEntity = repository.findById(id).orElseThrow(() -> new AuthoritySourceFileNotFoundException(id));
    final var detachedExisting = new AuthoritySourceFile(existingEntity);
    if (modified.getVersion() < existingEntity.getVersion()) {
      throw OptimisticLockingException.optimisticLockingOnUpdate(
        id, existingEntity.getVersion(), modified.getVersion());
    }

    updateSequenceStartNumber(existingEntity, modified);

    copyModifiableFields(existingEntity, modified);

    AuthoritySourceFile saved = repository.saveAndFlush(existingEntity);
    if (publishConsumer != null) {
      publishConsumer.accept(saved, detachedExisting);
    }
    return saved;
  }

  private void validateOnUpdate(UUID id, AuthoritySourceFile entity) {
    if (!Objects.equals(id, entity.getId())) {
      throw new RequestBodyValidationException("Request should have id = " + id,
          List.of(new Parameter("id").value(String.valueOf(entity.getId()))));
    }

    entity.getAuthoritySourceFileCodes().forEach(this::validateSourceFileCode);
  }

  private void validateSourceFileCode(AuthoritySourceFileCode sourceFileCode) {
    var code = sourceFileCode.getCode();
    if (StringUtils.isBlank(code) || !StringUtils.isAlpha(code)) {
      throw new RequestBodyValidationException("Authority Source File prefix should be non-empty sequence of letters",
          List.of(new Parameter("code").value(code)));
    }
  }

  private void initOnCreate(AuthoritySourceFile sourceFile) {
    initId(sourceFile);

    for (var code : sourceFile.getAuthoritySourceFileCodes()) {
      code.setAuthoritySourceFile(sourceFile);
    }

    if (sourceFile.getSource() == LOCAL) {
      initSourceFileSequenceFields(sourceFile);
    }
  }

  private void initSourceFileSequenceFields(AuthoritySourceFile sourceFile) {
    var sourceFileCode = sourceFile.getAuthoritySourceFileCodes().iterator().next();
    var sequenceName = String.format(AUTHORITY_SEQUENCE_NAME_TEMPLATE, sourceFileCode.getCode());
    sourceFile.setSequenceName(sequenceName);

    if (sourceFile.getHridStartNumber() == null) {
      sourceFile.setHridStartNumber(1);
    }
  }

  private void copyModifiableFields(AuthoritySourceFile existingEntity, AuthoritySourceFile modifiedEntity) {
    existingEntity.setName(modifiedEntity.getName());
    existingEntity.setBaseUrlProtocol(modifiedEntity.getBaseUrlProtocol());
    existingEntity.setBaseUrl(modifiedEntity.getBaseUrl());
    existingEntity.setSelectable(modifiedEntity.isSelectable());
    existingEntity.setType(modifiedEntity.getType());
    existingEntity.setHridStartNumber(modifiedEntity.getHridStartNumber());
    existingEntity.setVersion(existingEntity.getVersion() + 1);
    var existingCodes = mapper.toDtoCodes(existingEntity.getAuthoritySourceFileCodes());
    var modifiedCodes = mapper.toDtoCodes(modifiedEntity.getAuthoritySourceFileCodes());
    for (var code : modifiedEntity.getAuthoritySourceFileCodes()) {
      if (!existingCodes.contains(code.getCode())) {
        existingEntity.addCode(code);
      }
    }
    var iterator = existingEntity.getAuthoritySourceFileCodes().iterator();
    while (iterator.hasNext()) {
      var sourceFileCode = iterator.next();
      if (!modifiedCodes.contains(sourceFileCode.getCode())) {
        sourceFileCode.setAuthoritySourceFile(null);
        iterator.remove();
      }
    }
  }

  @Override
  public void createSequence(String sequenceName, int startNumber) {
    var command = String.format("""
            CREATE SEQUENCE %s MINVALUE %d INCREMENT BY 1 OWNED BY %s.authority_source_file.sequence_name;
            """,
        sequenceName, startNumber, moduleMetadata.getDBSchemaName(folioExecutionContext.getTenantId()));
    jdbcTemplate.execute(command);
  }

  @Override
  public void deleteSequence(String sequenceName) {
    var command = String.format("DROP SEQUENCE IF EXISTS %s.%s;",
        moduleMetadata.getDBSchemaName(folioExecutionContext.getTenantId()), sequenceName);
    jdbcTemplate.execute(command);
  }

  protected void updateSequenceStartNumber(AuthoritySourceFile existing, AuthoritySourceFile modified) {
    if (!Objects.equals(existing.getHridStartNumber(), modified.getHridStartNumber())
        && existing.getHridStartNumber() != null && modified.getHridStartNumber() != null) {
      var sequenceName = existing.getSequenceName();
      var modifiedStartNumber = (int) modified.getHridStartNumber();
      deleteSequence(sequenceName);
      createSequence(sequenceName, modifiedStartNumber);
    }
  }
}
