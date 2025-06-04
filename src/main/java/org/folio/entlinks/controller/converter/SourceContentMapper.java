package org.folio.entlinks.controller.converter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.entlinks.domain.dto.FieldContentValue;
import org.folio.entlinks.domain.dto.ParsedRecordContent;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.domain.dto.StrippedParsedRecord;
import org.folio.entlinks.domain.dto.StrippedParsedRecordCollection;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.ParsedSubfield;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SourceContentMapper {

  default ParsedRecordContentCollection convertToParsedContentCollection(List<SourceParsedContent> listOfContent) {
    return new ParsedRecordContentCollection()
      .records(listOfContent.stream()
        .map(this::convertToParsedContent)
        .toList());
  }

  default ParsedRecordContent convertToParsedContent(SourceParsedContent content) {
    var fields = convertFieldsToListOfMaps(content.getFields());

    return new ParsedRecordContent(fields, content.getLeader());
  }

  default List<SourceParsedContent> convertToParsedContent(ParsedRecordContentCollection contentCollection) {
    return contentCollection.getRecords().stream()
      .map(this::convertToParsedContent)
      .toList();
  }

  default SourceParsedContent convertToParsedContent(ParsedRecordContent content) {
    var fields = convertFieldsToOneMap(content.getFields());
    return new SourceParsedContent(UUID.randomUUID(), content.getLeader(), fields);
  }

  default List<AuthorityParsedContent> convertToAuthorityParsedContent(StrippedParsedRecordCollection recordCollection,
                                                                       List<Authority> authorities) {
    return recordCollection.getRecords().stream()
      .map(parsedRecord -> convertToAuthorityParsedContent(parsedRecord, authorities))
      .toList();
  }

  default AuthorityParsedContent convertToAuthorityParsedContent(StrippedParsedRecord parsedRecord,
                                                                 List<Authority> authorities) {
    var authorityId = parsedRecord.getExternalIdsHolder().getAuthorityId();
    var authority = extractAuthority(authorities, authorityId);
    var naturalId = authority.getNaturalId();
    var leader = parsedRecord.getParsedRecord().getContent().getLeader();
    var fields = convertFieldsToOneMap(parsedRecord.getParsedRecord().getContent().getFields());
    var sourceFileId = extractSourceFileId(authority);

    return new AuthorityParsedContent(authorityId, naturalId, leader, fields, sourceFileId);
  }

  private List<Map<String, FieldContentValue>> convertFieldsToListOfMaps(List<FieldParsedContent> fields) {
    return fields.stream()
      .map(this::convertParsedContent)
      .toList();
  }

  private List<FieldParsedContent> convertFieldsToOneMap(List<Map<String, FieldContentValue>> fields) {
    return fields.stream()
      .flatMap(map -> map.entrySet().stream())
      .map(this::convertFieldContent)
      .toList();
  }

  private FieldParsedContent convertFieldContent(Map.Entry<String, FieldContentValue> fieldContent) {
    var ind1 = fieldContent.getValue().getInd1();
    var ind2 = fieldContent.getValue().getInd2();
    var linkDetails = fieldContent.getValue().getLinkDetails();
    var subfieldsList = fieldContent.getValue().getSubfields().stream()
      .map(map -> map.entrySet().iterator().next())
      .map(entry -> new ParsedSubfield(entry.getKey().charAt(0), entry.getValue()))
      .toList();
    return new FieldParsedContent(fieldContent.getKey(), ind1, ind2, subfieldsList, linkDetails);
  }

  private Map<String, FieldContentValue> convertParsedContent(FieldParsedContent field) {
    var ind1 = field.getInd1();
    var ind2 = field.getInd2();
    var linkDetails = field.getLinkDetails();
    var subfields = convertSubfieldsToListOfMaps(field.getSubfieldList());

    var fieldContent = new FieldContentValue().ind1(ind1).ind2(ind2)
      .linkDetails(linkDetails)
      .subfields(subfields);

    return Map.of(field.getTag(), fieldContent);
  }

  private List<Map<String, String>> convertSubfieldsToListOfMaps(List<ParsedSubfield> subfields) {
    return subfields.stream()
      .map(subfield -> Map.of(String.valueOf(subfield.code()), subfield.value()))
      .toList();
  }

  private Authority extractAuthority(List<Authority> authorities, UUID authorityId) {
    return authorities.stream()
        .filter(authority -> authorityId.equals(authority.getId()))
        .findAny()
        .orElseThrow(() -> new AuthorityNotFoundException(authorityId));
  }

  private UUID extractSourceFileId(Authority authority) {
    var authoritySourceFile = authority.getAuthoritySourceFile();
    return (authoritySourceFile != null) ? authoritySourceFile.getId() : null;
  }
}
