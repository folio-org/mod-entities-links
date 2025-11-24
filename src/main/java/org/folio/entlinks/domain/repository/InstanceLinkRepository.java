package org.folio.entlinks.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.entity.projection.LinkCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstanceLinkRepository extends JpaRepository<InstanceAuthorityLink, Long>,
  JpaSpecificationExecutor<InstanceAuthorityLink> {

  @Query("""
          select l, auth.naturalId
          from InstanceAuthorityLink l
          left join Authority auth on l.authorityId = auth.id
          where l.instanceId = :instanceId
      """)
  List<Object[]> findByInstanceId(@Param("instanceId") UUID instanceId);

  @Query("select l from InstanceAuthorityLink l where l.authorityId = :id order by l.id")
  Page<InstanceAuthorityLink> findByAuthorityId(@Param("id") UUID id, Pageable pageable);

  @Query("select l.authorityId as id, count(distinct l.instanceId) as totalLinks"
    + " from InstanceAuthorityLink l where l.authorityId in :authorityIds"
    + " group by id")
  List<LinkCountView> countLinksByAuthorityIds(@Param("authorityIds") Set<UUID> authorityIds);

  @Modifying
  @Query("""
    update InstanceAuthorityLink i set i.status = :status, i.errorCause = :errorCause
    where i.authorityId = :authorityId""")
  void updateStatusAndErrorCauseByAuthorityId(@Param("status") InstanceAuthorityLinkStatus status,
                                              @Param("errorCause") String errorCause,
                                              @Param("authorityId") UUID authorityId);

  @Modifying
  @Query("delete from InstanceAuthorityLink i where i.authorityId in :authorityIds")
  void deleteByAuthorityIds(@Param("authorityIds") Collection<UUID> authorityIds);
}
