package org.folio.entlinks.service.authority;

import static org.folio.entlinks.utils.ServiceUtils.initId;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.repository.AuthorityJdbcRepository;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.entlinks.service.consortium.UserTenantsService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Primary
@Service("authorityService")
@RequiredArgsConstructor
public class AuthorityService implements AuthorityServiceI<Authority> {

  private final AuthorityRepository repository;
  private final AuthorityJdbcRepository jdbcRepository;
  private final AuthoritySourceFileRepository sourceFileRepository;
  private final UserTenantsService userTenantsService;
  private final FolioExecutionContext folioExecutionContext;

  @Override
  public Page<Authority> getAll(Integer offset, Integer limit, String cql) {
    log.debug("getAll:: Attempts to find all Authority by [offset: {}, limit: {}, cql: {}]", offset, limit,
      cql);

    if (StringUtils.isBlank(cql)) {
      return repository.findAllByDeletedFalse(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(cql, new OffsetRequest(offset, limit));
  }

  @Override
  public Page<UUID> getAllIds(Integer offset, Integer limit, String cql) {
    log.debug("getAll:: Attempts to find all Authority IDs by [offset: {}, limit: {}, cql: {}]",
      offset, limit, cql);
    if (StringUtils.isBlank(cql)) {
      return repository.findAllIdsByDeletedFalse(new OffsetRequest(offset, limit));
    }

    return repository.findIdsByCql(cql, new OffsetRequest(offset, limit));
  }

  @Override
  public Map<UUID, Authority> getAllByIds(Collection<UUID> ids) {
    return repository.findAllByIdInAndDeletedFalse(ids).stream()
      .collect(Collectors.toMap(Authority::getId, Function.identity()));
  }

  @Override
  public Authority getById(UUID id) {
    log.debug("getById:: Loading Authority by ID [id: {}]", id);

    return repository.findByIdAndDeletedFalse(id).orElseThrow(() -> new AuthorityNotFoundException(id));
  }

  @Override
  public Authority create(Authority entity) {
    return createInner(entity);
  }

  @Override
  @Transactional
  public AuthorityUpdateResult update(Authority modified, boolean forced) {
    return updateInner(modified, forced);
  }

  @Override
  @Transactional
  public List<AuthorityUpdateResult> upsert(List<Authority> authorities) {
    var existingRecordsMap = getAllByIds(authorities.stream().map(Authority::getId).toList());
    final var detachedExistingRecordsMap = Maps.transformEntries(existingRecordsMap,
      (key, value) -> new Authority(value));
    var modifiedRecords = authorities.stream()
      .filter(authority -> existingRecordsMap.containsKey(authority.getId()))
      .toList();
    for (Authority modified : modifiedRecords) {
      var id = modified.getId();
      var existing = existingRecordsMap.get(id);
      olCheck(modified, existing, id);
      copyModifiableFields(existing, modified);
    }
    var newRecords = authorities.stream()
      .filter(authority -> !existingRecordsMap.containsKey(authority.getId()))
      .toList();

    var authoritiesToSave = new ArrayList<>(newRecords);
    authoritiesToSave.addAll(existingRecordsMap.values());
    return repository.saveAll(authoritiesToSave).stream()
      .map(authority -> new AuthorityUpdateResult(detachedExistingRecordsMap.get(authority.getId()), authority))
      .toList();
  }

  @Override
  @Transactional
  public void deleteById(UUID id, boolean forced) {
    deleteByIdInner(id, forced);
  }

  @Override
  @Transactional
  public Authority deleteById(UUID id) {
    return deleteByIdInner(id, false);
  }

  /**
   * Performs hard-delete of {@link Authority} records.
   *
   * @param ids collection of authority record ids of {@link UUID} type
   */
  @Override
  @Transactional
  public void deleteByIds(Collection<UUID> ids) {
    repository.deleteAllByIdInBatch(ids);
  }

  /**
   * Check authorities existence by ids.
   *
   * @param authorityIds list of authority ids to check existence for.
   * @return map of authority ids to boolean values indicating existence.
   * */
  public Map<UUID, Boolean> authoritiesExist(Set<UUID> authorityIds) {
    var existingIds = repository.findExistingIdsByIdsAndDeletedFalse(authorityIds);
    return contructExistenceMap(authorityIds, existingIds);
  }

  /**
   * Check authorities existence by ids.
   *
   * @param authorityIds list of authority ids to check existence for.
   * @return map of authority ids to boolean values indicating existence.
   * */
  public Map<UUID, Boolean> authoritiesExistForCentralIfOnMember(Set<UUID> authorityIds) {
    var tenant = folioExecutionContext.getTenantId();
    var centralTenant = userTenantsService.getCentralTenant(tenant);
    if (centralTenant.isEmpty() || centralTenant.get().equals(tenant)) {
      return Collections.emptyMap();
    }
    var existingIds = jdbcRepository.findExistingIdsByIdsAndDeletedFalse(authorityIds, centralTenant.get());
    return contructExistenceMap(authorityIds, existingIds);
  }

  public Map<UUID, String> findNaturalIdsByIdInAndDeletedFalseForCentralIfOnMember(Collection<UUID> ids) {
    var tenant = folioExecutionContext.getTenantId();
    var centralTenant = userTenantsService.getCentralTenant(tenant);
    if (centralTenant.isEmpty() || centralTenant.get().equals(tenant)) {
      return Collections.emptyMap();
    }
    return jdbcRepository.findAuthorityNaturalIdsByIdsAndDeletedFalse(ids, centralTenant.get());
  }

  protected Authority createInner(Authority entity) {
    log.debug("create:: Attempting to create Authority [entity: {}]", entity);
    initId(entity);

    return repository.save(entity);
  }

  protected AuthorityUpdateResult updateInner(Authority modified, boolean forced) {
    log.debug("update:: Attempting to update Authority [authority: {}]", modified);

    var existing = validateOnUpdateAndGetExisting(modified.getId(), modified);
    var detachedExisting = new Authority(existing);

    copyModifiableFields(existing, modified);

    var saved = repository.saveAndFlush(existing);
    return new AuthorityUpdateResult(detachedExisting, saved);
  }

  protected Authority deleteByIdInner(UUID id, boolean forced) {
    log.debug("deleteById:: Attempt to delete Authority by [id: {}]", id);

    var existed = repository.findByIdAndDeletedFalse(id)
      .orElseThrow(() -> new AuthorityNotFoundException(id));
    existed.setDeleted(true);

    return repository.save(existed);
  }

  private Map<UUID, Boolean> contructExistenceMap(Set<UUID> ids, List<UUID> existingIds) {
    var existingIdsSet = new HashSet<>(existingIds);

    return ids.stream()
      .collect(Collectors.toMap(
        id -> id,
        existingIdsSet::contains
      ));
  }

  private Authority validateOnUpdateAndGetExisting(UUID id, Authority modified) {
    var existing = repository.findByIdAndDeletedFalse(id).orElseThrow(() -> new AuthorityNotFoundException(id));

    var sourceFileId = Optional.ofNullable(modified.getAuthoritySourceFile())
        .map(AuthoritySourceFile::getId)
        .orElse(null);
    if (sourceFileId != null && !sourceFileRepository.existsById(sourceFileId)) {
      throw new AuthoritySourceFileNotFoundException(sourceFileId);
    }

    olCheck(modified, existing, id);
    return existing;
  }

  private static void olCheck(Authority modified, Authority existing, UUID id) {
    if (modified.getVersion() < existing.getVersion()) {
      throw OptimisticLockingException.optimisticLockingOnUpdate(id, existing.getVersion(), modified.getVersion());
    }
  }

  private void copyModifiableFields(Authority existing, Authority modified) {
    existing.setHeading(modified.getHeading());
    existing.setHeadingType(modified.getHeadingType());
    existing.setSource(modified.getSource());
    existing.setNaturalId(modified.getNaturalId());
    existing.setSubjectHeadingCode(modified.getSubjectHeadingCode());
    existing.setSftHeadings(modified.getSftHeadings());
    existing.setSaftHeadings(modified.getSaftHeadings());
    existing.setIdentifiers(modified.getIdentifiers());
    existing.setNotes(modified.getNotes());
    existing.setVersion(existing.getVersion() + 1);

    Optional.ofNullable(modified.getAuthoritySourceFile())
      .map(AuthoritySourceFile::getId)
      .ifPresentOrElse(sourceFileId -> {
        var sourceFile = new AuthoritySourceFile();
        sourceFile.setId(sourceFileId);
        existing.setAuthoritySourceFile(sourceFile);
      }, () -> existing.setAuthoritySourceFile(null));
  }
}
