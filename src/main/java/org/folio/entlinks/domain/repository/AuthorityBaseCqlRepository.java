package org.folio.entlinks.domain.repository;

import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuthorityBaseCqlRepository<T extends AuthorityBase> {

  Page<T> findByCql(String cql, Pageable pageable);

  Page<UUID> findIdsByCql(String cqlQuery, Pageable pageable);
}
