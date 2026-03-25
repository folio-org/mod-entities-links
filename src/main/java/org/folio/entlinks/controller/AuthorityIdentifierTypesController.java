package org.folio.entlinks.controller;

import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.AuthorityIdentifierTypeServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityIdentifierTypeDtoCollection;
import org.folio.entlinks.rest.resource.AuthorityIdentifierTypeApi;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class AuthorityIdentifierTypesController implements AuthorityIdentifierTypeApi {

  private final AuthorityIdentifierTypeServiceDelegate delegate;

  @Override
  public ResponseEntity<AuthorityIdentifierTypeDtoCollection> retrieveAuthorityIdentifierTypes(Integer offset,
                                                                                               Integer limit,
                                                                                               String query) {
    var authorityIdentifierTypes = delegate.getAuthorityIdentifierTypes(offset, limit, query);
    return ResponseEntity.ok(authorityIdentifierTypes);
  }
}
