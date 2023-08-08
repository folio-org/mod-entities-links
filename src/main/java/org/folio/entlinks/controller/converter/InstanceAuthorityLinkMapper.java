package org.folio.entlinks.controller.converter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.LinksCountDto;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InstanceAuthorityLinkMapper {

  @Mapping(target = "authorityId", source = "authority.id")
  @Mapping(target = "authorityNaturalId", source = "authority.naturalId")
  @Mapping(target = "linkingRuleId", source = "linkingRule.id")
  InstanceLinkDto convertToDto(InstanceAuthorityLink source);

  default InstanceLinkDtoCollection convertToDto(List<InstanceAuthorityLink> source) {
    var convertedLinks = source.stream().map(this::convertToDto).toList();

    return new InstanceLinkDtoCollection(convertedLinks)
      .totalRecords(source.size());
  }

  @Mapping(target = "authority.id", source = "authorityId")
  @Mapping(target = "authority.naturalId", source = "authorityNaturalId")
  @Mapping(target = "linkingRule.id", source = "linkingRuleId")
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "errorCause", ignore = true)
  InstanceAuthorityLink convertDto(InstanceLinkDto source);

  List<InstanceAuthorityLink> convertDto(List<InstanceLinkDto> source);

  default List<LinksCountDto> convert(Map<UUID, Integer> source) {
    return source.entrySet().stream()
      .map(e -> new LinksCountDto().id(e.getKey()).totalLinks(e.getValue()))
      .toList();
  }
}
