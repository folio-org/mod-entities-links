package org.folio.entlinks.domain.repository;

import jakarta.persistence.EntityManager;

public class AuthorityCqlRepositoryImpl extends AuthorityBaseCqlRepositoryImpl
  implements AuthorityCqlRepository {

  public AuthorityCqlRepositoryImpl(EntityManager em) {
    super(em);
  }
}
