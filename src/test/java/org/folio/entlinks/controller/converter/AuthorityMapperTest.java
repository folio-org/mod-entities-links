package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;
import static org.folio.support.base.TestConstants.SOURCE_FILE_NATURAL_ID;
import static org.folio.support.base.TestConstants.SOURCE_FILE_SOURCE;
import static org.folio.support.base.TestConstants.TEST_DATE;
import static org.folio.support.base.TestConstants.TEST_ID;
import static org.folio.support.base.TestConstants.TEST_PROPERTY_VALUE;
import static org.folio.support.base.TestConstants.TEST_VERSION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityDtoCollection;
import org.folio.entlinks.domain.dto.AuthorityDtoIdentifier;
import org.folio.entlinks.domain.dto.AuthorityDtoNote;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.entity.AuthorityBase;
import org.folio.entlinks.domain.entity.AuthorityIdentifier;
import org.folio.entlinks.domain.entity.AuthorityNote;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.spring.testing.type.UnitTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

@UnitTest
class AuthorityMapperTest {

  private final AuthorityMapper authorityMapper = new AuthorityMapperImpl();

  @Test
  void testToEntityWithValidDto() {
    AuthorityDto dto = createAuthorityDto();

    Authority authority = authorityMapper.toEntity(dto);
    var mappedSubjectHeadingCode = dto.getSubjectHeadings().charAt(0);

    assertThat(authority).isNotNull();
    assertThat(dto.getId()).isEqualTo(authority.getId());
    assertThat(dto.getNaturalId()).isEqualTo(authority.getNaturalId());
    assertThat(dto.getSource()).isEqualTo(authority.getSource());
    assertThat(dto.getVersion()).isEqualTo(authority.getVersion());
    assertThat(mappedSubjectHeadingCode).isEqualTo(authority.getSubjectHeadingCode());
    assertThat(dto.getIdentifiers().get(0).getValue()).isEqualTo(authority.getIdentifiers().get(0).getValue());
    assertThat(dto.getNotes().get(0).getNote()).isEqualTo(authority.getNotes().get(0).getNote());
    assertThat(dto.getSourceFileId()).isEqualTo(authority.getAuthoritySourceFile().getId());
    assertThat(dto.getIdentifiers().get(0).getIdentifierTypeId())
        .isEqualTo(authority.getIdentifiers().get(0).getIdentifierTypeId());

  }

  @Test
  void testToDtoWithNullInput() {
    Authority authority = authorityMapper.toEntity(null);

    assertThat(authority).isNull();
  }

  @Test
  void testToDtoWithValidData() {
    Authority authority = createAuthority();

    AuthorityDto authorityDto = authorityMapper.toDto(authority);

    assertThat(authorityDto).isNotNull();
    assertThat(authority.getId()).isEqualTo(authorityDto.getId());
    assertThat(authority.getVersion()).isEqualTo(authorityDto.getVersion());
    assertThat(authority.getSource()).isEqualTo(authorityDto.getSource());
    assertThat(authority.getNaturalId()).isEqualTo(authorityDto.getNaturalId());
    assertThat(authority.getAuthoritySourceFile().getId()).isEqualTo(authorityDto.getSourceFileId());
    AuthorityIdentifier identifier = authority.getIdentifiers().get(0);
    assertThat(identifier.getIdentifierTypeId()).isEqualTo(authorityDto.getIdentifiers().get(0).getIdentifierTypeId());
    assertThat(identifier.getValue()).isEqualTo(authorityDto.getIdentifiers().get(0).getValue());
    assertThat(authority.getNotes().get(0).getNote()).isEqualTo(authorityDto.getNotes().get(0).getNote());
    assertThat(authority.getNotes().get(0).getStaffOnly()).isEqualTo(authorityDto.getNotes().get(0).getStaffOnly());
    assertThat(String.valueOf(authority.getSubjectHeadingCode())).isEqualTo(authorityDto.getSubjectHeadings());
    assertThat(fromTimestamp(authority.getUpdatedDate())).isEqualTo(authorityDto.getMetadata().getUpdatedDate());
  }


