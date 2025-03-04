package org.folio.entlinks.service.authority;

import static org.folio.entlinks.utils.ServiceUtils.initId;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
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
    return createInner(entity, null);
  }

  @Override
  public Authority create(Authority entity, Consumer<Authority> authorityCallback) {
    return createInner(entity, authorityCallback);
  }

  @Override
  @Transactional
  public Authority update(Authority modified) {
    return updateInner(modified, false, null);
  }

  @Override
  @Transactional
  public Authority update(Authority modified, boolean forced) {
    return updateInner(modified, forced, null);
  }

  @Override
  @Transactional
  public Authority update(Authority modified, BiConsumer<Authority, Authority> authorityConsumer) {
    return updateInner(modified, false, authorityConsumer);
  }

  @Override
  @Transactional
  public Authority update(Authority modified, boolean forced,
                          BiConsumer<Authority, Authority> authorityConsumer) {
    return updateInner(modified, forced, authorityConsumer);
  }

  @Override
  @Transactional
  public List<Authority> upsert(List<Authority> authorities, Consumer<Authority> authorityCreateCallback,
                                BiConsumer<Authority, Authority> authorityUpdateCallback) {
    var existingRecordsMap = getAllByIds(authorities.stream().map(Authority::getId).toList());
    final var detachedExistingRecordsMap =
        Maps.transformEntries(existingRecordsMap, (key, value) -> new Authority(value));
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
    var result = repository.saveAll(authoritiesToSave);

    if (authorityCreateCallback != null) {
      newRecords.forEach(authorityCreateCallback);
    }

    if (authorityUpdateCallback != null) {
      for (var modifiedRecord : modifiedRecords) {
        authorityUpdateCallback.accept(modifiedRecord, detachedExistingRecordsMap.get(modifiedRecord.getId()));
      }
    }

    return result;
  }

  /**
   * Performs soft-delete of {@link Authority} records.
   *
   * @param id authority record id of {@link UUID} type
   */
  @Override
  @Transactional
  public void deleteById(UUID id) {
    deleteByIdInner(id, false, null);
  }

  @Override
  @Transactional
  public void deleteById(UUID id, boolean forced) {
    deleteByIdInner(id, forced, null);
  }

  @Override
  @Transactional
  public void deleteById(UUID id, Consumer<Authority> authorityCallback) {
    deleteByIdInner(id, false, authorityCallback);
  }

  @Override
  @Transactional
  public void deleteById(UUID id, boolean forced, Consumer<Authority> authorityCallback) {
    deleteByIdInner(id, forced, authorityCallback);
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

  protected Authority createInner(Authority entity, Consumer<Authority> authorityCallback) {
    log.debug("create:: Attempting to create Authority [entity: {}]", entity);
    initId(entity);
    var saved = repository.save(entity);

    if (authorityCallback != null) {
      authorityCallback.accept(saved);
    }
    return saved;
  }

  protected Authority updateInner(Authority modified, boolean forced,
                                  BiConsumer<Authority, Authority> authorityConsumer) {
    log.debug("update:: Attempting to update Authority [authority: {}]", modified);
    var id = modified.getId();
    var existing = repository.findByIdAndDeletedFalse(id).orElseThrow(() -> new AuthorityNotFoundException(id));
    var detachedExisting = new Authority(existing);
    olCheck(modified, existing, id);

    copyModifiableFields(existing, modified);

    var saved = repository.saveAndFlush(existing);
    if (authorityConsumer != null) {
      authorityConsumer.accept(saved, detachedExisting);
    }
    return saved;
  }

  protected void deleteByIdInner(UUID id, boolean forced, Consumer<Authority> authorityCallback) {
    log.debug("deleteById:: Attempt to delete Authority by [id: {}]", id);

    var existed = repository.findByIdAndDeletedFalse(id)
      .orElseThrow(() -> new AuthorityNotFoundException(id));
    existed.setDeleted(true);

    if (authorityCallback != null) {
      authorityCallback.accept(existed);
    }

    repository.save(existed);
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
