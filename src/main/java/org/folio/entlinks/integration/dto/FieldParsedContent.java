package org.folio.entlinks.integration.dto;

import static org.folio.entlinks.utils.FieldUtils.ID_SUBFIELD_CODE;
import static org.folio.entlinks.utils.FieldUtils.NATURAL_ID_SUBFIELD_CODE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.folio.entlinks.domain.dto.LinkDetails;

/**
 * Represents the parsed content of a MARC field, including its tag, indicators, and associated subfields.
 * This class provides methods to retrieve and manipulate subfields associated with the field.
 */
@Getter
public class FieldParsedContent {

  private @Setter String tag;
  private @Setter String ind1;
  private @Setter String ind2;
  private @Setter LinkDetails linkDetails;

  private List<ParsedSubfield> subfieldList;

  private Map<Character, List<ParsedSubfield>> subfieldsMap;

  public FieldParsedContent(String tag, String ind1, String ind2, List<ParsedSubfield> subfieldList,
                            LinkDetails linkDetails) {
    this.tag = tag;
    this.ind1 = ind1;
    this.ind2 = ind2;
    this.subfieldList = subfieldList;
    this.linkDetails = linkDetails;
    this.subfieldsMap = toSubfieldMap(subfieldList);
  }

  public List<ParsedSubfield> getSubfields(char code) {
    return subfieldsMap.get(code);
  }

  public List<ParsedSubfield> getIdSubfields() {
    return subfieldsMap.get(ID_SUBFIELD_CODE);
  }

  public List<ParsedSubfield> getNaturalIdSubfields() {
    return subfieldsMap.get(NATURAL_ID_SUBFIELD_CODE);
  }

  public boolean hasSubfield(char code) {
    return subfieldsMap.containsKey(code);
  }

  public void setSubfieldList(List<ParsedSubfield> subfieldList) {
    this.subfieldList = subfieldList;
    this.subfieldsMap = toSubfieldMap(subfieldList);
  }

  private Map<Character, List<ParsedSubfield>> toSubfieldMap(List<ParsedSubfield> subfieldList) {
    return subfieldList.stream()
      .collect(Collectors.groupingBy(ParsedSubfield::code));
  }
}
