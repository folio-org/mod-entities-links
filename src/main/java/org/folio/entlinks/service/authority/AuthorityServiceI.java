package org.folio.entlinks.service.authority;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.springframework.data.domain.Page;

public interface AuthorityServiceI<T> {

  Page<T> getAll(Integer offset, Integer limit, String cql);

  Page<T> getAllDeleted(Integer offset, Integer limit, String cql);

  Page<UUID> getAllIds(Integer offset, Integer limit, String cql);

  Page<UUID> getAllDeletedIds(Integer offset, Integer limit, String cql);

  Map<UUID, T> getAllByIds(Collection<UUID> ids);

  T getById(UUID id);

  T create(T entity);

  AuthorityUpdateResult update(Authority modified, boolean forced);

  List<AuthorityUpdateResult> upsert(List<T> entities);

  void deleteById(UUID id, boolean forced);

  T deleteById(UUID id);

  void deleteByIds(Collection<UUID> ids);
}