  @Test
  void testToAuthorityWithNullInput() {
    AuthorityDto authorityDto = authorityMapper.toDto(null);

    assertThat(authorityDto).isNull();
  }

  @Test
  void testAuthorityArchiveToDtoWithValidData() {
    var archive = createAuthorityArchive();

    var dto = authorityMapper.toDto(archive);

    assertThat(dto).isNotNull();
    assertThat(archive.getId()).isEqualTo(dto.getId());
    assertThat(archive.getVersion()).isEqualTo(dto.getVersion());
    assertThat(archive.getSource()).isEqualTo(dto.getSource());
    assertThat(archive.getNaturalId()).isEqualTo(dto.getNaturalId());
    assertThat(archive.getAuthoritySourceFile().getId()).isEqualTo(dto.getSourceFileId());
    AuthorityIdentifier identifier = archive.getIdentifiers().get(0);
    assertThat(identifier.getIdentifierTypeId()).isEqualTo(dto.getIdentifiers().get(0).getIdentifierTypeId());
    assertThat(identifier.getValue()).isEqualTo(dto.getIdentifiers().get(0).getValue());
    assertThat(archive.getNotes().get(0).getNote()).isEqualTo(dto.getNotes().get(0).getNote());
    assertThat(archive.getNotes().get(0).getStaffOnly()).isEqualTo(dto.getNotes().get(0).getStaffOnly());
    assertThat(String.valueOf(archive.getSubjectHeadingCode())).isEqualTo(dto.getSubjectHeadings());
    assertThat(fromTimestamp(archive.getUpdatedDate())).isEqualTo(dto.getMetadata().getUpdatedDate());
  }

  @Test
  void testToAuthorityArchiveWithNullInput() {
    AuthorityDto authorityDto = authorityMapper.toDto(null);

    assertThat(authorityDto).isNull();
  }

  @Test
  void testToAuthorityIdentifierWithValidData() {
    var dtoIdentifier = new AuthorityDtoIdentifier();
    dtoIdentifier.setValue(TEST_PROPERTY_VALUE);
    dtoIdentifier.setIdentifierTypeId(TEST_ID);

    AuthorityIdentifier authorityIdentifier = authorityMapper.toAuthorityIdentifier(dtoIdentifier);

    assertThat(authorityIdentifier).isNotNull();
    assertThat(dtoIdentifier.getValue()).isEqualTo(authorityIdentifier.getValue());
    assertThat(dtoIdentifier.getIdentifierTypeId()).isEqualTo(authorityIdentifier.getIdentifierTypeId());

  }

  @Test
  void testToAuthorityIdentifierWithNullInput() {
    AuthorityIdentifier authorityIdentifier = authorityMapper.toAuthorityIdentifier(null);

    assertThat(authorityIdentifier).isNull();
  }


  @Test
  void testToAuthorityNoteWithValidData() {
    var dtoNote = new AuthorityDtoNote();
    dtoNote.setNoteTypeId(TEST_ID);
    dtoNote.setNote(TEST_PROPERTY_VALUE);
    dtoNote.setStaffOnly(true);

    AuthorityNote authorityNote = authorityMapper.toAuthorityNote(dtoNote);

    assertThat(authorityNote).isNotNull();
    assertThat(dtoNote.getNoteTypeId()).isEqualTo(authorityNote.getNoteTypeId());
    assertThat(dtoNote.getNote()).isEqualTo(authorityNote.getNote());
    assertThat(authorityNote.getStaffOnly()).isTrue();
  }

  @Test
  void testToAuthorityNoteWithNullInput() {
    AuthorityNote authorityNote = authorityMapper.toAuthorityNote(null);

    assertThat(authorityNote).isNull();
  }

