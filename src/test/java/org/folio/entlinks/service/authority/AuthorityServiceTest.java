package org.folio.entlinks.service.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.test.type.UnitTest;
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

  @Mock
  private AuthoritySourceFileRepository sourceFileRepository;

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
    when(repository.findByCqlAndDeletedFalse(any(String.class), any(Pageable.class))).thenReturn(expected);

    var result = service.getAll(0, 10, "some_query_string");

    assertThat(result).isEqualTo(expected);
    verify(repository).findByCqlAndDeletedFalse(any(String.class), any(Pageable.class));
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
    var authority = new Authority();
    authority.setId(UUID.randomUUID());
    when(repository.findAllByIdInAndDeletedFalse(anyList())).thenReturn(List.of(authority));

    var allGroupedByIds = service.getAllByIds(List.of(authority.getId()));

    assertThat(allGroupedByIds).isEqualTo(Map.of(authority.getId(), authority));
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
    when(sourceFileRepository.existsById(any(UUID.class))).thenReturn(true);
    var argumentCaptor = ArgumentCaptor.forClass(Authority.class);

    var created = service.create(newEntity);

    assertThat(created).isEqualTo(expected);
    verify(sourceFileRepository).existsById(any(UUID.class));
    verify(repository).save(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getId()).isNotNull();
  }

  @Test
  void shouldThrowExceptionIfSourceFileDoesNotExist() {
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setId(UUID.randomUUID());
    var newEntity = new Authority();
    newEntity.setAuthoritySourceFile(sourceFile);
    when(sourceFileRepository.existsById(any(UUID.class))).thenReturn(false);

    assertThrows(AuthoritySourceFileNotFoundException.class, () -> service.create(newEntity));

    verify(sourceFileRepository).existsById(any(UUID.class));
    verifyNoInteractions(repository);
  }

  @Test
  void shouldUpdateAuthorityStorage() {
    var entity = new Authority();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setHeading("updated heading");
    entity.setSource("updated source");
    var expected = new Authority();
    expected.setId(id);
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setId(UUID.randomUUID());
    entity.setAuthoritySourceFile(sourceFile);

    when(repository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(expected));
    when(sourceFileRepository.existsById(any(UUID.class))).thenReturn(true);
    when(repository.save(expected)).thenReturn(expected);

    var updated = service.update(id, entity);

    assertThat(updated).isEqualTo(expected);
    assertThat(updated.getAuthoritySourceFile()).isEqualTo(sourceFile);
    verify(repository).findByIdAndDeletedFalse(id);
    verify(sourceFileRepository).existsById(any(UUID.class));
    verify(repository).save(expected);
    verifyNoMoreInteractions(repository);
    verifyNoMoreInteractions(sourceFileRepository);
  }

  @Test
  void shouldThrowExceptionIfEntityIdDiffersFromProvidedId() {
    var entity = new Authority();
    UUID id = UUID.randomUUID();
    UUID differentId = UUID.randomUUID();
    entity.setId(id);

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.update(differentId, entity));

    assertThat(thrown.getInvalidParameters()).hasSize(1);
    assertThat(thrown.getInvalidParameters().get(0).getKey()).isEqualTo("id");
    assertThat(thrown.getInvalidParameters().get(0).getValue()).isEqualTo(id.toString());
    verifyNoInteractions(repository);
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
}
