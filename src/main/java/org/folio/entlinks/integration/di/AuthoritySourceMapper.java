package org.folio.entlinks.integration.di;

import static org.folio.ActionProfile.FolioRecord.MARC_AUTHORITY;
import static org.folio.ActionProfile.FolioRecord.MARC_AUTHORITY_EXTENDED;
import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.MARC_SOURCE;

import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.ActionProfile.FolioRecord;
import org.folio.DataImportEventPayload;
import org.folio.Record;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.SettingsService;
import org.folio.entlinks.integration.internal.MappingRulesService;
import org.folio.processing.mapping.defaultmapper.RecordMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting MARC authority records to AuthorityDto objects.
 */
@Log4j2
@Component
public class AuthoritySourceMapper {

  private final Cache cache;
  private final SettingsService settingsService;
  private final MappingRulesService mappingRulesService;
  private final Map<FolioRecord, RecordMapper<Object>> recordMappers;

  public AuthoritySourceMapper(@Qualifier("authorityExtendedMappingEnabledCache") Cache cache,
                               SettingsService settingsService,
                               MappingRulesService mappingRulesService,
                               List<RecordMapper<Object>> recordMappers) {
    this.cache = cache;
    this.settingsService = settingsService;
    this.mappingRulesService = mappingRulesService;
    this.recordMappers = recordMappers.stream().collect(Collectors.toMap(
      recordMapper -> FolioRecord.fromValue(recordMapper.getMapperFormat()),
      Function.identity()));
  }

  public AuthorityDto map(DataImportEventPayload payload) {
    var mappingMetadata = mappingRulesService.getMappingMetadata(payload.getJobExecutionId());
    var rawAuthority = getRecordMapper(payload.getJobExecutionId())
      .mapRecord(getSourceRecord(payload), mappingMetadata.mappingParameters(), mappingMetadata.mappingRules());
    var authority = JsonObject.mapFrom(rawAuthority).mapTo(AuthorityDto.class);
    authority.setSource(MARC_SOURCE);
    return authority;
  }

  private RecordMapper<Object> getRecordMapper(String jobExecutionId) {
    var isExtendedMappingEnabled = cache.get(jobExecutionId, settingsService::isAuthorityExtendedMappingEnabled);
    return Boolean.TRUE.equals(isExtendedMappingEnabled)
           ? recordMappers.get(MARC_AUTHORITY_EXTENDED)
           : recordMappers.get(MARC_AUTHORITY);
  }

  private JsonObject getSourceRecord(DataImportEventPayload payload) {
    var authorityJson = payload.getContext().get(MARC_AUTHORITY.value());
    var parsedRecordContent = new JsonObject(authorityJson).mapTo(Record.class).getParsedRecord().getContent();
    return new JsonObject((String) parsedRecordContent);
  }
}