  @Test
  void testToAuthorityDtoNoteWithValidData() {
    var authorityNote = new AuthorityNote();
    authorityNote.setNoteTypeId(TEST_ID);
    authorityNote.setNote(TEST_PROPERTY_VALUE);
    authorityNote.setStaffOnly(true);

    AuthorityDtoNote dtoNote = authorityMapper.toAuthorityDtoNote(authorityNote);

    assertThat(authorityNote.getNoteTypeId()).isEqualTo(dtoNote.getNoteTypeId());
    assertThat(authorityNote.getNote()).isEqualTo(dtoNote.getNote());
    assertThat(dtoNote.getStaffOnly()).isTrue();
  }

  @Test
  void testToAuthorityDtoNoteWithNullInput() {
    AuthorityDtoNote dtoNote = authorityMapper.toAuthorityDtoNote(null);

    assertThat(dtoNote).isNull();
  }

  @Test
  void testToDtoListWithValidData() {
    AuthorityBase authority = createAuthority();
    var authorityList = new ArrayList<>(List.of(authority));

    List<AuthorityDto> dtoList = authorityMapper.toDtoList(authorityList);

    assertThat(dtoList).hasSize(1);
    AuthorityDto dto1 = dtoList.get(0);
    assertThat(authority.getId()).isEqualTo(dto1.getId());
    assertThat(authority.getVersion()).isEqualTo(dto1.getVersion());
    assertThat(authority.getSource()).isEqualTo(dto1.getSource());
    assertThat(authority.getNaturalId()).isEqualTo(dto1.getNaturalId());
    assertThat(authority.getAuthoritySourceFile().getId()).isEqualTo(dto1.getSourceFileId());
    AuthorityIdentifier identifier = authority.getIdentifiers().get(0);
    assertThat(identifier.getIdentifierTypeId()).isEqualTo(dto1.getIdentifiers().get(0).getIdentifierTypeId());
    assertThat(identifier.getValue()).isEqualTo(dto1.getIdentifiers().get(0).getValue());
    assertThat(authority.getNotes().get(0).getNote()).isEqualTo(dto1.getNotes().get(0).getNote());
    assertThat(authority.getNotes().get(0).getStaffOnly()).isEqualTo(dto1.getNotes().get(0).getStaffOnly());

  }

  @Test
  void testToDtoListWithNullInput() {
    List<AuthorityDto> dtoList = authorityMapper.toDtoList(null);

    assertThat(dtoList).isNull();
  }

  @Test
  void testToAuthorityDtoIdentifierWithValidData() {
    var authorityIdentifier = new AuthorityIdentifier();
    authorityIdentifier.setValue(TEST_PROPERTY_VALUE);
    authorityIdentifier.setIdentifierTypeId(TEST_ID);

    AuthorityDtoIdentifier dtoIdentifier = authorityMapper.toAuthorityDtoIdentifier(authorityIdentifier);

    assertThat(dtoIdentifier).isNotNull();
    assertThat(authorityIdentifier.getValue()).isEqualTo(dtoIdentifier.getValue());
    assertThat(authorityIdentifier.getIdentifierTypeId()).isEqualTo(dtoIdentifier.getIdentifierTypeId());
  }

  @Test
  void testToAuthorityDtoIdentifierWithNullInput() {
    AuthorityDtoIdentifier dtoIdentifier = authorityMapper.toAuthorityDtoIdentifier(null);

    assertThat(dtoIdentifier).isNull();
  }

  @Test
   void testToAuthorityCollectionWithValidPage() {
    AuthorityBase authority = createAuthority();

    var authorityList = List.of(authority);
    var authorityPage = new PageImpl<>(authorityList);

    AuthorityDtoCollection dtoCollection = authorityMapper.toAuthorityCollection(authorityPage);

    assertThat(dtoCollection).isNotNull();
    assertThat(dtoCollection.getAuthorities()).hasSize(1);
    AuthorityDto dto = dtoCollection.getAuthorities().get(0);
    assertThat(authority.getId()).isEqualTo(dto.getId());
  }

