package org.folio.entlinks.controller.converter;

import java.util.List;
import org.folio.entlinks.domain.dto.AuthorityHeadingTypeDto;
import org.folio.entlinks.domain.dto.AuthorityHeadingTypeDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityHeadingType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.data.domain.Page;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthorityHeadingTypeMapper {

  @Mapping(target = "new", ignore = true)
  AuthorityHeadingType toEntity(AuthorityHeadingTypeDto authorityHeadingTypeDto);

  AuthorityHeadingTypeDto toDto(AuthorityHeadingType authorityHeadingType);

  List<AuthorityHeadingTypeDto> toDtoList(Iterable<AuthorityHeadingType> authorityHeadingTypes);

  default AuthorityHeadingTypeDtoCollection toAuthorityHeadingTypeCollection(
    Page<AuthorityHeadingType> authorityHeadingTypes) {
    var headingTypes = toDtoList(authorityHeadingTypes);
    return new AuthorityHeadingTypeDtoCollection(headingTypes, (int) authorityHeadingTypes.getTotalElements());
  }
}
