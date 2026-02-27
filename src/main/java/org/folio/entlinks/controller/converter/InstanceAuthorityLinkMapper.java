package org.folio.entlinks.controller.converter;

import java.util.List;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InstanceAuthorityLinkMapper {

  @Mapping(target = "linkingRuleId", source = "linkingRule.id")
  InstanceLinkDto convertToDto(InstanceAuthorityLink source);

  default InstanceLinkDtoCollection convertToDto(List<InstanceAuthorityLink> source) {
    var convertedLinks = source.stream().map(this::convertToDto).toList();

    return new InstanceLinkDtoCollection(convertedLinks)
      .totalRecords(source.size());
  }

  @Mapping(target = "linkingRule.id", source = "linkingRuleId")
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "errorCause", ignore = true)
  InstanceAuthorityLink convertDto(InstanceLinkDto source);

  List<InstanceAuthorityLink> convertDto(List<InstanceLinkDto> source);
}
