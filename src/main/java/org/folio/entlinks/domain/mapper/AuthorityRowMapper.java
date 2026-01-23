package org.folio.entlinks.domain.mapper;

import static org.folio.entlinks.domain.entity.AuthorityBase.DELETED_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.HEADING_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.HEADING_TYPE_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.IDENTIFIERS_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.ID_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.NATURAL_ID_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.NOTES_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.SAFT_HEADINGS_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.SFT_HEADINGS_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.SOURCE_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.SOURCE_FILE_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.SUBJECT_HEADING_CODE_COLUMN;
import static org.folio.entlinks.domain.entity.AuthorityBase.VERSION_COLUMN;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityIdentifier;
import org.folio.entlinks.domain.entity.AuthorityNote;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.HeadingRef;
import org.folio.entlinks.domain.entity.MetadataEntity;
import org.springframework.jdbc.core.RowMapper;

public class AuthorityRowMapper implements RowMapper<Authority> {

  private static final ObjectMapper MAPPER = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Override
  public Authority mapRow(ResultSet rs, int rowNum) throws SQLException {
    try {
      Authority a = new Authority();

      a.setId(rs.getObject(ID_COLUMN, UUID.class));
      a.setNaturalId(rs.getString(NATURAL_ID_COLUMN));
      a.setSource(rs.getString(SOURCE_COLUMN));
      a.setHeading(rs.getString(HEADING_COLUMN));
      a.setHeadingType(rs.getString(HEADING_TYPE_COLUMN));
      a.setVersion(rs.getInt(VERSION_COLUMN));

      var sourceFile = new AuthoritySourceFile();
      sourceFile.setId(rs.getObject(SOURCE_FILE_COLUMN, UUID.class));
      a.setAuthoritySourceFile(sourceFile);

      String subj = rs.getString(SUBJECT_HEADING_CODE_COLUMN);
      a.setSubjectHeadingCode(subj == null || subj.isBlank() ? null : subj.charAt(0));

      a.setDeleted(rs.getBoolean(DELETED_COLUMN));

      // metadata fields (from MetadataEntity)
      Timestamp createdDate = rs.getTimestamp(MetadataEntity.CREATED_DATE_COLUMN);
      if (createdDate != null) {
        a.setCreatedDate(createdDate);
      }
      Timestamp updatedDate = rs.getTimestamp(MetadataEntity.UPDATED_DATE_COLUMN);
      if (updatedDate != null) {
        a.setUpdatedDate(updatedDate);
      }
      UUID createdBy = rs.getObject(MetadataEntity.CREATED_BY_USER_COLUMN, UUID.class);
      if (createdBy != null) {
        a.setCreatedByUserId(createdBy);
      }
      UUID updatedBy = rs.getObject(MetadataEntity.UPDATED_BY_USER_COLUMN, UUID.class);
      if (updatedBy != null) {
        a.setUpdatedByUserId(updatedBy);
      }

      String sftJson = rs.getString(SFT_HEADINGS_COLUMN);
      if (sftJson != null && !sftJson.isBlank()) {
        List<HeadingRef> sft = MAPPER.readValue(sftJson, new TypeReference<>() {
        });
        a.setSftHeadings(sft);
      }

      String saftJson = rs.getString(SAFT_HEADINGS_COLUMN);
      if (saftJson != null && !saftJson.isBlank()) {
        List<HeadingRef> saft = MAPPER.readValue(saftJson, new TypeReference<>() {
        });
        a.setSaftHeadings(saft);
      }

      String idsJson = rs.getString(IDENTIFIERS_COLUMN);
      if (idsJson != null && !idsJson.isBlank()) {
        List<AuthorityIdentifier> ids = MAPPER.readValue(idsJson, new TypeReference<>() {
        });
        a.setIdentifiers(ids);
      }

      String notesJson = rs.getString(NOTES_COLUMN);
      if (notesJson != null && !notesJson.isBlank()) {
        List<AuthorityNote> notes = MAPPER.readValue(notesJson, new TypeReference<>() {
        });
        a.setNotes(notes);
      }

      return a;
    } catch (Exception e) {
      throw new SQLException("Failed to map Authority row", e);
    }
  }
}
