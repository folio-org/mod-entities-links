package org.folio.entlinks.domain.repository;

import static org.folio.entlinks.utils.JdbcUtils.getFullPath;
import static org.folio.entlinks.utils.JdbcUtils.getSchemaName;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.domain.entity.AuthorityDataStatStatus;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AuthorityDataStatJdbcRepository {

  private static final String AUTHORITY_DATA_STAT_TABLE = "authority_data_stat";
  private static final String AUTHORITY_TABLE = "authority";

  private final JdbcTemplate jdbcTemplate;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  public AuthorityDataStatJdbcRepository(JdbcTemplate jdbcTemplate, FolioModuleMetadata folioModuleMetadata,
                                         FolioExecutionContext folioExecutionContext) {
    this.jdbcTemplate = jdbcTemplate;
    this.folioModuleMetadata = folioModuleMetadata;
    this.folioExecutionContext = folioExecutionContext;
  }

  public List<AuthorityDataStat> findActualByActionAndDate(AuthorityDataStatAction action,
                                                           Timestamp startedAtStart,
                                                           Timestamp startedAtEnd,
                                                           Pageable pageable, String tenant) {

    String sql = """
        SELECT a.*
        FROM %s a
        JOIN %s auth ON a.authority_id = auth.id
        WHERE a.action = ?::%s.authoritydatastataction
          AND a.started_at BETWEEN ? AND ?
          AND auth.deleted = false
        ORDER BY a.started_at DESC
        LIMIT %d OFFSET %d
        """
        .formatted(
            getFullPath(folioModuleMetadata, tenant, AUTHORITY_DATA_STAT_TABLE),
            getFullPath(folioModuleMetadata, tenant, AUTHORITY_TABLE),
            getSchemaName(folioExecutionContext, tenant),
            pageable.getPageSize(),
            pageable.getOffset()
        );
    Object[] params = {action.name(), startedAtStart, startedAtEnd};
    return jdbcTemplate.query(sql, params, authorityDataStatRowMapper());
  }

  private RowMapper<AuthorityDataStat> authorityDataStatRowMapper() {
    return (rs, rowNum) -> {
      var stat = new AuthorityDataStat();
      stat.setId(rs.getObject("id", UUID.class));
      var authority = new Authority();
      authority.setId(rs.getObject("authority_id", UUID.class));
      stat.setAuthority(authority);
      stat.setAction(AuthorityDataStatAction.valueOf(rs.getString("action")));
      stat.setAuthorityNaturalIdOld(rs.getString("authority_natural_id_old"));
      stat.setAuthorityNaturalIdNew(rs.getString("authority_natural_id_new"));
      stat.setHeadingOld(rs.getString("heading_old"));
      stat.setHeadingNew(rs.getString("heading_new"));
      stat.setHeadingTypeOld(rs.getString("heading_type_old"));
      stat.setHeadingTypeNew(rs.getString("heading_type_new"));
      stat.setAuthoritySourceFileOld(rs.getObject("authority_source_file_old", UUID.class));
      stat.setAuthoritySourceFileNew(rs.getObject("authority_source_file_new", UUID.class));
      stat.setLbTotal(rs.getInt("lb_total"));
      stat.setLbUpdated(rs.getInt("lb_updated"));
      stat.setLbFailed(rs.getInt("lb_failed"));
      stat.setStatus(AuthorityDataStatStatus.valueOf(rs.getString("status")));
      stat.setFailCause(rs.getString("fail_cause"));
      stat.setStartedByUserId(rs.getObject("started_by_user_id", UUID.class));
      stat.setStartedAt(rs.getTimestamp("started_at"));
      stat.setCompletedAt(rs.getTimestamp("completed_at"));
      return stat;
    };
  }
}
