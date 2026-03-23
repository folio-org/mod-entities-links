package org.folio.entlinks.domain.repository;

import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthorityBaseCqlRepository {

  Page<Authority> findByCql(String cql, Pageable pageable);

  Page<UUID> findIdsByCql(String cqlQuery, Pageable pageable);

  Page<Authority> findDeletedByCql(String cql, Pageable pageable);

  Page<UUID> findDeletedIdsByCql(String cqlQuery, Pageable pageable);
}
