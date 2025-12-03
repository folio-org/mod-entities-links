package org.folio.entlinks.domain.repository;

import static org.folio.entlinks.utils.JdbcUtils.getFullPath;
import static org.folio.entlinks.utils.JdbcUtils.getSchemaName;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.entity.projection.LinkCountViewImpl;
import org.folio.spring.FolioExecutionContext;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class InstanceLinkJdbcRepository {

  private static final String INSTANCE_AUTHORITY_LINK_TABLE = "instance_authority_link";
  private static final String AUTHORITY_TABLE = "authority";
  private static final String INSTANCE_AUTHORITY_LINKING_RULE_TABLE = "instance_authority_linking_rule";

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

  public List<InstanceAuthorityLink> findAll(InstanceAuthorityLinkStatus status,
                                             Timestamp fromDate,
                                             Timestamp toDate,
                                             String tenant,
                                             Pageable pageable) {

    List<Object> params = new ArrayList<>();
    List<String> conditions = new ArrayList<>();

    if (status != null) {
      conditions.add("l.status = ?::%s.linkstatus".formatted(getSchemaName(folioExecutionContext, tenant)));
      params.add(status.name());
    }
    if (fromDate != null) {
      conditions.add("l.updated_at >= ?");
      params.add(fromDate);
    }
    if (toDate != null) {
      conditions.add("l.updated_at <= ?");
      params.add(toDate);
    }
    StringBuilder sql = new StringBuilder("""
            SELECT l.*, auth.natural_id, r.bib_field
            FROM %s l
            LEFT JOIN %s auth ON l.authority_id = auth.id
            LEFT JOIN %s r ON l.linking_rule_id = r.id
        """.formatted(
        getFullPath(folioExecutionContext, tenant, INSTANCE_AUTHORITY_LINK_TABLE),
        getFullPath(folioExecutionContext, tenant, AUTHORITY_TABLE),
        getFullPath(folioExecutionContext, tenant, INSTANCE_AUTHORITY_LINKING_RULE_TABLE)
    ));
    if (!conditions.isEmpty()) {
      sql.append(" WHERE ").append(String.join(" AND ", conditions));
    }

    sql.append(" ORDER BY l.updated_at DESC");
    sql.append(" LIMIT ").append(pageable.getPageSize());
    sql.append(" OFFSET ").append(pageable.getOffset());

    return jdbcTemplate.query(sql.toString(), params.toArray(), linkRowMapper());
  }

  private RowMapper<InstanceAuthorityLink> linkRowMapper() {
    return (rs, rowNum) -> {
      InstanceAuthorityLink link = new InstanceAuthorityLink();
      link.setId(rs.getLong("id"));
      link.setInstanceId(rs.getObject("instance_id", UUID.class));
      link.setAuthorityId(rs.getObject("authority_id", UUID.class));
      link.setErrorCause(rs.getString("error_cause"));

      String statusStr = rs.getString("status");
      if (statusStr != null) {
        link.setStatus(InstanceAuthorityLinkStatus.valueOf(statusStr));
      }

      link.setUpdatedAt(rs.getTimestamp("updated_at"));
      link.setNaturalId(rs.getString("natural_id"));

      Integer linkingRuleId = rs.getObject("linking_rule_id", Integer.class);
      if (linkingRuleId != null) {
        InstanceAuthorityLinkingRule rule = new InstanceAuthorityLinkingRule();
        rule.setId(linkingRuleId);
        rule.setBibField(rs.getString("bib_field"));
        link.setLinkingRule(rule);
      }
      return link;
    };
  }
}
