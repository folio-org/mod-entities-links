package org.folio.entlinks.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityIdentifierType;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityIdentifierTypeRepository extends JpaCqlRepository<AuthorityIdentifierType, UUID> {

  Optional<AuthorityIdentifierType> findByName(String name);
}
