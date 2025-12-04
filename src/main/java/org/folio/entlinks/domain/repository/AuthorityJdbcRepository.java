package org.folio.entlinks.domain.repository;

import static org.folio.entlinks.utils.JdbcUtils.getFullPath;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityRowMapper;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthorityJdbcRepository {

  private static final String AUTHORITY_TABLE = "authority";

  private final JdbcTemplate jdbcTemplate;
  private final FolioModuleMetadata folioModuleMetadata;

  public AuthorityJdbcRepository(JdbcTemplate jdbcTemplate, FolioModuleMetadata folioModuleMetadata) {
    this.jdbcTemplate = jdbcTemplate;
    this.folioModuleMetadata = folioModuleMetadata;
  }

  public List<Authority> findAllByIdInAndDeletedFalse(Collection<UUID> ids, String tenantId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    var sql = "SELECT * FROM %s WHERE id IN (%s) AND deleted = false".formatted(
        getFullPath(folioModuleMetadata, tenantId, AUTHORITY_TABLE),
        String.join(",", ids.stream().map(id -> "'" + id + "'").toList()));
    return jdbcTemplate.query(sql, new AuthorityRowMapper());
  }

  public List<Authority> findByNaturalIdInAndDeletedFalse(Collection<String> ids, String tenantId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    var sql = "SELECT * FROM %s WHERE natural_id IN (%s) AND deleted = false".formatted(
        getFullPath(folioModuleMetadata, tenantId, AUTHORITY_TABLE),
        String.join(",", ids.stream().map(id -> "'" + id + "'").toList()));
    return jdbcTemplate.query(sql, new AuthorityRowMapper());
  }
}
