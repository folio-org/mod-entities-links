package org.folio.entlinks.config;

import static org.folio.ActionProfile.FolioRecord.MARC_AUTHORITY;
import static org.folio.ActionProfile.FolioRecord.MARC_AUTHORITY_EXTENDED;

import org.folio.processing.mapping.defaultmapper.RecordMapper;
import org.folio.processing.mapping.defaultmapper.RecordMapperBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataImportConfiguration {

  @Bean
  public RecordMapper<Object> extendedRecordMapper() {
    return RecordMapperBuilder.buildMapper(MARC_AUTHORITY_EXTENDED.value());
  }

  @Bean
  public RecordMapper<Object> simpleRecordMapper() {
    return RecordMapperBuilder.buildMapper(MARC_AUTHORITY.value());
  }
}
