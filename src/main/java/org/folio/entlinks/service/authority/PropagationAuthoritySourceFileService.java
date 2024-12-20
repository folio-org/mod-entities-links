package org.folio.entlinks.service.authority;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.entlinks.utils.JdbcUtils.getFullPath;

import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.entlinks.utils.JdbcUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service("propagationAuthoritySourceFileService")
@Log4j2
public class PropagationAuthoritySourceFileService extends AuthoritySourceFileService {

  private final JdbcTemplate jdbcTemplate;
  private final AuthoritySourceFileRepository repository;
  private final FolioExecutionContext folioExecutionContext;

  public PropagationAuthoritySourceFileService(AuthoritySourceFileRepository repository,
                                               AuthorityRepository authorityRepository,
                                               AuthoritySourceFileMapper mapper,
                                               JdbcTemplate jdbcTemplate,
                                               FolioModuleMetadata moduleMetadata,
                                               FolioExecutionContext folioExecutionContext) {
    super(repository, authorityRepository, mapper, jdbcTemplate, moduleMetadata, folioExecutionContext);
    this.jdbcTemplate = jdbcTemplate;
    this.repository = repository;
    this.folioExecutionContext = folioExecutionContext;
  }

  /**
   * Creates shadow copy of Authority Source File in the member tenant's database schema.
   *
   * @param entity Authority Source File created in the consortium central tenant
   * @return Authority Source File created in the member tenant.
   *
   *   Note: As this method creates a shadow copy of already created authority source file, it assumes no
   *   validation/initialization is required and the entity passed to the method had this validation/initialization.
   */
  @Override
  @Transactional
  public AuthoritySourceFile create(AuthoritySourceFile entity) {
    log.debug("create:: Attempting to create AuthoritySourceFile [entity: {}]", entity);

    var sourceType = getFullPath(folioExecutionContext, "authority_source_file_source");
    var sqlValues = JdbcUtils.getParamPlaceholder(3)
        + "::" + sourceType + ","
        + JdbcUtils.getParamPlaceholder(9);
    var sql = """
                INSERT INTO %s (id, name, source, type, base_url_protocol, base_url, hrid_start_number, _version,
                created_date, updated_date, created_by_user_id, updated_by_user_id)
                VALUES (%s);
        """;
    jdbcTemplate.update(sql.formatted(getFullPath(folioExecutionContext, "authority_source_file"), sqlValues),
        entity.getId(), entity.getName(),
        entity.getSource().name(), entity.getType(), entity.getBaseUrlProtocol(), entity.getBaseUrl(),
        entity.getHridStartNumber(), 0, entity.getCreatedDate(), entity.getUpdatedDate(), entity.getCreatedByUserId(),
        entity.getUpdatedByUserId());

    return entity;
  }

  /**
   * Updates shadow copy of Authority Source File in the member tenant's database schema.
   *
   * @param id the ID of the Authority Source File to update
   * @param modified Authority Source File updated by central tenant
   * @param publishConsumer the consumer to publish changes
   * @return updated Authority Source File
   *
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

    updateSequenceStartNumber(existingEntity, modified);

    var sourceType = getFullPath(folioExecutionContext, "authority_source_file_source");
    var sql = """
                UPDATE %s
                SET name=?, source=?::%s, type=?, base_url_protocol=?, base_url=?, hrid_start_number=?,
                created_date=?, updated_date=?, created_by_user_id=?, updated_by_user_id=?, _version=?
                WHERE id = ? and _version = ?;
        """;

    jdbcTemplate.update(sql.formatted(getFullPath(folioExecutionContext, "authority_source_file"), sourceType),
        modified.getName(), modified.getSource().name(), modified.getType(),
        modified.getBaseUrlProtocol(), modified.getBaseUrl(), modified.getHridStartNumber(),
        modified.getCreatedDate(), modified.getUpdatedDate(), modified.getCreatedByUserId(),
        modified.getUpdatedByUserId(), modified.getVersion(), id, existingEntity.getVersion());

    if (isNotEmpty(modified.getAuthoritySourceFileCodes())) {
      var sourceFileCode = modified.getAuthoritySourceFileCodes().iterator().next();
      sql = "INSERT INTO %s (authority_source_file_id, code) VALUES (?, ?);";
      jdbcTemplate.update(sql.formatted(getFullPath(folioExecutionContext, "authority_source_file_code")),
          sourceFileCode.getAuthoritySourceFile().getId(), sourceFileCode.getCode());
    }

    if (publishConsumer != null) {
      publishConsumer.accept(modified, existingEntity);
    }

    return modified;
  }
}
