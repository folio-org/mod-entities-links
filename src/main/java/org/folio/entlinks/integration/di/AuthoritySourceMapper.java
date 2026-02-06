package org.folio.entlinks.integration.di;

import static org.folio.ActionProfile.FolioRecord.MARC_AUTHORITY;
import static org.folio.ActionProfile.FolioRecord.MARC_AUTHORITY_EXTENDED;
import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.MARC_SOURCE;
import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.isAuthorityExtendedMode;

import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.DataImportEventPayload;
import org.folio.Record;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.internal.MappingRulesService;
import org.folio.processing.mapping.defaultmapper.RecordMapper;
import org.folio.processing.mapping.defaultmapper.RecordMapperBuilder;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting MARC authority records to AuthorityDto objects.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class AuthoritySourceMapper {

  private final MappingRulesService mappingRulesService;

  public AuthorityDto map(DataImportEventPayload payload) {
    var mappingMetadata = mappingRulesService.getMappingMetadata(payload.getJobExecutionId());
    var rawAuthority = getRecordMapper()
      .mapRecord(getSourceRecord(payload), mappingMetadata.mappingParameters(), mappingMetadata.mappingRules());
    var authority = JsonObject.mapFrom(rawAuthority).mapTo(AuthorityDto.class);
    authority.setSource(MARC_SOURCE);
    return authority;
  }

  private RecordMapper<Object> getRecordMapper() {
    return isAuthorityExtendedMode()
           ? RecordMapperBuilder.buildMapper(MARC_AUTHORITY_EXTENDED.value())
           : RecordMapperBuilder.buildMapper(MARC_AUTHORITY.value());
  }

  private JsonObject getSourceRecord(DataImportEventPayload payload) {
    var authorityJson = payload.getContext().get(MARC_AUTHORITY.value());
    var parsedRecordContent = new JsonObject(authorityJson).mapTo(Record.class).getParsedRecord().getContent();
    return new JsonObject((String) parsedRecordContent);
  }
}
