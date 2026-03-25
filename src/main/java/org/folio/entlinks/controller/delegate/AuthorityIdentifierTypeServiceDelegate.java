package org.folio.entlinks.controller.delegate;

import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.converter.AuthorityIdentifierTypeMapper;
import org.folio.entlinks.domain.dto.AuthorityIdentifierTypeDtoCollection;
import org.folio.entlinks.service.authority.AuthorityIdentifierTypeService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorityIdentifierTypeServiceDelegate {

  private final AuthorityIdentifierTypeService service;
  private final AuthorityIdentifierTypeMapper mapper;

  public AuthorityIdentifierTypeDtoCollection getAuthorityIdentifierTypes(Integer offset, Integer limit,
                                                                          String cqlQuery) {
    var identifierTypes = service.getAll(offset, limit, cqlQuery);
    return mapper.toAuthorityIdentifierTypeCollection(identifierTypes);
  }
}
