package org.folio.entlinks.service.dataloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.entity.AuthorityNoteType;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.service.authority.AuthorityNoteTypeService;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ReferenceDataLoaderTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final AuthoritySourceFileMapper sourceFileMapper = Mappers.getMapper(AuthoritySourceFileMapper.class);

  private final AuthorityNoteTypeService noteTypeService = mock(AuthorityNoteTypeService.class);

  private final AuthoritySourceFileService sourceFileService = mock(AuthoritySourceFileService.class);

  private final ReferenceDataLoader referenceDataLoader =
      new ReferenceDataLoader(noteTypeService, sourceFileService, sourceFileMapper, OBJECT_MAPPER);

  @Test
  void shouldLoadReferenceData() {
    when(noteTypeService.findById(any(UUID.class))).thenReturn(null);
    when(noteTypeService.create(any(AuthorityNoteType.class))).thenAnswer(i -> i.getArguments()[0]);
    when(sourceFileService.findById(any(UUID.class))).thenReturn(null);
    when(sourceFileService.create(any(AuthoritySourceFile.class))).thenAnswer(i -> i.getArguments()[0]);

    referenceDataLoader.loadRefData();

    verify(noteTypeService).findById(any(UUID.class));
    verify(sourceFileService).findById(any(UUID.class));

    var noteTypeCaptor = ArgumentCaptor.forClass(AuthorityNoteType.class);
    var sourceFileCaptor = ArgumentCaptor.forClass(AuthoritySourceFile.class);
    verify(noteTypeService).create(noteTypeCaptor.capture());
    verify(sourceFileService).create(sourceFileCaptor.capture());

    var loadedNoteType = noteTypeCaptor.getValue();
    assertNotNull(loadedNoteType.getId());
    assertEquals("76c74801-afec-45a0-aad7-3ff23591e147", loadedNoteType.getId().toString());
    assertEquals("general note", loadedNoteType.getName());
    assertEquals("folio", loadedNoteType.getSource());

    var loadedSourceFile = sourceFileCaptor.getValue();
    assertNotNull(loadedSourceFile.getId());
    assertEquals("cb58492d-018e-442d-9ce3-35aabfc524aa", loadedSourceFile.getId().toString());
    assertEquals("Art & architecture thesaurus (AAT)", loadedSourceFile.getName());
    assertThat(loadedSourceFile.getAuthoritySourceFileCodes()).hasSize(1);
    assertEquals("aat", loadedSourceFile.getAuthoritySourceFileCodes().iterator().next().getCode());
    assertEquals("Subjects", loadedSourceFile.getType());
    assertEquals("vocab.getty.edu/aat/", loadedSourceFile.getBaseUrl());
    assertEquals("FOLIO", loadedSourceFile.getSource().name());
  }

  @Test
  void shouldHandleExceptionInLoadRefData() {
    when(noteTypeService.findById(any(UUID.class))).thenThrow(new RuntimeException("Unable to load reference data"));

    var exception = Assertions.assertThrows(IllegalStateException.class, referenceDataLoader::loadRefData);

    assertEquals("Unable to load reference data", exception.getMessage());
  }
}
