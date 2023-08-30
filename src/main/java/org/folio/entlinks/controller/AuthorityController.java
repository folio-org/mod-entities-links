package org.folio.entlinks.controller;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.folio.entlinks.controller.delegate.AuthorityServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityDtoCollection;
import org.folio.entlinks.rest.resource.AuthorityStorageApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@AllArgsConstructor
public class AuthorityController implements AuthorityStorageApi {

  private final AuthorityServiceDelegate delegate;

  @Override
  public ResponseEntity<AuthorityDto> createAuthority(AuthorityDto authority) {
    var created = delegate.createAuthority(authority);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<Void> deleteAuthority(UUID id) {
    delegate.deleteAuthorityById(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<AuthorityDto> getAuthority(UUID id) {
    var authority = delegate.getAuthorityById(id);
    return ResponseEntity.ok(authority);
  }

  @Override
  public ResponseEntity<AuthorityDtoCollection> retrieveAuthorities(Integer offset, Integer limit, String query) {
    return ResponseEntity.ok(delegate.retrieveAuthorityCollection(offset, limit, query));
  }

  @Override
  public ResponseEntity<Void> updateAuthority(UUID id, AuthorityDto authority) {
    delegate.updateAuthority(id, authority);
    return ResponseEntity.noContent().build();
  }
}
