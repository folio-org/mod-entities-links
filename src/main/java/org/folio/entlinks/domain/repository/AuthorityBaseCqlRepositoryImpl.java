package org.folio.entlinks.domain.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityBase;
import org.folio.spring.cql.Cql2JpaCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.support.PageableExecutionUtils;

public abstract class AuthorityBaseCqlRepositoryImpl<T extends AuthorityBase> implements AuthorityBaseCqlRepository<T> {

  private final EntityManager em;
  private final Cql2JpaCriteria<T> cql2JpaCriteria;

  protected AuthorityBaseCqlRepositoryImpl(EntityManager em) {
    this.em = em;
    this.cql2JpaCriteria = new Cql2JpaCriteria<>(getClassType(), em);
  }

  @Override
  public Page<T> findByCql(String cqlQuery, Pageable pageable) {

    var collectBy = collectByQuery(cqlQuery);
    var countBy = countByQuery(cqlQuery);
    var criteria = cql2JpaCriteria.toCollectCriteria(collectBy);

    List<T> resultList = em
      .createQuery(criteria)
      .setFirstResult((int) pageable.getOffset())
      .setMaxResults(pageable.getPageSize())
      .getResultList();
    return PageableExecutionUtils.getPage(resultList, pageable, () -> count(countBy));
  }

  @Override
  public Page<UUID> findIdsByCql(String cqlQuery, Pageable pageable) {
    var collectBy = collectByQuery(cqlQuery);
    var countBy = countByQuery(cqlQuery);

    var cb = em.getCriteriaBuilder();
    var query = cb.createQuery(UUID.class);
    var root = query.from(getClassType());

    query.select(root.get(AuthorityBase.ID_COLUMN));
    query.where(collectBy.toPredicate(root, query, cb));

    List<UUID> resultList = em
      .createQuery(query)
      .setFirstResult((int) pageable.getOffset())
      .setMaxResults(pageable.getPageSize())
      .getResultList();
    return PageableExecutionUtils.getPage(resultList, pageable, () -> count(countBy));
  }

  protected abstract Class<T> getClassType();

  protected abstract Boolean deleted();

  private long count(Specification<T> specification) {
    var criteria = cql2JpaCriteria.toCountCriteria(specification);
    return em.createQuery(criteria).getSingleResult();
  }

  private Specification<T> collectByQuery(String cqlQuery) {
    return deletedIs(deleted()).and(cql2JpaCriteria.createCollectSpecification(cqlQuery));
  }

  private Specification<T> countByQuery(String cqlQuery) {
    return deletedIs(deleted()).and(cql2JpaCriteria.createCountSpecification(cqlQuery));
  }

  private Specification<T> deletedIs(Boolean deleted) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(AuthorityBase.DELETED_COLUMN), deleted);
  }
}
