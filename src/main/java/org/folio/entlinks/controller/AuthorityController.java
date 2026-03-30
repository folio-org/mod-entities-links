package org.folio.entlinks.controller;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.entlinks.controller.delegate.AuthorityServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityBulkRequest;
import org.folio.entlinks.domain.dto.AuthorityBulkResponse;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityFullDtoCollection;
import org.folio.entlinks.domain.dto.AuthorityIdDto;
import org.folio.entlinks.domain.dto.AuthorityIdDtoCollection;
import org.folio.entlinks.exception.AuthoritiesRequestNotSupportedMediaTypeException;
import org.folio.entlinks.rest.resource.AuthorityStorageApi;
import org.folio.tenant.domain.dto.Parameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@AllArgsConstructor
public class AuthorityController implements AuthorityStorageApi {

  public static final String RETRIEVE_COLLECTION_INVALID_ACCEPT_MESSAGE =
    "It is not allowed to retrieve authorities in text/plain format";

  private final AuthorityServiceDelegate delegate;

  @Override
  public ResponseEntity<AuthorityDto> createAuthority(AuthorityDto authority) {
    var created = delegate.createAuthority(authority);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  public ResponseEntity<AuthorityBulkResponse> createAuthorityBulk(AuthorityBulkRequest createRequest) {
    return ResponseEntity.ok(delegate.createAuthorities(createRequest));
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
  public ResponseEntity retrieveAuthorities(Boolean deleted, Boolean idOnly, Integer offset, Integer limit,
                                            String query, @RequestHeader(value = "Accept", required = false,
                                                                         defaultValue = "application/json")
                                            List<String> acceptingMediaTypes) {
    validateGetParams(idOnly, acceptingMediaTypes);
    var collectionDto = delegate.retrieveAuthorityCollection(offset, limit, query, idOnly, deleted);

    return getAuthoritiesCollectionResponse(collectionDto, acceptingMediaTypes, idOnly);
  }

  @Override
  public ResponseEntity<Void> updateAuthority(UUID id, AuthorityDto authority) {
    delegate.updateAuthority(id, authority);
    return ResponseEntity.noContent().build();
  }

  /**
   * POST /authority-storage/expire/authorities.
   *
   * @return Successfully published authorities expire job (status code 202)
   *   or Internal server error. (status code 500)
   */
  @PostMapping(
    value = "/authority-storage/expire/authorities",
    produces = {"application/json"}
  )
  public ResponseEntity<Void> expireAuthorities() {
    delegate.expire();
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  private ResponseEntity<Object> getAuthoritiesCollectionResponse(AuthorityFullDtoCollection collectionDto,
                                                                  List<String> acceptingMediaTypes,
                                                                  Boolean idOnly) {
    var headers = new HttpHeaders();
    if (Boolean.TRUE.equals(idOnly) && CollectionUtils.isNotEmpty(acceptingMediaTypes)
      && acceptingMediaTypes.contains(TEXT_PLAIN_VALUE)) {
      headers.setContentType(MediaType.TEXT_PLAIN);
      return new ResponseEntity<>(
        ((AuthorityIdDtoCollection) collectionDto).getAuthorities().stream()
          .map(AuthorityIdDto::getId)
          .map(UUID::toString)
          .collect(Collectors.joining(System.lineSeparator())),
        headers,
        HttpStatus.OK
      );
    }

    headers.setContentType(MediaType.APPLICATION_JSON);
    return new ResponseEntity<>(collectionDto, headers, HttpStatus.OK);
  }

  private void validateGetParams(Boolean idOnly, List<String> acceptingMediaTypes) {
    if (List.of(TEXT_PLAIN_VALUE).equals(acceptingMediaTypes) && Boolean.FALSE.equals(idOnly)) {
      throw new AuthoritiesRequestNotSupportedMediaTypeException(RETRIEVE_COLLECTION_INVALID_ACCEPT_MESSAGE,
        List.of(new Parameter("Accept").value(TEXT_PLAIN_VALUE), new Parameter("idOnly").value("false")));
    }
  }
}
