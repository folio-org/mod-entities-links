package org.folio.entlinks.service.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.MatchUtils.authorityMatch;
import static org.folio.support.base.TestConstants.CENTRAL_TENANT_ID;
import static org.folio.support.base.TestConstants.COLLEGE_TENANT_ID;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityIdentifier;
import org.folio.entlinks.domain.entity.AuthorityNote;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.HeadingRef;
import org.folio.entlinks.domain.repository.AuthorityJdbcRepository;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.entlinks.service.consortium.UserTenantsService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityServiceTest {
  private static final String CQL_QUERY = "some_query_string";

  @Mock
  private AuthorityRepository repository;
  @Mock
  private AuthorityJdbcRepository jdbcRepository;
  @Mock
  private AuthoritySourceFileRepository sourceFileRepository;
  @Mock
  private UserTenantsService userTenantsService;
  @Mock
  private FolioExecutionContext folioExecutionContext;

  @InjectMocks
  private AuthorityService service;

  @Test
  void shouldGetAllAuthoritiesByOffsetAndLimit() {
    var expected = new PageImpl<>(List.of(new Authority()));
    var pageable = new OffsetRequest(0, 10);
    when(repository.findAllByDeletedFalse(pageable)).thenReturn(expected);

    var result = service.getAll(0, 10, null);

    assertThat(result).isEqualTo(expected);
    verify(repository).findAllByDeletedFalse(pageable);
  }

  @Test
  void shouldGetAllAuthoritiesByCqlQuery() {
    var expected = new PageImpl<>(List.of(new Authority()));
    var pageable = new OffsetRequest(0, 10);
    when(repository.findByCql(CQL_QUERY, pageable)).thenReturn(expected);

    var result = service.getAll(0, 10, CQL_QUERY);

    assertThat(result).isEqualTo(expected);
    verify(repository).findByCql(CQL_QUERY, pageable);
  }

  @Test
  void shouldGetAuthorityStorageById() {
    var expected = new Authority();
    when(repository.findByIdAndDeletedFalse(any(UUID.class))).thenReturn(Optional.of(expected));

    var result = service.getById(UUID.randomUUID());

    assertThat(result).isEqualTo(expected);
    verify(repository).findByIdAndDeletedFalse(any(UUID.class));
  }

  @Test
  void shouldGetAllAuthoritiesByIds() {
    var id = UUID.randomUUID();
    var ids = List.of(id);
    var authority = new Authority();
    authority.setId(id);
    when(repository.findAllByIdInAndDeletedFalse(ids)).thenReturn(List.of(authority));

    var allGroupedByIds = service.getAllByIds(ids);

    assertThat(allGroupedByIds).isEqualTo(Map.of(id, authority));
  }

  @Test
  void shouldThrowExceptionWhenNoAuthorityStorageExistById() {
    when(repository.findByIdAndDeletedFalse(any(UUID.class))).thenReturn(Optional.empty());
    var id = UUID.randomUUID();

    assertThrows(AuthorityNotFoundException.class, () -> service.getById(id));
    verify(repository).findByIdAndDeletedFalse(any(UUID.class));
  }

  @Test
  void shouldCreateAuthorityStorage() {
    var expected = new Authority();
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setId(UUID.randomUUID());
    expected.setAuthoritySourceFile(sourceFile);
    var newEntity = new Authority();
    newEntity.setAuthoritySourceFile(sourceFile);

    when(repository.save(any(Authority.class))).thenReturn(expected);
    var argumentCaptor = ArgumentCaptor.forClass(Authority.class);

    var created = service.create(newEntity);

    assertThat(created).isEqualTo(expected);
    verify(repository).save(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getId()).isNotNull();
  }

  @Test
  void shouldUpdateAuthority() {
    var id = UUID.randomUUID();
    var existed = getExistedAuthority(id);
    var modified = getModifiedAuthority(id);

    when(repository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(existed));
    when(sourceFileRepository.existsById(any(UUID.class))).thenReturn(true);
    when(repository.saveAndFlush(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));
    var updated = service.update(modified, false);

    assertThat(updated.oldEntity()).isEqualTo(existed);
    assertThat(updated.newEntity()).isEqualTo(modified);
    verify(repository).findByIdAndDeletedFalse(id);
    verify(repository).saveAndFlush(argThat(authorityMatch(modified)));
  }

  @Test
  void shouldUpdateAuthority_whenSourceFileIsNull() {
    UUID id = UUID.randomUUID();

    var existed = new Authority();
    existed.setId(id);
    var sourceFileOld = new AuthoritySourceFile();
    sourceFileOld.setId(UUID.randomUUID());
    existed.setAuthoritySourceFile(sourceFileOld);

    var modified = new Authority();
    modified.setId(id);
    modified.setAuthoritySourceFile(null);

    when(repository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(existed));
    when(repository.saveAndFlush(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var updated = service.update(modified, false);

    assertThat(updated.newEntity().getAuthoritySourceFile()).isNull();
    verify(repository).findByIdAndDeletedFalse(id);
    verify(repository).saveAndFlush(existed);
    verifyNoMoreInteractions(repository);
    verifyNoInteractions(sourceFileRepository);
  }

  @Test
  void shouldThrowOptimisticLockingFailureExceptionWhenProvidedOldAuthorityVersion() {
    var id = UUID.randomUUID();
    var existing = new Authority();
    existing.setVersion(1);
    existing.setId(id);
    var modified = new Authority();
    modified.setId(id);

    when(repository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(existing));

    var thrown = assertThrows(OptimisticLockingException.class, () -> service.update(modified, false));

    assertThat(thrown.getMessage())
      .isEqualTo("Cannot update record " + id + " because it has been changed (optimistic locking): "
                 + "Stored _version is 1, _version of request is 0");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteAuthorityStorage() {
    var authority = new Authority();
    when(repository.findByIdAndDeletedFalse(any(UUID.class))).thenReturn(Optional.of(authority));
    when(repository.save(any(Authority.class))).thenReturn(authority);

    service.deleteById(UUID.randomUUID(), false);

    verify(repository).findByIdAndDeletedFalse(any(UUID.class));
    verify(repository).save(any(Authority.class));
  }

  @Test
  void shouldThrowExceptionWhenNoEntityExistsToDelete() {
    var id = UUID.randomUUID();
    when(repository.findByIdAndDeletedFalse(any(UUID.class))).thenReturn(Optional.empty());

    var thrown = assertThrows(AuthorityNotFoundException.class, () -> service.deleteById(id, false));

    assertThat(thrown.getMessage()).containsOnlyOnce(id.toString());
    verify(repository).findByIdAndDeletedFalse(any(UUID.class));
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldHardDeleteAuthoritiesByIds() {
    service.deleteByIds(List.of(UUID.randomUUID()));

    verify(repository).deleteAllByIdInBatch(anyIterable());
  }

  @Test
  void shouldGetAllAuthorityIdsByOffsetAndLimit() {
    var id = UUID.randomUUID();
    var expected = new PageImpl<>(List.of(id));
    var pageable = new OffsetRequest(0, 10);
    when(repository.findAllIdsByDeletedFalse(pageable)).thenReturn(expected);

    var result = service.getAllIds(0, 10, null);

    assertThat(result).isEqualTo(expected);
    verify(repository).findAllIdsByDeletedFalse(pageable);
  }

  @Test
  void shouldGetAllAuthorityIdsByCqlQuery() {
    var id = UUID.randomUUID();
    var expected = new PageImpl<>(List.of(id));
    var pageable = new OffsetRequest(0, 10);
    when(repository.findIdsByCql(CQL_QUERY, pageable)).thenReturn(expected);

    var result = service.getAllIds(0, 10, CQL_QUERY);

    assertThat(result).isEqualTo(expected);
    verify(repository).findIdsByCql(CQL_QUERY, pageable);
  }

  @Test
  void shouldUpsertAuthorities_withNewAndExistingRecords() {
    var existingId = UUID.randomUUID();
    final var newId = UUID.randomUUID();

    var existing = new Authority();
    existing.setId(existingId);
    existing.setVersion(0);
    existing.setHeading("old heading");

    var modified = new Authority();
    modified.setId(existingId);
    modified.setVersion(0);
    modified.setHeading("new heading");

    var newAuthority = new Authority();
    newAuthority.setId(newId);
    newAuthority.setHeading("brand new");

    var ids = List.of(existingId, newId);

    when(repository.findAllByIdInAndDeletedFalse(ids)).thenReturn(List.of(existing));
    when(repository.saveAll(anyIterable())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.upsert(List.of(modified, newAuthority));

    assertThat(result).hasSize(2);
    verify(repository).findAllByIdInAndDeletedFalse(ids);
    verify(repository).saveAll(anyIterable());
  }

  @Test
  void shouldUpsertAuthorities_throwOptimisticLockingException() {
    var id = UUID.randomUUID();
    var existing = new Authority();
    existing.setId(id);
    existing.setVersion(1);

    var modified = new Authority();
    modified.setId(id);
    modified.setVersion(0);

    var ids = List.of(id);

    when(repository.findAllByIdInAndDeletedFalse(ids)).thenReturn(List.of(existing));

    var modifiedList = List.of(modified);
    assertThrows(OptimisticLockingException.class, () -> service.upsert(modifiedList));
    verify(repository).findAllByIdInAndDeletedFalse(ids);
  }

  @Test
  void shouldDeleteByIdAndReturnDeletedAuthority() {
    var id = UUID.randomUUID();
    var authority = new Authority();
    authority.setId(id);
    when(repository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(authority));
    when(repository.save(any(Authority.class))).thenReturn(authority);

    var deleted = service.deleteById(id);

    assertThat(deleted).isEqualTo(authority);
    assertThat(deleted.isDeleted()).isTrue();
    verify(repository).findByIdAndDeletedFalse(id);
    verify(repository).save(authority);
  }

  @Test
  void shouldCheckAuthoritiesExist() {
    var existingId = UUID.randomUUID();
    var nonExistingId = UUID.randomUUID();
    var authorityIds = Set.of(existingId, nonExistingId);
    when(repository.findExistingIdsByIdsAndDeletedFalse(authorityIds)).thenReturn(List.of(existingId));

    var result = service.authoritiesExist(authorityIds);

    assertThat(result)
      .containsEntry(existingId, true)
      .containsEntry(nonExistingId, false);
    verify(repository).findExistingIdsByIdsAndDeletedFalse(authorityIds);
  }

  @Test
  void shouldCheckAuthoritiesExistForCentralTenant_whenOnMemberTenant() {
    var authorityId = UUID.randomUUID();
    var authorityIds = Set.of(authorityId);
    when(folioExecutionContext.getTenantId()).thenReturn(COLLEGE_TENANT_ID);
    when(userTenantsService.getCentralTenant(COLLEGE_TENANT_ID)).thenReturn(Optional.of(CENTRAL_TENANT_ID));
    when(jdbcRepository.findExistingIdsByIdsAndDeletedFalse(authorityIds, CENTRAL_TENANT_ID))
      .thenReturn(List.of(authorityId));

    var result = service.authoritiesExistForCentralIfOnMember(authorityIds);

    assertThat(result).containsEntry(authorityId, true);
    verify(jdbcRepository).findExistingIdsByIdsAndDeletedFalse(authorityIds, CENTRAL_TENANT_ID);
  }

  @Test
  void shouldReturnEmptyMap_whenOnCentralTenant() {
    when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT_ID);
    when(userTenantsService.getCentralTenant(CENTRAL_TENANT_ID)).thenReturn(Optional.of(CENTRAL_TENANT_ID));

    var result = service.authoritiesExistForCentralIfOnMember(Set.of(UUID.randomUUID()));

    assertThat(result).isEmpty();
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  void shouldReturnEmptyMap_whenNoCentralTenant() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(userTenantsService.getCentralTenant(TENANT_ID)).thenReturn(Optional.empty());

    var result = service.authoritiesExistForCentralIfOnMember(Set.of(UUID.randomUUID()));

    assertThat(result).isEmpty();
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  void shouldFindNaturalIdsByIdsForCentralTenant_whenOnMemberTenant() {
    var authorityId = UUID.randomUUID();
    var authorityIds = List.of(authorityId);
    var naturalId = "n123456";
    when(folioExecutionContext.getTenantId()).thenReturn(COLLEGE_TENANT_ID);
    when(userTenantsService.getCentralTenant(COLLEGE_TENANT_ID)).thenReturn(Optional.of(CENTRAL_TENANT_ID));
    when(jdbcRepository.findAuthorityNaturalIdsByIdsAndDeletedFalse(authorityIds, CENTRAL_TENANT_ID))
      .thenReturn(Map.of(authorityId, naturalId));

    var result = service.findNaturalIdsByIdInAndDeletedFalseForCentralIfOnMember(authorityIds);

    assertThat(result).containsEntry(authorityId, naturalId);
    verify(jdbcRepository).findAuthorityNaturalIdsByIdsAndDeletedFalse(authorityIds, CENTRAL_TENANT_ID);
  }

  @Test
  void shouldReturnEmptyMapForNaturalIds_whenOnCentralTenant() {
    when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT_ID);
    when(userTenantsService.getCentralTenant(CENTRAL_TENANT_ID)).thenReturn(Optional.of(CENTRAL_TENANT_ID));

    var result = service.findNaturalIdsByIdInAndDeletedFalseForCentralIfOnMember(List.of(UUID.randomUUID()));

    assertThat(result).isEmpty();
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  void shouldReturnEmptyMapForNaturalIds_whenNoCentralTenant() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(userTenantsService.getCentralTenant(TENANT_ID)).thenReturn(Optional.empty());

    var result = service.findNaturalIdsByIdInAndDeletedFalseForCentralIfOnMember(List.of(UUID.randomUUID()));

    assertThat(result).isEmpty();
    verifyNoInteractions(jdbcRepository);
  }

  @Test
  void shouldThrowAuthoritySourceFileNotFoundException_whenSourceFileDoesNotExist() {
    var id = UUID.randomUUID();
    final var sourceFileId = UUID.randomUUID();

    var existing = new Authority();
    existing.setId(id);
    existing.setVersion(0);

    var modified = new Authority();
    modified.setId(id);
    modified.setVersion(0);
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setId(sourceFileId);
    modified.setAuthoritySourceFile(sourceFile);

    when(repository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(existing));
    when(sourceFileRepository.existsById(sourceFileId)).thenReturn(false);

    assertThrows(AuthoritySourceFileNotFoundException.class, () -> service.update(modified, false));
    verify(repository).findByIdAndDeletedFalse(id);
    verify(sourceFileRepository).existsById(sourceFileId);
    verifyNoMoreInteractions(repository);
  }

  private Authority getModifiedAuthority(UUID id) {
    var modified = new Authority();
    modified.setId(id);
    modified.setHeading("new heading");
    modified.setHeadingType("personalNameNew");
    modified.setSource("MARCNEW");
    modified.setNaturalId("naturalNew");
    modified.setVersion(1);
    modified.setSaftHeadings(List.of(new HeadingRef("personalNameNew", "saftNew")));
    modified.setSftHeadings(List.of(new HeadingRef("personalNameNew", "sftNew")));
    modified.setNotes(List.of(new AuthorityNote(UUID.randomUUID(), "noteNew", true)));
    modified.setIdentifiers(List.of(new AuthorityIdentifier("identifierNew", UUID.randomUUID())));
    var sourceFileNew = new AuthoritySourceFile();
    sourceFileNew.setId(UUID.randomUUID());
    modified.setAuthoritySourceFile(sourceFileNew);
    return modified;
  }

  private Authority getExistedAuthority(UUID id) {
    var existed = new Authority();
    existed.setId(id);
    existed.setHeading("heading");
    existed.setHeadingType("personalName");
    existed.setSource("MARC");
    existed.setNaturalId("natural");
    existed.setVersion(0);
    existed.setSaftHeadings(List.of(new HeadingRef("personalName", "saft")));
    existed.setSftHeadings(List.of(new HeadingRef("personalName", "sft")));
    existed.setNotes(List.of(new AuthorityNote(UUID.randomUUID(), "note", true)));
    existed.setIdentifiers(List.of(new AuthorityIdentifier("identifier", UUID.randomUUID())));
    var sourceFileOld = new AuthoritySourceFile();
    sourceFileOld.setId(UUID.randomUUID());
    existed.setAuthoritySourceFile(sourceFileOld);
    return existed;
  }
}
