package org.folio.entlinks.domain.repository;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityArchiveRepository extends JpaRepository<AuthorityArchive, UUID>,
  AuthorityArchiveCqlRepository {

  @Query("select aa from AuthorityArchive aa where aa.updatedDate <= :tillDate and aa.source not like :sourcePrefix%")
  @QueryHints(@QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "25"))
  Stream<AuthorityArchive> streamByUpdatedTillDateAndSourcePrefix(
      @Param("tillDate") LocalDateTime tillDate, @Param("sourcePrefix") String sourcePrefix);

  @Query("select a.id as id from AuthorityArchive a")
  Page<UUID> findAllIds(Pageable pageable);
}
