package org.folio.entlinks.domain.repository;

import java.util.UUID;
import java.util.stream.Stream;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityRepository extends JpaCqlRepository<Authority, UUID> {

  Stream<Authority> streamAll();
}
