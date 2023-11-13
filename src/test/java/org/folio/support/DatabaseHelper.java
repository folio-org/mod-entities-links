package org.folio.support;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityNoteType;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.entlinks.utils.DateUtils;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

public class DatabaseHelper {

  public static final String AUTHORITY_DATA_STAT_TABLE = "authority_data_stat";
  public static final String INSTANCE_AUTHORITY_LINK_TABLE = "instance_authority_link";
  public static final String AUTHORITY_NOTE_TYPE_TABLE = "authority_note_type";
  public static final String AUTHORITY_SOURCE_FILE_TABLE = "authority_source_file";
  public static final String AUTHORITY_SOURCE_FILE_CODE_TABLE = "authority_source_file_code";
  public static final String AUTHORITY_TABLE = "authority";
  public static final String AUTHORITY_ARCHIVE_TABLE = "authority_archive";
  public static final String AUTHORITY_REINDEX_JOB_TABLE = "reindex_job";

  private final FolioModuleMetadata metadata;
  private final JdbcTemplate jdbcTemplate;

  public DatabaseHelper(FolioModuleMetadata metadata, JdbcTemplate jdbcTemplate) {
    this.metadata = metadata;
    this.jdbcTemplate = jdbcTemplate;
  }

  public String getTable(String tenantId, String table) {
    return metadata.getDBSchemaName(tenantId) + "." + table;
  }

  public int countRows(String tableName, String tenant) {
    return JdbcTestUtils.countRowsInTable(jdbcTemplate, getTable(tenant, tableName));
  }

  public int countRowsWhere(String tableName, String tenant, String whereClause) {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, getTable(tenant, tableName), whereClause);
  }

  public void deleteFromTable(String tableName, String tenant) {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, getTable(tenant, tableName));
  }

  public void saveAuthorityNoteType(String tenant, AuthorityNoteType entity) {
    var sql = "INSERT INTO " + getTable(tenant, AUTHORITY_NOTE_TYPE_TABLE)
      + " (id, name, source, created_date, updated_date, created_by_user_id, "
      + "updated_by_user_id) VALUES (?,?,?,?,?,?,?)";
    jdbcTemplate.update(sql, entity.getId(), entity.getName(), entity.getSource(), entity.getCreatedDate(),
      entity.getUpdatedDate(), entity.getCreatedByUserId(), entity.getUpdatedByUserId());
  }

  public void saveAuthoritySourceFile(String tenant, AuthoritySourceFile entity) {
    var sql = "INSERT INTO " + getTable(tenant, AUTHORITY_SOURCE_FILE_TABLE)
      + " (id, name, source, type, base_url, created_date, updated_date,"
      + "created_by_user_id, updated_by_user_id) VALUES (?,?,?,?,?,?,?,?,?)";
    jdbcTemplate.update(sql, entity.getId(), entity.getName(),
      entity.getSource(), entity.getType(), entity.getBaseUrl(), entity.getCreatedDate(),
      entity.getUpdatedDate(), entity.getCreatedByUserId(), entity.getUpdatedByUserId());
  }

  public void updateAuthorityNaturalId(String tenant, UUID authorityId, String naturalId) {
    var sql = "UPDATE " + getTable(tenant, AUTHORITY_TABLE)
        + " SET natural_id = ? where id = ?";
    jdbcTemplate.update(sql, naturalId, authorityId);
  }

  public void updateAuthorityArchiveUpdateDate(String tenant, UUID authorityArchiveId, Timestamp updatedDate) {
    var sql = "UPDATE " + getTable(tenant, AUTHORITY_ARCHIVE_TABLE)
        + " SET updated_date = ? where id = ?";
    jdbcTemplate.update(sql, updatedDate, authorityArchiveId);
  }

  public void saveAuthoritySourceFileCode(String tenant, UUID sourceFileId, AuthoritySourceFileCode code) {
    var sql = "INSERT INTO " + getTable(tenant, AUTHORITY_SOURCE_FILE_CODE_TABLE)
      + " (authority_source_file_id, code) VALUES (?,?)";
    jdbcTemplate.update(sql, sourceFileId, code.getCode());
  }

  public void saveAuthority(String tenant, Authority entity) {
    var sql = "INSERT INTO " + getTable(tenant, AUTHORITY_TABLE)
        +  " (id, _version, natural_id, source, heading, heading_type, subject_heading_code, created_date, "
        + "created_by_user_id, updated_date, updated_by_user_id, deleted, source_file_id) "
        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    var sourceFileId = Optional.ofNullable(entity.getAuthoritySourceFile())
        .map(AuthoritySourceFile::getId)
        .orElse(null);
    jdbcTemplate.update(sql, entity.getId(), entity.getVersion(), entity.getNaturalId(), entity.getSource(),
        entity.getHeading(), entity.getHeadingType(), entity.getSubjectHeadingCode(), entity.getCreatedDate(),
        entity.getCreatedByUserId(), entity.getUpdatedDate(), entity.getUpdatedByUserId(),
        entity.isDeleted(), sourceFileId);
  }

  public AuthorityNoteType getAuthorityNoteTypeById(UUID id, String tenant) {
    String sql = "SELECT * FROM " + getTable(tenant, AUTHORITY_NOTE_TYPE_TABLE) + " WHERE id = ?";
    return jdbcTemplate.query(sql, new Object[] {id}, rs -> {
      if (rs.next()) {
        var authorityNoteType = new AuthorityNoteType();
        authorityNoteType.setId(UUID.fromString(rs.getString("id")));
        authorityNoteType.setName(rs.getString("name"));
        authorityNoteType.setSource(rs.getString("source"));
        return authorityNoteType;
      } else {
        return null;
      }
    });
  }

  public void saveAuthorityReindexJob(String tenant, ReindexJob job) {
    var sql = "INSERT INTO " + getTable(tenant, AUTHORITY_REINDEX_JOB_TABLE)
        + " (id, resource_name, job_status, published, submitted_date) VALUES (?,?,?,?,?)";
    jdbcTemplate.update(sql, job.getId(), job.getResourceName().name(), job.getJobStatus().name(),
        job.getPublished(), DateUtils.toTimestamp(job.getSubmittedDate()));
  }
}
