package org.folio.entlinks.service.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthoritySourceFileServiceTest {

  @Mock
  private AuthoritySourceFileRepository repository;

  @Mock
  private AuthoritySourceFileMapper mapper;

  @InjectMocks
  private AuthoritySourceFileService service;

  @Test
  void shouldGetAllAuthoritySourceFilesByOffsetAndLimit() {
    var expected = new PageImpl<>(List.of(new AuthoritySourceFile()));
    when(repository.findAll(any(Pageable.class))).thenReturn(expected);

    var result = service.getAll(0, 10, null);

    assertThat(result).isEqualTo(expected);
    verify(repository).findAll(any(Pageable.class));
  }

  @Test
  void shouldGetAllAuthoritySourceFilesByCqlQuery() {
    var expected = new PageImpl<>(List.of(new AuthoritySourceFile()));
    when(repository.findByCql(any(String.class), any(Pageable.class))).thenReturn(expected);

    var result = service.getAll(0, 10, "some_query_string");

    assertThat(result).isEqualTo(expected);
    verify(repository).findByCql(any(String.class), any(Pageable.class));
  }

  @Test
  void shouldGetAuthoritySourceFileById() {
    var expected = new AuthoritySourceFile();
    when(repository.findById(any(UUID.class))).thenReturn(Optional.of(expected));

    var result = service.getById(UUID.randomUUID());

    assertThat(result).isEqualTo(expected);
    verify(repository).findById(any(UUID.class));
  }

  @Test
  void shouldThrowExceptionWhenNoAuthoritySourceFileExistById() {
    when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());
    var id = UUID.randomUUID();

    assertThrows(AuthoritySourceFileNotFoundException.class, () -> service.getById(id));
    verify(repository).findById(any(UUID.class));
  }

  @Test
  void shouldCreateAuthoritySourceFile() {
    var expected = new AuthoritySourceFile();
    when(repository.save(any(AuthoritySourceFile.class))).thenReturn(expected);

    var created = service.create(new AuthoritySourceFile());

    assertThat(created).isEqualTo(expected);
    verify(repository).save(any(AuthoritySourceFile.class));
  }

  @Test
  void shouldThrowExceptionWhileCreatingWhenEntityAlreadyExists() {
    var entity = new AuthoritySourceFile();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    when(repository.existsById(id)).thenReturn(true);

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.create(entity));

    assertThat(thrown.getInvalidParameters()).hasSize(1);
    assertThat(thrown.getInvalidParameters().get(0).getKey()).isEqualTo("id");
    assertThat(thrown.getInvalidParameters().get(0).getValue()).isEqualTo(id.toString());
  }

  @Test
  void shouldUpdateAuthoritySourceFile() {
    var entity = new AuthoritySourceFile();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setName("updated name");
    entity.setSource("updated source");
    var expected = new AuthoritySourceFile();
    expected.setId(id);

    when(repository.findById(id)).thenReturn(Optional.of(expected));
    when(repository.save(expected)).thenReturn(expected);
    when(mapper.toDtoCodes(entity.getAuthoritySourceFileCodes())).thenReturn(List.of());
    when(mapper.toDtoCodes(expected.getAuthoritySourceFileCodes())).thenReturn(List.of());

    var updated = service.update(id, entity);

    assertThat(updated).isEqualTo(expected);
    verify(repository).findById(id);
    verify(repository).save(expected);
  }

  @Test
  void shouldThrowExceptionEntityIdDiffersFromProvidedId() {
    var entity = new AuthoritySourceFile();
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
  void shouldDeleteAuthoritySourceFile() {
    when(repository.existsById(any(UUID.class))).thenReturn(true);
    doNothing().when(repository).deleteById(any(UUID.class));

    service.deleteById(UUID.randomUUID());

    verify(repository).existsById(any(UUID.class));
    verify(repository).deleteById(any(UUID.class));
  }

  @Test
  void shouldThrowExceptionWhenNoEntityExistsToDelete() {
    var id = UUID.randomUUID();
    when(repository.existsById(any(UUID.class))).thenReturn(false);

    var thrown = assertThrows(AuthoritySourceFileNotFoundException.class, () -> service.deleteById(id));

    assertThat(thrown.getMessage()).containsOnlyOnce(id.toString());
  }
}
