package org.folio.entlinks.domain.repository;

import static org.folio.entlinks.utils.JdbcUtils.getFullPath;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityRowMapper;
import org.folio.entlinks.service.authority.NaturalIdsData;
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

  public List<Authority> findAllByIdInAndDeletedFalse(Collection<UUID> ids, String tenantId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    var sql = "SELECT * FROM %s WHERE id IN (%s) AND deleted = false".formatted(
        getFullPath(folioExecutionContext, tenantId, AUTHORITY_TABLE),
        String.join(",", ids.stream().map(id -> "'" + id + "'").toList()));
    return jdbcTemplate.query(sql, new AuthorityRowMapper());
  }

  public List<Authority> findByNaturalIdInAndDeletedFalse(Collection<String> ids, String tenantId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    var sql = "SELECT * FROM %s WHERE natural_id IN (%s) AND deleted = false".formatted(
        getFullPath(folioExecutionContext, tenantId, AUTHORITY_TABLE),
        String.join(",", ids.stream().map(id -> "'" + id + "'").toList()));
    return jdbcTemplate.query(sql, new AuthorityRowMapper());
  }

  public List<NaturalIdsData> findNaturalIdsByIdInAndDeletedFalse(Collection<UUID> ids, String tenantId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    var sql = "SELECT id, natural_id FROM %s WHERE id IN (%s) AND deleted = false".formatted(
        getFullPath(folioExecutionContext, tenantId, AUTHORITY_TABLE),
        String.join(",", ids.stream().map(id -> "'" + id + "'").toList()));
    return jdbcTemplate.query(sql, (rs, rowNum) ->
        new NaturalIdsDataImpl(rs.getObject("id", UUID.class), rs.getString("natural_id"))
    );
  }

  private record NaturalIdsDataImpl(UUID id, String naturalId) implements NaturalIdsData {
    @Override
    public UUID getId() {
      return id;
    }

    @Override
    public String getNaturalId() {
      return naturalId;
    }
  }
}
