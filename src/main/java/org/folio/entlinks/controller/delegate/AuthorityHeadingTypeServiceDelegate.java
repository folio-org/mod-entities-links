package org.folio.entlinks.controller.delegate;

import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.converter.AuthorityHeadingTypeMapper;
import org.folio.entlinks.domain.dto.AuthorityHeadingTypeDtoCollection;
import org.folio.entlinks.service.authority.AuthorityHeadingTypeService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorityHeadingTypeServiceDelegate {

  private final AuthorityHeadingTypeService service;
  private final AuthorityHeadingTypeMapper mapper;

  public AuthorityHeadingTypeDtoCollection getAuthorityHeadingTypes(Integer offset, Integer limit, String cqlQuery) {
    var headingTypes = service.getAll(offset, limit, cqlQuery);
    return mapper.toAuthorityHeadingTypeCollection(headingTypes);
  }
}
