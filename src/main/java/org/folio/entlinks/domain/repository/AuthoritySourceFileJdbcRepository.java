package org.folio.entlinks.domain.repository;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.entlinks.utils.JdbcUtils.getFullPath;
import static org.folio.entlinks.utils.JdbcUtils.getParamPlaceholder;
import static org.folio.entlinks.utils.JdbcUtils.getSchemaName;

import java.util.ArrayList;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.spring.FolioExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthoritySourceFileJdbcRepository {

  static final String AUTHORITY_SOURCE_FILE_CODE_TABLE = "authority_source_file_code";
  static final String AUTHORITY_SOURCE_FILE_TABLE = "authority_source_file";

  private final JdbcTemplate jdbcTemplate;
  private final FolioExecutionContext folioExecutionContext;

  public AuthoritySourceFileJdbcRepository(JdbcTemplate jdbcTemplate, FolioExecutionContext folioExecutionContext) {
    this.jdbcTemplate = jdbcTemplate;
    this.folioExecutionContext = folioExecutionContext;
  }

  public void insert(AuthoritySourceFile entity) {
    var sourceType = getFullPath(folioExecutionContext, "authority_source_file_source");
    var sqlValues = "%s::%s,%s".formatted(getParamPlaceholder(3), sourceType, getParamPlaceholder(10));

    var sql = """
              INSERT INTO %s (id, name, source, type, base_url_protocol, base_url, hrid_start_number, selectable,
              _version, created_date, updated_date, created_by_user_id, updated_by_user_id)
              VALUES (%s);
      """;
    jdbcTemplate.update(sql.formatted(getFullPath(folioExecutionContext, AUTHORITY_SOURCE_FILE_TABLE), sqlValues),
      entity.getId(), entity.getName(),
      entity.getSource().name(), entity.getType(), entity.getBaseUrlProtocol(), entity.getBaseUrl(),
      entity.getHridStartNumber(), entity.isSelectable(), 0, entity.getCreatedDate(), entity.getUpdatedDate(),
      entity.getCreatedByUserId(), entity.getUpdatedByUserId());

    if (isNotEmpty(entity.getAuthoritySourceFileCodes())) {
      insertSourceFileCodes(entity, entity.getId());
    }
  }

  public void update(AuthoritySourceFile entity, int version) {
    var sourceType = getFullPath(folioExecutionContext, "authority_source_file_source");
    var sql = """
              UPDATE %s
              SET name=?, source=?::%s, type=?, base_url_protocol=?, base_url=?, hrid_start_number=?, selectable=?,
              created_date=?, updated_date=?, created_by_user_id=?, updated_by_user_id=?, _version=?
              WHERE id = ? and _version = ?;
      """;

    var id = entity.getId();
    jdbcTemplate.update(sql.formatted(getFullPath(folioExecutionContext, AUTHORITY_SOURCE_FILE_TABLE), sourceType),
      entity.getName(), entity.getSource().name(), entity.getType(),
      entity.getBaseUrlProtocol(), entity.getBaseUrl(), entity.getHridStartNumber(), entity.isSelectable(),
      entity.getCreatedDate(), entity.getUpdatedDate(), entity.getCreatedByUserId(),
      entity.getUpdatedByUserId(), entity.getVersion(), id, version);

    if (isNotEmpty(entity.getAuthoritySourceFileCodes())) {
      jdbcTemplate.execute("DELETE FROM %s WHERE authority_source_file_id = '%s';"
        .formatted(getFullPath(folioExecutionContext, AUTHORITY_SOURCE_FILE_CODE_TABLE), id));

      insertSourceFileCodes(entity, id);
    }
  }

  public void delete(UUID id) {
    jdbcTemplate.execute("DELETE FROM %s WHERE authority_source_file_id = '%s'"
      .formatted(getFullPath(folioExecutionContext, AUTHORITY_SOURCE_FILE_CODE_TABLE), id));
    jdbcTemplate.execute("DELETE FROM %s WHERE id = '%s'"
      .formatted(getFullPath(folioExecutionContext, AUTHORITY_SOURCE_FILE_TABLE), id));
  }

  public void createSequence(String sequenceName, int startNumber) {
    var command = String.format("""
        CREATE SEQUENCE IF NOT EXISTS %s MINVALUE %d INCREMENT BY 1 OWNED BY %s.authority_source_file.sequence_name;
        """,
      sequenceName, startNumber, getSchemaName(folioExecutionContext));
    jdbcTemplate.execute(command);
  }

  public void dropSequence(String sequenceName) {
    var command = String.format("DROP SEQUENCE IF EXISTS %s.%s;", getSchemaName(folioExecutionContext), sequenceName);
    jdbcTemplate.execute(command);
  }

  private void insertSourceFileCodes(AuthoritySourceFile entity, UUID id) {
    var sourceFileCodes = new ArrayList<>(entity.getAuthoritySourceFileCodes());
    jdbcTemplate.update("INSERT INTO %s (authority_source_file_id, code) VALUES %s;"
        .formatted(getFullPath(folioExecutionContext, AUTHORITY_SOURCE_FILE_CODE_TABLE),
          getParamPlaceholder(sourceFileCodes.size(), 2)),
      ps -> {
        for (int i = 0; i < sourceFileCodes.size(); i++) {
          var sourceFileCode = sourceFileCodes.get(i);
          ps.setObject(i * 2 + 1, id);
          ps.setString(i * 2 + 2, sourceFileCode.getCode());
        }
      });
  }
}
