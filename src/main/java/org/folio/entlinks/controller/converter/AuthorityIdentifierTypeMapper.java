package org.folio.entlinks.controller.converter;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import org.folio.entlinks.domain.dto.AuthorityIdentifierTypeDto;
import org.folio.entlinks.domain.dto.AuthorityIdentifierTypeDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityIdentifierType;
import org.folio.entlinks.utils.DateUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.data.domain.Page;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthorityIdentifierTypeMapper {

  @Mapping(target = "new", ignore = true)
  @Mapping(target = "updatedDate", ignore = true)
  @Mapping(target = "updatedByUserId", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "createdByUserId", ignore = true)
  AuthorityIdentifierType toEntity(AuthorityIdentifierTypeDto authorityIdentifierTypeDto);

  @Mapping(target = "metadata.updatedDate", source = "updatedDate")
  @Mapping(target = "metadata.updatedByUserId", source = "updatedByUserId")
  @Mapping(target = "metadata.createdDate", source = "createdDate")
  @Mapping(target = "metadata.createdByUserId", source = "createdByUserId")
  AuthorityIdentifierTypeDto toDto(AuthorityIdentifierType authorityIdentifierType);

  List<AuthorityIdentifierTypeDto> toDtoList(Iterable<AuthorityIdentifierType> authorityIdentifierTypes);

  default AuthorityIdentifierTypeDtoCollection toAuthorityIdentifierTypeCollection(
    Page<AuthorityIdentifierType> authorityIdentifierTypes) {
    var identifierTypes = toDtoList(authorityIdentifierTypes);
    return new AuthorityIdentifierTypeDtoCollection(identifierTypes, (int) authorityIdentifierTypes.getTotalElements());
  }

  default OffsetDateTime map(Timestamp timestamp) {
    return DateUtils.fromTimestamp(timestamp);
  }
}
