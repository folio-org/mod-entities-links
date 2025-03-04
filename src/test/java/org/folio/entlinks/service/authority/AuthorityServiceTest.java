package org.folio.entlinks.service.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.MatchUtils.authorityMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityIdentifier;
import org.folio.entlinks.domain.entity.AuthorityNote;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.HeadingRef;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityServiceTest {
  @Mock
  private AuthorityRepository repository;

  @InjectMocks
  private AuthorityService service;

  @Test
  void shouldGetAllAuthoritiesByOffsetAndLimit() {
    var expected = new PageImpl<>(List.of(new Authority()));
    when(repository.findAllByDeletedFalse(any(Pageable.class))).thenReturn(expected);

    var result = service.getAll(0, 10, null);

    assertThat(result).isEqualTo(expected);
    verify(repository).findAllByDeletedFalse(any(Pageable.class));
  }

  @Test
  void shouldGetAllAuthoritiesByCqlQuery() {
    var expected = new PageImpl<>(List.of(new Authority()));
    when(repository.findByCql(any(String.class), any(Pageable.class))).thenReturn(expected);

    var result = service.getAll(0, 10, "some_query_string");

    assertThat(result).isEqualTo(expected);
    verify(repository).findByCql(any(String.class), any(Pageable.class));
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
    var authority = new Authority();
    authority.setId(id);
    when(repository.findAllByIdInAndDeletedFalse(anyList())).thenReturn(List.of(authority));

    var allGroupedByIds = service.getAllByIds(List.of(id));

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
    UUID id = UUID.randomUUID();

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

    when(repository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(existed));
    when(repository.saveAndFlush(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));
    var updated = service.update(modified);

    assertThat(updated).isEqualTo(modified);
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

    var updated = service.update(modified);

    assertThat(updated.getAuthoritySourceFile()).isNull();
    verify(repository).findByIdAndDeletedFalse(id);
    verify(repository).saveAndFlush(existed);
    verifyNoMoreInteractions(repository);
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

    var thrown = assertThrows(OptimisticLockingException.class, () -> service.update(modified));

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

    service.deleteById(UUID.randomUUID());

    verify(repository).findByIdAndDeletedFalse(any(UUID.class));
    verify(repository).save(any(Authority.class));
  }

  @Test
  void shouldThrowExceptionWhenNoEntityExistsToDelete() {
    var id = UUID.randomUUID();
    when(repository.findByIdAndDeletedFalse(any(UUID.class))).thenReturn(Optional.empty());

    var thrown = assertThrows(AuthorityNotFoundException.class, () -> service.deleteById(id));

    assertThat(thrown.getMessage()).containsOnlyOnce(id.toString());
    verify(repository).findByIdAndDeletedFalse(any(UUID.class));
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldHardDeleteAuthoritiesByIds() {
    service.deleteByIds(List.of(UUID.randomUUID()));

    verify(repository).deleteAllByIdInBatch(anyIterable());
  }
}