  @NotNull
  private static Authority createAuthority() {
    var file = new AuthoritySourceFile();
    file.setId(TEST_ID);
    return Authority.builder()
        .id(TEST_ID)
        .version(TEST_VERSION)
        .source(TEST_PROPERTY_VALUE)
        .naturalId(TEST_PROPERTY_VALUE)
        .authoritySourceFile(file)
        .identifiers(List.of(new AuthorityIdentifier(TEST_PROPERTY_VALUE, TEST_ID)))
        .notes(List.of(new AuthorityNote(TEST_ID, TEST_PROPERTY_VALUE, true)))
        .subjectHeadingCode(TEST_PROPERTY_VALUE.charAt(0))
        .updatedDate(TEST_DATE)
        .createdDate(TEST_DATE)
        .updatedByUserId(TEST_ID)
        .createdByUserId(TEST_ID)
        .build();
  }

  @NotNull
  private static AuthorityArchive createAuthorityArchive() {
    var file = new AuthoritySourceFile();
    file.setId(TEST_ID);
    var archive = new AuthorityArchive();
    archive.setId(TEST_ID);
    archive.setVersion(TEST_VERSION);
    archive.setSource(TEST_PROPERTY_VALUE);
    archive.setNaturalId(TEST_PROPERTY_VALUE);
    archive.setAuthoritySourceFile(file);
    archive.setIdentifiers(List.of(new AuthorityIdentifier(TEST_PROPERTY_VALUE, TEST_ID)));
    archive.setNotes(List.of(new AuthorityNote(TEST_ID, TEST_PROPERTY_VALUE, true)));
    archive.setSubjectHeadingCode(TEST_PROPERTY_VALUE.charAt(0));
    archive.setUpdatedDate(TEST_DATE);
    archive.setCreatedDate(TEST_DATE);
    archive.setUpdatedByUserId(TEST_ID);
    archive.setCreatedByUserId(TEST_ID);
    return archive;
  }

  @NotNull
  private static AuthorityDto createAuthorityDto() {
    var dto = new AuthorityDto();
    dto.setId(TEST_ID);
    dto.setNaturalId(SOURCE_FILE_NATURAL_ID);
    dto.setSource(SOURCE_FILE_SOURCE);
    dto.setVersion(TEST_VERSION);
    dto.setIdentifiers(List.of(new AuthorityDtoIdentifier(TEST_PROPERTY_VALUE, TEST_ID)));
    dto.setNotes(List.of(new AuthorityDtoNote(TEST_ID, TEST_PROPERTY_VALUE)));
    dto.setSubjectHeadings(TEST_PROPERTY_VALUE);
    dto.setSourceFileId(TEST_ID);
    return dto;
  }

  @Test
  void serializedDtoShouldNotContainEmptyArraysForExtendedFields() throws JsonProcessingException {

    String serializedDto = new ObjectMapper().writeValueAsString(new AuthorityDto());

    assertTrue(serializedDto.contains("\"saftGenreTerm\""),
        "JSON should contain 'saftGenreTerm' key when it's an empty array");
    assertFalse(serializedDto.contains("\"saftBroaderTerm\""),
        "JSON should not contain 'saftBroaderTerm' key when it's an empty array");
    assertFalse(serializedDto.contains("\"saftNarrowerTerm\""),
        "JSON should not contain 'saftNarrowerTerm' key when it's an empty array");
    assertFalse(serializedDto.contains("\"saftEarlierHeading\""),
        "JSON should not contain 'saftEarlierHeading' key when it's an empty array");
    assertFalse(serializedDto.contains("\"saftLaterHeading\""),
        "JSON should not contain 'saftLaterHeading' key when it's an empty array");
  }
}
