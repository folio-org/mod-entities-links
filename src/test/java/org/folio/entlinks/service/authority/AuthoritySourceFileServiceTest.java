package org.folio.entlinks.service.authority;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.entity.AuthoritySourceFileSource.LOCAL;
import static org.folio.support.MatchUtils.authoritySourceFileMatch;
import static org.folio.support.TestDataUtils.AuthorityTestData.authoritySourceFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.entity.AuthoritySourceFileSource;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileJdbcRepository;
import org.folio.entlinks.domain.repository.AuthoritySourceFileRepository;
import org.folio.entlinks.exception.AuthoritySourceFileHridException;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthoritySourceFileServiceTest {

  @Mock
  private AuthoritySourceFileRepository repository;
  @Mock
  private AuthoritySourceFileJdbcRepository jdbcRepository;
  @Mock
  private AuthorityRepository authorityRepository;

  @Mock
  private AuthoritySourceFileMapper mapper;
  @Mock
  private JdbcTemplate jdbcTemplate;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private FolioModuleMetadata moduleMetadata;

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
  void shouldFindAuthoritySourceFileById() {
    var entity = new AuthoritySourceFile();
    var id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    var found = service.findById(id);

    assertEquals(entity, found);
    verify(repository).findById(id);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindAuthoritySourceFileForNullId() {
    var notFound = service.findById(null);

    assertNull(notFound);
    verifyNoInteractions(repository);
  }

  @Test
  void shouldFindAuthoritySourceFileByName() {
    var entity = new AuthoritySourceFile();
    var typeName = "type_name";
    when(repository.findByName(typeName)).thenReturn(Optional.of(entity));

    var found = service.findByName(typeName);

    assertEquals(entity, found);
    verify(repository).findByName(typeName);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindAuthoritySourceFileForNullName() {
    var notFound = service.findByName(null);

    assertNull(notFound);
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"LOCAL", "FOLIO"})
  void shouldCreateAuthoritySourceFile(String source) {
    var code = new AuthoritySourceFileCode();
    code.setCode("code");
    var entity = new AuthoritySourceFile();
    entity.setAuthoritySourceFileCodes(Set.of(code));
    entity.setSource(AuthoritySourceFileSource.valueOf(source));
    var expected = new AuthoritySourceFile(entity);
    when(repository.save(any(AuthoritySourceFile.class))).thenAnswer(i -> i.getArguments()[0]);

    var created = service.create(entity);

    expected.setId(created.getId());
    assertThat(created).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "123", "#", "abc123", "abc def", "abc,def"})
  void shouldNotBePossibleToCreateAuthoritySourceFileWithInvalidCode(String code) {
    var sourceFileCode = new AuthoritySourceFileCode();
    sourceFileCode.setCode(code);
    var entity = new AuthoritySourceFile();
    entity.setAuthoritySourceFileCodes(Set.of(sourceFileCode));
    entity.setSource(LOCAL);

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.create(entity));

    verifyNoInteractions(repository);
    assertThat(thrown.getInvalidParameters()).hasSize(1);
    assertThat(thrown.getInvalidParameters().getFirst().getKey()).isEqualTo("code");
    assertThat(thrown.getInvalidParameters().getFirst().getValue()).isEqualTo(code);
  }

  @ValueSource(ints = 0)
  @ParameterizedTest
  void shouldUpdateAuthoritySourceFileModifiableFields(Integer existingHridStartNumber) {
    var existing = authoritySourceFile(0);
    existing.setHridStartNumber(existingHridStartNumber);
    var id = existing.getId();
    var modified = authoritySourceFile(1);
    modified.setId(id);
    modified.setSource(LOCAL);

    var expected = new AuthoritySourceFile(modified);
    expected.setSource(existing.getSource());
    expected.setSequenceName(existing.getSequenceName());
    var existingDtoCodes = existing.getAuthoritySourceFileCodes().stream()
        .map(AuthoritySourceFileCode::getCode).toList();
    var modifiedDtoCodes = modified.getAuthoritySourceFileCodes().stream()
        .map(AuthoritySourceFileCode::getCode).toList();

    when(repository.findById(id)).thenReturn(Optional.of(existing));
    when(repository.saveAndFlush(expected)).thenReturn(expected);
    when(mapper.toDtoCodes(existing.getAuthoritySourceFileCodes())).thenReturn(existingDtoCodes);
    when(mapper.toDtoCodes(modified.getAuthoritySourceFileCodes())).thenReturn(modifiedDtoCodes);

    var actual = service.update(id, modified);

    assertThat(actual).isEqualTo(expected);
    verify(repository).findById(id);
    verify(repository).saveAndFlush(argThat(authoritySourceFileMatch(expected)));
    verify(jdbcRepository).createSequence(existing.getSequenceName(), modified.getHridStartNumber());
    verify(jdbcRepository).dropSequence(existing.getSequenceName());
  }

  @ValueSource(ints = 1)
  @ParameterizedTest
  void shouldUpdateAuthoritySourceFile_WhenSequenceStartNumberLessThenExisting(Integer existingHridStartNumber) {
    var existing = authoritySourceFile(0);
    existing.setHridStartNumber(existingHridStartNumber);
    var id = existing.getId();
    var modified = authoritySourceFile(1);
    modified.setId(id);
    modified.setSource(LOCAL);
    modified.setHridStartNumber(0);

    var expected = new AuthoritySourceFile(modified);
    expected.setSource(existing.getSource());
    expected.setSequenceName(existing.getSequenceName());
    var existingDtoCodes = existing.getAuthoritySourceFileCodes().stream()
        .map(AuthoritySourceFileCode::getCode).toList();
    var modifiedDtoCodes = modified.getAuthoritySourceFileCodes().stream()
        .map(AuthoritySourceFileCode::getCode).toList();

    when(repository.findById(id)).thenReturn(Optional.of(existing));
    when(repository.saveAndFlush(expected)).thenReturn(expected);
    when(mapper.toDtoCodes(existing.getAuthoritySourceFileCodes())).thenReturn(existingDtoCodes);
    when(mapper.toDtoCodes(modified.getAuthoritySourceFileCodes())).thenReturn(modifiedDtoCodes);

    var actual = service.update(id, modified);

    assertThat(actual).isEqualTo(expected);
    verify(repository).findById(id);
    verify(repository).saveAndFlush(argThat(authoritySourceFileMatch(expected)));

    verify(jdbcRepository).createSequence(existing.getSequenceName(), modified.getHridStartNumber());
    verify(jdbcRepository).dropSequence(existing.getSequenceName());
  }

  @Test
  void shouldUpdateAuthoritySource_NotUpdatingSequenceStartNumberWhenNotChanged() {
    var existing = authoritySourceFile(0);
    var id = existing.getId();
    var modified = new AuthoritySourceFile(existing);
    modified.setType("test");

    var expected = new AuthoritySourceFile(modified);

    when(repository.findById(id)).thenReturn(Optional.of(existing));
    when(repository.saveAndFlush(expected)).thenReturn(expected);
    when(mapper.toDtoCodes(existing.getAuthoritySourceFileCodes())).thenReturn(emptyList());
    when(mapper.toDtoCodes(modified.getAuthoritySourceFileCodes())).thenReturn(emptyList());

    var actual = service.update(id, modified);

    assertThat(actual).isEqualTo(expected);
    verify(repository).findById(id);
    verify(repository).saveAndFlush(expected);
    verifyNoInteractions(context);
    verifyNoInteractions(moduleMetadata);
    verifyNoInteractions(jdbcTemplate);
  }

  @Test
  void shouldThrowExceptionEntityIdDiffersFromProvidedId() {
    var entity = new AuthoritySourceFile();
    UUID id = UUID.randomUUID();
    UUID differentId = UUID.randomUUID();
    entity.setId(id);

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.update(differentId, entity));

    assertThat(thrown.getInvalidParameters()).hasSize(1);
    assertThat(thrown.getInvalidParameters().getFirst().getKey()).isEqualTo("id");
    assertThat(thrown.getInvalidParameters().getFirst().getValue()).isEqualTo(id.toString());
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "123", "#", "abc123", "abc def", "abc,def"})
  void shouldThrowExceptionWhenProvidedInvalidSourceFileCode(String code) {
    var entity = new AuthoritySourceFile();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    var sourceFileCode = new AuthoritySourceFileCode();
    sourceFileCode.setCode(code);
    entity.addCode(sourceFileCode);

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.update(id, entity));

    assertThat(thrown.getInvalidParameters()).hasSize(1);
    assertThat(thrown.getInvalidParameters().getFirst().getKey()).isEqualTo("code");
    assertThat(thrown.getInvalidParameters().getFirst().getValue())
        .isEqualTo(sourceFileCode.getCode());
    assertThat(thrown.getMessage())
        .isEqualTo("Authority Source File prefix should be non-empty sequence of letters");
    verifyNoInteractions(repository);
  }

  @Test
  void shouldDeleteAuthoritySourceFile() {
    var id = UUID.randomUUID();
    var authoritySourceFile = new AuthoritySourceFile();
    authoritySourceFile.setId(id);
    authoritySourceFile.setType("non-folio");

    when(repository.findById(any(UUID.class))).thenReturn(Optional.of(authoritySourceFile));
    doNothing().when(repository).deleteById(any(UUID.class));

    service.deleteById(UUID.randomUUID());

    verify(repository).findById(any(UUID.class));
    verify(repository).deleteById(any(UUID.class));
  }

  @Test
  void shouldThrowExceptionWhenNoEntityExistsToDelete() {
    var id = UUID.randomUUID();

    when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());

    var thrown = assertThrows(AuthoritySourceFileNotFoundException.class, () -> service.deleteById(id));

    assertThat(thrown.getMessage()).containsOnlyOnce(id.toString());
    verify(repository, never()).deleteById(any(UUID.class));
  }

  @Test
  void shouldNotDeleteWhenFolioType() {
    UUID id = UUID.randomUUID();
    var authoritySourceFile = new AuthoritySourceFile();
    authoritySourceFile.setId(id);
    authoritySourceFile.setSource(AuthoritySourceFileSource.FOLIO);

    when(repository.findById(id)).thenReturn(Optional.of(authoritySourceFile));

    var thrown = assertThrows(RequestBodyValidationException.class, () -> service.deleteById(id));

    assertThat(thrown.getMessage()).isEqualTo("Cannot delete Authority source file with source 'folio'");
    verify(repository).findById(id);
    verify(repository, never()).deleteById(any(UUID.class));
  }

  @Test
  void nextHrid_SuccessfulExecution_ReturnsNextHrid() {
    // Arrange
    final var id = UUID.randomUUID();
    var code = new AuthoritySourceFileCode();
    code.setCode("CODE");
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setSequenceName("sequenceName");
    sourceFile.setAuthoritySourceFileCodes(Collections.singleton(code));

    when(repository.getNextSequenceNumber("sequenceName")).thenReturn(10L);
    when(repository.findById(id)).thenReturn(Optional.of(sourceFile));

    // Act
    String nextHrid = service.nextHrid(id);

    // Assert
    assertEquals("CODE10", nextHrid);
  }

  @Test
  void nextHrid_BlankSequenceName_ThrowsException() {
    // Arrange
    final var id = UUID.randomUUID();
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setAuthoritySourceFileCodes(Collections.singleton(new AuthoritySourceFileCode()));

    when(repository.findById(id)).thenReturn(Optional.of(sourceFile));

    // Act & Assert
    assertThrows(AuthoritySourceFileHridException.class, () -> service.nextHrid(id));
  }

  @Test
  void nextHrid_MultipleCodes_ThrowsException() {
    // Arrange
    final var id = UUID.randomUUID();
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setSequenceName("sequenceName");
    sourceFile.setAuthoritySourceFileCodes(Set.of(new AuthoritySourceFileCode(), new AuthoritySourceFileCode()));

    when(repository.findById(id)).thenReturn(Optional.of(sourceFile));

    // Act & Assert
    assertThrows(AuthoritySourceFileHridException.class, () -> service.nextHrid(id));
  }

  @Test
  void nextHrid_DataAccessException_ThrowsException() {
    // Arrange
    final var id = UUID.randomUUID();
    var code = new AuthoritySourceFileCode();
    code.setCode("CODE");
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setSequenceName("sequenceName");
    sourceFile.setAuthoritySourceFileCodes(Collections.singleton(code));

    when(repository.findById(id)).thenReturn(Optional.of(sourceFile));
    when(repository.getNextSequenceNumber("sequenceName")).thenThrow(BadSqlGrammarException.class);

    // Act & Assert
    assertThrows(AuthoritySourceFileHridException.class, () -> service.nextHrid(id));
  }

  @Test
  void shouldCheckAuthoritiesExistForSourceFile() {
    var id = UUID.randomUUID();
    var expected = true;
    when(authorityRepository.existsAuthorityByAuthoritySourceFileId(id)).thenReturn(expected);

    var actual = service.authoritiesExistForSourceFile(id);

    assertThat(actual).isEqualTo(expected);
    verify(authorityRepository).existsAuthorityByAuthoritySourceFileId(id);
  }

  @Test
  void shouldCheckAuthoritiesExistForSourceFileAndTenant() {
    var id = UUID.randomUUID();
    var expected = true;
    var tenant = "tenant";
    var schema = "schema";
    when(context.getFolioModuleMetadata()).thenReturn(moduleMetadata);
    when(moduleMetadata.getDBSchemaName(tenant)).thenReturn(schema);
    when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(true);
    var captor = ArgumentCaptor.forClass(String.class);

    var actual = service.authoritiesExistForSourceFile(id, tenant, "authority");

    assertThat(actual).isEqualTo(expected);
    verify(jdbcTemplate).queryForObject(captor.capture(), eq(Boolean.class));
    assertThat("select exists (select true from %s.authority a where a.source_file_id='%s' limit 1)"
        .formatted(schema, id.toString())).isEqualTo(captor.getValue());
  }

  @Test
  void shouldThrowOptimisticLockingExceptionWhenProvidedOldAuthoritySourceFileVersion() {
    var id = UUID.randomUUID();
    var existing = new AuthoritySourceFile();
    existing.setVersion(1);
    existing.setId(id);
    var modified = new AuthoritySourceFile();
    modified.setId(id);

    when(repository.findById(id)).thenReturn(Optional.of(existing));

    var thrown = assertThrows(OptimisticLockingException.class, () -> service.update(id, modified));

    assertThat(thrown.getMessage())
        .isEqualTo("Cannot update record " + id + " because it has been changed (optimistic locking): "
            + "Stored _version is 1, _version of request is 0");
    verifyNoMoreInteractions(repository);
  }
}
