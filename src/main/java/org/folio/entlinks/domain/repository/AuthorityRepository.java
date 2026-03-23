package org.folio.entlinks.domain.repository;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.entlinks.domain.entity.Authority;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, UUID>, AuthorityCqlRepository {

  List<Authority> findByNaturalIdInAndDeletedFalse(Collection<String> naturalIds);

  Page<Authority> findAllByDeletedFalse(Pageable pageable);

  @Query("select a.id as id from Authority a where a.deleted = false")
  Page<UUID> findAllIdsByDeletedFalse(Pageable pageable);

  Optional<Authority> findByIdAndDeletedFalse(UUID id);

  List<Authority> findAllByIdInAndDeletedFalse(Collection<UUID> ids);

  boolean existsAuthorityByAuthoritySourceFileId(UUID sourceFileId);

  @Query("select a.id from Authority a where a.id in :ids and a.deleted = false")
  List<UUID> findExistingIdsByIdsAndDeletedFalse(Collection<UUID> ids);

  Page<Authority> findAllByDeletedTrue(Pageable pageable);

  @Query("select a.id as id from Authority a where a.deleted = true")
  Page<UUID> findAllIdsByDeletedTrue(Pageable pageable);

  @Query("select a from Authority a where a.deleted = true and a.updatedDate <= :tillDate")
  @QueryHints(@QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "25"))
  Stream<Authority> streamByDeletedTrueAndUpdatedDateLessThanEqual(@Param("tillDate") LocalDateTime tillDate);
}
