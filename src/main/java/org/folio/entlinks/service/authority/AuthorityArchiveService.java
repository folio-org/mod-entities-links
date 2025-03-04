package org.folio.entlinks.service.authority;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.repository.AuthorityArchiveRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Log4j2
public class AuthorityArchiveService implements AuthorityServiceI<AuthorityArchive> {

  private final AuthorityArchiveRepository repository;

  @Override
  public Page<AuthorityArchive> getAll(Integer offset, Integer limit, String cqlQuery) {
    log.debug("getAll:: Attempts to find all AuthorityArchive by [offset: {}, limit: {}, cql: {}]", offset, limit,
      cqlQuery);

    if (StringUtils.isBlank(cqlQuery)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(cqlQuery, new OffsetRequest(offset, limit));
  }

  @Override
  public Page<UUID> getAllIds(Integer offset, Integer limit, String cqlQuery) {
    log.debug("getAll:: Attempts to find all AuthorityArchive IDs by [offset: {}, limit: {}, cql: {}]",
      offset, limit, cqlQuery);

    if (StringUtils.isBlank(cqlQuery)) {
      return repository.findAllIds(new OffsetRequest(offset, limit));
    }

    return repository.findIdsByCql(cqlQuery, new OffsetRequest(offset, limit));
  }

  @Override
  public Map<UUID, AuthorityArchive> getAllByIds(Collection<UUID> ids) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AuthorityArchive getById(UUID id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AuthorityArchive create(AuthorityArchive entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AuthorityArchive create(AuthorityArchive entity, Consumer<AuthorityArchive> authorityCallback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AuthorityArchive update(AuthorityArchive modified) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Authority update(Authority modified, boolean forced) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AuthorityArchive update(AuthorityArchive modified,
                                 BiConsumer<AuthorityArchive, AuthorityArchive> authorityCallback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Authority update(Authority modified, boolean forced, BiConsumer<Authority, Authority> authorityConsumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<AuthorityArchive> upsert(List<AuthorityArchive> entities,
                                       Consumer<AuthorityArchive> authorityCreateCallback,
                                       BiConsumer<AuthorityArchive, AuthorityArchive> authorityUpdateCallback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteById(UUID id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteById(UUID id, boolean forced) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteById(UUID id, Consumer<AuthorityArchive> authorityCallback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteById(UUID id, boolean forced, Consumer<Authority> authorityCallback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteByIds(Collection<UUID> ids) {
    throw new UnsupportedOperationException();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void delete(AuthorityArchive authorityArchive) {
    log.debug("Deleting authority archive: id = {}", authorityArchive.getId());
    repository.delete(authorityArchive);
  }
}
