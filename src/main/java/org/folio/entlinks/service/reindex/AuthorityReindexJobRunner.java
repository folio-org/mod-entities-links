package org.folio.entlinks.service.reindex;

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
import static org.folio.entlinks.domain.entity.MetadataEntity.CREATED_BY_USER_COLUMN;
import static org.folio.entlinks.domain.entity.MetadataEntity.CREATED_DATE_COLUMN;
import static org.folio.entlinks.domain.entity.MetadataEntity.UPDATED_BY_USER_COLUMN;
import static org.folio.entlinks.domain.entity.MetadataEntity.UPDATED_DATE_COLUMN;
import static org.folio.entlinks.utils.ObjectUtils.transformIfNotNull;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityIdentifier;
import org.folio.entlinks.domain.entity.AuthorityNote;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.HeadingRef;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AuthorityReindexJobRunner implements ReindexJobRunner {

  private static final TypeReference<HeadingRef[]> HEADING_TYPE_REF = new TypeReference<>() { };
  private static final TypeReference<AuthorityIdentifier[]> IDENTIFIER_TYPE_REF = new TypeReference<>() { };
  private static final TypeReference<AuthorityNote[]> NOTE_TYPE_REF = new TypeReference<>() { };
  private static final String COUNT_QUERY_TEMPLATE = "SELECT COUNT(*) FROM %s_mod_entities_links.authority";
  private static final String SELECT_QUERY_TEMPLATE =
    "SELECT * FROM %s_mod_entities_links.authority WHERE deleted = false";

  private final JdbcTemplate jdbcTemplate;
  private final FolioExecutionContext folioExecutionContext;
  private final ReindexService reindexService;
  private final AuthorityDomainEventPublisher eventPublisher;
  private final AuthorityMapper mapper;
  private final ObjectMapper objectMapper;

  @Async
  @Override
  @Transactional
  public void startReindex(ReindexJob reindexJob) {
    log.info("reindex::started");
    var reindexContext = new ReindexContext(reindexJob, folioExecutionContext);
    streamAuthorities(reindexContext);
    log.info("reindex::ended");
  }

  @Transactional(readOnly = true)
  public void streamAuthorities(ReindexContext context) {
    var totalRecords = jdbcTemplate.queryForObject(countQuery(context.getTenantId()), Integer.class);
    log.info("reindex::count={}", totalRecords);
    ReindexJobProgressTracker progressTracker = new ReindexJobProgressTracker(totalRecords == null ? 0 : totalRecords);

    jdbcTemplate.setFetchSize(50);
    var query = selectQuery(context.getTenantId());
    try (var authorityStream = jdbcTemplate.queryForStream(query, (rs, rowNum) -> toAuthority(rs))) {
      authorityStream
        .forEach(authority -> {
          eventPublisher.publishReindexEvent(authority, context);
          progressTracker.incrementProcessedCount();
          reindexService.logJobProgress(progressTracker, context.getJobId());
        });
    } catch (Exception e) {
      log.warn(e);
      reindexService.logJobFailed(context.getJobId());
      return;
    }

    reindexService.logJobSuccess(context.getJobId());
  }

  private AuthorityDto toAuthority(ResultSet rs) {
    var authority = new Authority();
    try {
      authority.setId(UUID.fromString(rs.getString(ID_COLUMN)));
      authority.setNaturalId(rs.getString(NATURAL_ID_COLUMN));
      authority.setSource(rs.getString(SOURCE_COLUMN));
      authority.setVersion(rs.getInt(VERSION_COLUMN));

      Optional.ofNullable(rs.getString(SOURCE_FILE_COLUMN))
        .ifPresent(sourceFileId -> {
          var sourceFile = new AuthoritySourceFile();
          sourceFile.setId(UUID.fromString(sourceFileId));
          authority.setAuthoritySourceFile(sourceFile);
        });

      authority.setHeading(rs.getString(HEADING_COLUMN));
      authority.setHeadingType(rs.getString(HEADING_TYPE_COLUMN));

      var subjectHeadingCode = rs.getString(SUBJECT_HEADING_CODE_COLUMN);
      authority.setSubjectHeadingCode(subjectHeadingCode != null ? subjectHeadingCode.charAt(0) : null);

      // read JSON arrays from SQL array columns
      authority.setSftHeadings(readJsonArray(rs, SFT_HEADINGS_COLUMN, HEADING_TYPE_REF));
      authority.setSaftHeadings(readJsonArray(rs, SAFT_HEADINGS_COLUMN, HEADING_TYPE_REF));
      authority.setIdentifiers(readJsonArray(rs, IDENTIFIERS_COLUMN, IDENTIFIER_TYPE_REF));
      authority.setNotes(readJsonArray(rs, NOTES_COLUMN, NOTE_TYPE_REF));

      // metadata
      populateMetadata(authority, rs);
    } catch (Exception e) {
      log.warn(e);
      throw new IllegalStateException(e);
    }

    return mapper.toDto(authority);
  }

  @SuppressWarnings("java:S1168")
  private <T> List<T> readJsonArray(ResultSet rs, String column, TypeReference<T[]> typeRef)
    throws SQLException, JsonProcessingException {
    var array = rs.getArray(column);
    if (array == null) {
      return null;
    }
    T[] values = objectMapper.readValue(array.toString(), typeRef);
    return Arrays.asList(values);
  }

  private void populateMetadata(Authority authority, ResultSet rs) throws SQLException {
    var createdDate = rs.getTimestamp(CREATED_DATE_COLUMN);
    authority.setCreatedDate(createdDate);
    var createdBy = rs.getString(CREATED_BY_USER_COLUMN);
    authority.setCreatedByUserId(transformIfNotNull(createdBy, UUID::fromString));
    var updatedDate = rs.getTimestamp(UPDATED_DATE_COLUMN);
    authority.setUpdatedDate(updatedDate);
    var updatedBy = rs.getString(UPDATED_BY_USER_COLUMN);
    authority.setUpdatedByUserId(transformIfNotNull(updatedBy, UUID::fromString));
  }

  private String countQuery(String tenant) {
    return String.format(COUNT_QUERY_TEMPLATE, tenant);
  }

  private String selectQuery(String tenant) {
    return String.format(SELECT_QUERY_TEMPLATE, tenant);
  }
}
