package org.folio.entlinks.controller;

import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.AuthorityHeadingTypeServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityHeadingTypeDtoCollection;
import org.folio.entlinks.rest.resource.AuthorityHeadingTypeApi;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class AuthorityHeadingTypesController implements AuthorityHeadingTypeApi {

  private final AuthorityHeadingTypeServiceDelegate delegate;

  @Override
  public ResponseEntity<AuthorityHeadingTypeDtoCollection> retrieveAuthorityHeadingTypes(Integer offset,
                                                                                          Integer limit,
                                                                                          String query) {
    var authorityHeadingTypes = delegate.getAuthorityHeadingTypes(offset, limit, query);
    return ResponseEntity.ok(authorityHeadingTypes);
  }
}
