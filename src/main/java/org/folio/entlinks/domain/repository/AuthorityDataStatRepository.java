package org.folio.entlinks.domain.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

public interface AuthorityDataStatRepository extends JpaRepository<AuthorityDataStat, UUID> {

  @Query("""
    select a from AuthorityDataStat a
    where a.action = :action and a.startedAt between :startedAtStart and :startedAtEnd
    limit :limit
    order by a.startedAt DESC""")
  List<AuthorityDataStat> findByDateAndAction(@Param("action") AuthorityDataStatAction action,
                                              @Param("startedAtStart") @Nullable Timestamp startedAtStart,
                                              @Param("startedAtEnd") @Nullable Timestamp startedAtEnd,
                                              @Param("limit") int limit);
}