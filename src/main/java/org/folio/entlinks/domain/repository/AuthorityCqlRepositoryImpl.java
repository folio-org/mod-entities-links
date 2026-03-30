package org.folio.entlinks.domain.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.spring.cql.Cql2JpaCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.support.PageableExecutionUtils;

public class AuthorityCqlRepositoryImpl implements AuthorityCqlRepository {

  private final EntityManager em;
  private final Cql2JpaCriteria<Authority> cql2JpaCriteria;

  public AuthorityCqlRepositoryImpl(EntityManager em) {
    this.em = em;
    this.cql2JpaCriteria = new Cql2JpaCriteria<>(Authority.class, em);
  }

  @Override
  public Page<Authority> findByCql(String cqlQuery, Pageable pageable) {
    return findByCqlInternal(cqlQuery, pageable, Boolean.FALSE);
  }

  @Override
  public Page<UUID> findIdsByCql(String cqlQuery, Pageable pageable) {
    return findIdsByCqlInternal(cqlQuery, pageable, Boolean.FALSE);
  }

  @Override
  public Page<Authority> findDeletedByCql(String cqlQuery, Pageable pageable) {
    return findByCqlInternal(cqlQuery, pageable, Boolean.TRUE);
  }

  @Override
  public Page<UUID> findDeletedIdsByCql(String cqlQuery, Pageable pageable) {
    return findIdsByCqlInternal(cqlQuery, pageable, Boolean.TRUE);
  }

  private Page<Authority> findByCqlInternal(String cqlQuery, Pageable pageable, Boolean deleted) {
    var collectBy = deletedIs(deleted).and(cql2JpaCriteria.createCollectSpecification(cqlQuery));
    var countBy = deletedIs(deleted).and(cql2JpaCriteria.createCountSpecification(cqlQuery));
    var criteria = cql2JpaCriteria.toCollectCriteria(collectBy);

    List<Authority> resultList = em
      .createQuery(criteria)
      .setFirstResult((int) pageable.getOffset())
      .setMaxResults(pageable.getPageSize())
      .getResultList();
    return PageableExecutionUtils.getPage(resultList, pageable, () -> count(countBy));
  }

  private Page<UUID> findIdsByCqlInternal(String cqlQuery, Pageable pageable, Boolean deleted) {
    var collectBy = deletedIs(deleted).and(cql2JpaCriteria.createCollectSpecification(cqlQuery));
    var countBy = deletedIs(deleted).and(cql2JpaCriteria.createCountSpecification(cqlQuery));

    var cb = em.getCriteriaBuilder();
    var query = cb.createQuery(UUID.class);
    var root = query.from(Authority.class);

    query.select(root.get(Authority.ID_COLUMN));
    query.where(collectBy.toPredicate(root, query, cb));

    List<UUID> resultList = em
      .createQuery(query)
      .setFirstResult((int) pageable.getOffset())
      .setMaxResults(pageable.getPageSize())
      .getResultList();
    return PageableExecutionUtils.getPage(resultList, pageable, () -> count(countBy));
  }

  private long count(Specification<Authority> specification) {
    var criteria = cql2JpaCriteria.toCountCriteria(specification);
    return em.createQuery(criteria).getSingleResult();
  }

  private Specification<Authority> deletedIs(Boolean deleted) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Authority.DELETED_COLUMN), deleted);
  }
}
