package org.folio.entlinks.domain.repository;

import static org.folio.entlinks.utils.JdbcUtils.getFullPath;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.entity.projection.LinkCountViewImpl;
import org.folio.spring.FolioExecutionContext;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InstanceLinkJdbcRepository {

  private static final String INSTANCE_AUTHORITY_LINK_TABLE = "instance_authority_link";

  private final JdbcTemplate jdbcTemplate;
  private final FolioExecutionContext folioExecutionContext;

  public InstanceLinkJdbcRepository(JdbcTemplate jdbcTemplate, FolioExecutionContext folioExecutionContext) {
    this.jdbcTemplate = jdbcTemplate;
    this.folioExecutionContext = folioExecutionContext;
  }

  public List<LinkCountViewImpl> countLinksByAuthorityIds(Set<UUID> authorityIds, String tenantId) {
    var sql = "SELECT authority_id AS id, COUNT(DISTINCT instance_id) AS totalLinks "
        + "FROM %s WHERE authority_id IN (%s) GROUP BY authority_id".formatted(
        getFullPath(folioExecutionContext, tenantId, INSTANCE_AUTHORITY_LINK_TABLE),
        String.join(",", authorityIds.stream().map(id -> "'" + id + "'").toList()));
    return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(LinkCountViewImpl.class));
  }
}
