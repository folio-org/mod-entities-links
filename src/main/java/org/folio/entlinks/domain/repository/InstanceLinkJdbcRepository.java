package org.folio.entlinks.domain.repository;

import static org.folio.entlinks.utils.JdbcUtils.getFullPath;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.entity.projection.LinkCountViewImpl;
import org.folio.spring.FolioExecutionContext;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InstanceLinkJdbcRepository {

  private static final String INSTANCE_AUTHORITY_LINK_TABLE = "instance_authority_link";
  private static final String AUTHORITY_TABLE = "authority";

  private final JdbcTemplate jdbcTemplate;
  private final FolioExecutionContext folioExecutionContext;

  public InstanceLinkJdbcRepository(JdbcTemplate jdbcTemplate, FolioExecutionContext folioExecutionContext) {
    this.jdbcTemplate = jdbcTemplate;
    this.folioExecutionContext = folioExecutionContext;
  }

  public List<InstanceAuthorityLink> findByInstanceId(UUID id, String tenantId) {
    var linkTable = getFullPath(folioExecutionContext, tenantId, INSTANCE_AUTHORITY_LINK_TABLE);
    var authTable = getFullPath(folioExecutionContext, tenantId, AUTHORITY_TABLE);

    var sql = """
        SELECT i.id,
               i.authority_id,
               a.natural_id AS authority_natural_id,
               i.linking_rule_id,
               i.instance_id,
               i.status,
               i.error_cause
        FROM %s i
        LEFT JOIN %s a ON i.authority_id = a.id
        WHERE i.instance_id = ?
        """.formatted(linkTable, authTable);

    var delegate = new BeanPropertyRowMapper<>(InstanceAuthorityLink.class);

    return jdbcTemplate.query(sql, (rs, rowNum) -> {
      var link = delegate.mapRow(rs, rowNum);

      var authorityId = rs.getString("authority_id");
      if (authorityId != null) {
        var authority = new Authority();
        authority.setId(UUID.fromString(authorityId));

        var naturalId = rs.getString("authority_natural_id");
        if (naturalId != null) {
          authority.setNaturalId(naturalId);
        }
        link.setAuthority(authority);
      }

      var linkingRuleId = rs.getString("linking_rule_id");
      if (linkingRuleId != null) {
        var rule = new InstanceAuthorityLinkingRule();
        rule.setId(Integer.valueOf(linkingRuleId));
        link.setLinkingRule(rule);
      }
      return link;
    }, id);
  }

  public List<LinkCountViewImpl> countLinksByAuthorityIds(Set<UUID> authorityIds, String tenantId) {
    var sql = "SELECT authority_id AS id, COUNT(DISTINCT instance_id) AS totalLinks "
        + "FROM %s WHERE authority_id IN (%s) GROUP BY authority_id".formatted(
        getFullPath(folioExecutionContext, tenantId, INSTANCE_AUTHORITY_LINK_TABLE),
        String.join(",", authorityIds.stream().map(id -> "'" + id + "'").toList()));
    return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(LinkCountViewImpl.class));
  }
}
