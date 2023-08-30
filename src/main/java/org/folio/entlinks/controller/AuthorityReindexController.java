package org.folio.entlinks.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.ReindexServiceDelegate;
import org.folio.entlinks.domain.dto.ReindexJobDto;
import org.folio.entlinks.domain.dto.ReindexJobDtoCollection;
import org.folio.entlinks.rest.resource.AuthorityStorageReindexApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class AuthorityReindexController implements AuthorityStorageReindexApi {

  private final ReindexServiceDelegate reindexServiceDelegate;

  @Override
  public ResponseEntity<ReindexJobDtoCollection> getReindexJobs(String query, Integer offset, Integer limit) {
    return ResponseEntity.ok(reindexServiceDelegate.retrieveReindexJobs(query, offset, limit));
  }

  @Override
  public ResponseEntity<ReindexJobDto> submitReindexJob() {
    return ResponseEntity.status(HttpStatus.CREATED).body(reindexServiceDelegate.startAuthoritiesReindex());
  }

  @Override
  public ResponseEntity<Void> deleteReindexJob(UUID id) {
    reindexServiceDelegate.deleteReindexJob(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ReindexJobDto> getReindexJob(UUID id) {
    return ResponseEntity.ok(reindexServiceDelegate.getReindexJobById(id));
  }
}
