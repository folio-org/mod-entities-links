package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.TestUtils.mapOf;
import static org.folio.support.base.TestConstants.TEST_ID;
import static org.folio.support.base.TestConstants.TEST_PROPERTY_VALUE;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.domain.dto.ExternalIdsHolder;
import org.folio.entlinks.domain.dto.FieldContentValue;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.dto.ParsedRecordContent;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.domain.dto.StrippedParsedRecord;
import org.folio.entlinks.domain.dto.StrippedParsedRecordCollection;
import org.folio.entlinks.domain.dto.StrippedParsedRecordParsedRecord;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.ParsedSubfield;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.folio.spring.testing.type.UnitTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

@UnitTest
class SourceContentMapperTest {

  private final SourceContentMapper mapper = new SourceContentMapperImpl();

  @Test
  void testConvertToParsedContentCollection() {
    SourceParsedContent content = createSourceParsedContent();

    var listOfContent = List.of(content);

    ParsedRecordContentCollection contentCollection = mapper.convertToParsedContentCollection(listOfContent);

    assertThat(contentCollection.getRecords()).hasSize(1);
    assertThat(contentCollection.getRecords().getFirst().getLeader()).isEqualTo(content.getLeader());
    FieldContentValue tag = contentCollection.getRecords().getFirst().getFields().getFirst().get("tag");
    assertThat(tag.getInd1()).isEqualTo(content.getFields().getFirst().getInd1());
    assertThat(tag.getInd2()).isEqualTo(content.getFields().getFirst().getInd2());
    var subFieldValue = content.getFields().getFirst().getSubfields('a').getFirst().value();
    assertThat(tag.getSubfields().getFirst()).containsEntry("a", subFieldValue);

  }

  @Test
  void testConvertToParsedContent() {
    ParsedRecordContent content = createParsedRecordContent();

    SourceParsedContent result = mapper.convertToParsedContent(content);

    assertThat(result.getLeader()).isEqualTo(content.getLeader());
    FieldParsedContent parsedContent = result.getFields().getFirst();
    assertThat(parsedContent.getInd1()).isEqualTo(content.getFields().getFirst().get("tag").getInd1());
    assertThat(parsedContent.getInd2()).isEqualTo(content.getFields().getFirst().get("tag").getInd2());
    assertThat(parsedContent.getLinkDetails()).isEqualTo(content.getFields().getFirst().get("tag").getLinkDetails());
  }

  @Test
  void testConvertToAuthorityParsedContent() {
    var recordCollection = new StrippedParsedRecordCollection();
    StrippedParsedRecord strippedParsedRecord = createStrippedParsedRecord();
    recordCollection.getRecords().add(strippedParsedRecord);
    var authoritySourceFile = new AuthoritySourceFile();
    authoritySourceFile.setId(UUID.randomUUID());
    var authority = Authority.builder()
      .id(TEST_ID)
      .source(TEST_PROPERTY_VALUE)
      .naturalId(TEST_ID.toString())
      .authoritySourceFile(authoritySourceFile)
      .build();
    var authorities = List.of(authority);

    List<AuthorityParsedContent> resultList = mapper.convertToAuthorityParsedContent(recordCollection, authorities);

    var authorityParsedContent = resultList.getFirst();
    var parsedContent = authorityParsedContent.getFields().getFirst();
    var field = strippedParsedRecord.getParsedRecord().getContent().getFields().getFirst();
    assertThat(resultList).hasSize(1);
    assertThat(authorityParsedContent.getId()).isEqualTo(authorities.getFirst().getId());
    assertThat(authorityParsedContent.getNaturalId()).isEqualTo(authorities.getFirst().getNaturalId());
    assertThat(parsedContent.getInd1()).isEqualTo(field.get("tag").getInd1());
    assertThat(parsedContent.getInd2()).isEqualTo(field.get("tag").getInd2());
    assertThat(parsedContent.getLinkDetails()).isEqualTo(field.get("tag").getLinkDetails());
    assertThat(authorityParsedContent.getLeader())
      .isEqualTo(strippedParsedRecord.getParsedRecord().getContent().getLeader());
    assertThat(parsedContent.getSubfields('a').getFirst().value())
      .isEqualTo(field.get("tag").getSubfields().getFirst().get("a"));
  }

  @Test
  void testConvertToParsedContent_ContentCollection() {
    ParsedRecordContent recordContent = createParsedRecordContent();
    var contentCollection = new ParsedRecordContentCollection();
    contentCollection.setRecords(List.of(recordContent));

    List<SourceParsedContent> resultList = mapper.convertToParsedContent(contentCollection);

    FieldParsedContent parsedContent = resultList.getFirst().getFields().getFirst();
    Map<String, FieldContentValue> contentMap = recordContent.getFields().getFirst();
    assertThat(resultList).hasSize(1);
    assertThat(resultList.getFirst().getLeader()).isEqualTo(recordContent.getLeader());
    assertThat(parsedContent.getInd1()).isEqualTo(contentMap.get("tag").getInd1());
    assertThat(parsedContent.getInd2()).isEqualTo(contentMap.get("tag").getInd2());
    assertThat(parsedContent.getLinkDetails()).isEqualTo(contentMap.get("tag").getLinkDetails());
    assertThat(parsedContent.getSubfields('a').getFirst().value())
      .isEqualTo(contentMap.get("tag").getSubfields().getFirst().get("a"));
  }

  @NotNull
  private static StrippedParsedRecord createStrippedParsedRecord() {
    var strippedParsedRecord = new StrippedParsedRecord();
    var externalIdsHolder = new ExternalIdsHolder();
    externalIdsHolder.setAuthorityId(TEST_ID);
    strippedParsedRecord.setExternalIdsHolder(externalIdsHolder);
    var parsedRecord = new StrippedParsedRecordParsedRecord();
    var parsedRecordContent = createParsedRecordContent();
    parsedRecordContent.setLeader(TEST_PROPERTY_VALUE);
    parsedRecord.setContent(createParsedRecordContent());
    strippedParsedRecord.setParsedRecord(parsedRecord);
    return strippedParsedRecord;
  }

  @NotNull
  private static ParsedRecordContent createParsedRecordContent() {
    FieldContentValue fieldContent = new FieldContentValue();
    fieldContent.setInd1("ind1");
    fieldContent.setInd2("ind2");
    fieldContent.setSubfields(List.of(mapOf("a", "a1"), mapOf("b", "b1")));
    fieldContent.setLinkDetails(new LinkDetails());
    Map<String, FieldContentValue> fields = mapOf("tag", fieldContent);
    return new ParsedRecordContent(List.of(fields), "leader");
  }

  @NotNull
  private static SourceParsedContent createSourceParsedContent() {
    FieldParsedContent fieldContent =
      new FieldParsedContent("tag", "ind1", "ind2",
        List.of(new ParsedSubfield('a', "a1"),
          new ParsedSubfield('a', "a2")), new LinkDetails());
    return new SourceParsedContent(TEST_ID, TEST_PROPERTY_VALUE, List.of(fieldContent));
  }
}
