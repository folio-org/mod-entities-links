package org.folio.entlinks.domain.repository;

import static org.folio.entlinks.utils.JdbcUtils.getFullPath;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.mapper.AuthorityRowMapper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthorityJdbcRepository {
  private static final String AUTHORITY_TABLE = "authority";

  private final JdbcTemplate jdbcTemplate;
  private final FolioExecutionContext folioExecutionContext;

  public AuthorityJdbcRepository(JdbcTemplate jdbcTemplate, FolioExecutionContext folioExecutionContext) {
    this.jdbcTemplate = jdbcTemplate;
    this.folioExecutionContext = folioExecutionContext;
  }

  @SuppressWarnings("java:S2077") //dynamically formatted query is safe here
  public List<Authority> findAllByIdInAndDeletedFalse(Collection<UUID> ids, String tenantId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    var sql = "SELECT * FROM %s WHERE id IN (%s) AND deleted = false".formatted(
      getFullPath(folioExecutionContext, tenantId, AUTHORITY_TABLE),
      String.join(",", ids.stream().map(id -> "'" + id + "'").toList()));
    return jdbcTemplate.query(sql, new AuthorityRowMapper());
  }

  @SuppressWarnings("java:S2077") //dynamically formatted query is safe here
  public List<Authority> findByNaturalIdInAndDeletedFalse(Collection<String> ids, String tenantId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    var sql = "SELECT * FROM %s WHERE natural_id IN (%s) AND deleted = false".formatted(
      getFullPath(folioExecutionContext, tenantId, AUTHORITY_TABLE),
      String.join(",", ids.stream().map(id -> "'" + id + "'").toList()));
    return jdbcTemplate.query(sql, new AuthorityRowMapper());
  }

  @SuppressWarnings("java:S2077") //dynamically formatted query is safe here
  public List<UUID> findExistingIdsByIdsAndDeletedFalse(Collection<UUID> ids, String tenantId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    var sql = "SELECT id FROM %s WHERE id IN (%s) AND deleted = false".formatted(
      getFullPath(folioExecutionContext, tenantId, AUTHORITY_TABLE),
      String.join(",", ids.stream().map(id -> "'" + id + "'").toList()));
    return jdbcTemplate.query(sql, (rs, rowNum) -> UUID.fromString(rs.getString("id")));
  }

  @SuppressWarnings("java:S2077") //dynamically formatted query is safe here
  public Map<UUID, String> findAuthorityNaturalIdsByIdsAndDeletedFalse(Collection<UUID> ids, String tenantId) {
    if (ids == null || ids.isEmpty()) {
      return Collections.emptyMap();
    }
    var sql = "SELECT id, natural_id FROM %s WHERE id IN (%s) AND deleted = false".formatted(
      getFullPath(folioExecutionContext, tenantId, AUTHORITY_TABLE),
      String.join(",", ids.stream().map(id -> "'" + id + "'").toList()));
    return jdbcTemplate.query(sql, rs -> {
      var result = new HashMap<UUID, String>();
      while (rs.next()) {
        result.put(UUID.fromString(rs.getString("id")), rs.getString("natural_id"));
      }
      return result;
    });
  }
}
