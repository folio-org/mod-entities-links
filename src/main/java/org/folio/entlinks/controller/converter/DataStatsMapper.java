package org.folio.entlinks.controller.converter;

import static org.folio.entlinks.utils.DateUtils.fromTimestamp;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import org.folio.entlinks.domain.dto.AuthorityStatsDto;
import org.folio.entlinks.domain.dto.BibStatsDto;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DataStatsMapper {

  @Mapping(target = "authorityId", source = "authorityData.id")
  @Mapping(target = "naturalIdOld", source = "authorityNaturalIdOld")
  @Mapping(target = "naturalIdNew", source = "authorityNaturalIdNew")
  @Mapping(target = "sourceFileOld", source = "authoritySourceFileOld")
  @Mapping(target = "sourceFileNew", source = "authoritySourceFileNew")
  AuthorityStatsDto convertToDto(AuthorityDataStat source);

  @Mapping(target = "authorityNaturalId", source = "authorityData.naturalId")
  BibStatsDto convertToDto(InstanceAuthorityLink source);

  default OffsetDateTime map(Timestamp timestamp) {
    return fromTimestamp(timestamp);
  }

}
