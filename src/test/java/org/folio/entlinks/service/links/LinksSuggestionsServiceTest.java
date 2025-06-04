package org.folio.entlinks.service.links;

import static java.util.Collections.emptyMap;
import static org.folio.entlinks.config.constants.ErrorCode.DISABLED_AUTO_LINKING;
import static org.folio.entlinks.config.constants.ErrorCode.MORE_THAN_ONE_SUGGESTIONS;
import static org.folio.entlinks.config.constants.ErrorCode.NO_SUGGESTIONS;
import static org.folio.entlinks.utils.FieldUtils.ID_SUBFIELD_CODE;
import static org.folio.entlinks.utils.FieldUtils.NATURAL_ID_SUBFIELD_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.ParsedSubfield;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinksSuggestionsServiceTest {

  private static final UUID AUTHORITY_ID = UUID.randomUUID();
  private static final UUID SOURCE_FILE_ID = UUID.fromString("a501dcc2-23ce-4a4a-adb4-ff683b6f325e");
  private static final String NATURAL_ID = "n12345";
  private static final String BASE_URL = "https://base/url/";
  private static final String SOURCE_FILE_NAME = "sourceFileName";

  private @Spy AuthorityRuleValidationService authorityRuleValidationService;
  private @Mock AuthoritySourceFileService authoritySourceFileService;
  private @InjectMocks LinksSuggestionsService linksSuggestionsService;

  private AuthoritySourceFile authoritySourceFile;

  @BeforeEach
  void setup() {
    authoritySourceFile = new AuthoritySourceFile();
    authoritySourceFile.setId(SOURCE_FILE_ID);
    authoritySourceFile.setBaseUrl(BASE_URL);
    authoritySourceFile.setName(SOURCE_FILE_NAME);

    var sourceFileCode = new AuthoritySourceFileCode();
    sourceFileCode.setAuthoritySourceFile(authoritySourceFile);
    sourceFileCode.setCode("n");
    authoritySourceFile.addCode(sourceFileCode);
  }

  @ParameterizedTest
  @ValueSource(chars = {NATURAL_ID_SUBFIELD_CODE, ID_SUBFIELD_CODE})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withNewLink(char linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", null);
    var authority = getAuthorityParsedRecordContent("100");
    when(authoritySourceFileService.getById(SOURCE_FILE_ID)).thenReturn(authoritySourceFile);

    linksSuggestionsService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var bibField = bib.getFields().getFirst();
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.NEW, linkDetails.getStatus());
    assertEquals(AUTHORITY_ID, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getAuthorityNaturalId());
    assertEquals(1, linkDetails.getLinkingRuleId());
    assertNull(linkDetails.getErrorCause());

    assertEquals(AUTHORITY_ID.toString(), bibField.getSubfields('9').getFirst().value());
    assertEquals(BASE_URL + NATURAL_ID, bibField.getSubfields('0').getFirst().value());
    assertFalse(bibField.hasSubfield('a'));
    assertTrue(bibField.hasSubfield('b'));
  }

  @ParameterizedTest
  @ValueSource(chars = {NATURAL_ID_SUBFIELD_CODE, ID_SUBFIELD_CODE})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withMultipleRulesForFieldAndOnlyOneSuitable(
    char linkingMatchSubfield) {
    var rules = getMapRule("240", List.of("100", "110", "111"));
    var bib = getBibParsedRecordContent("240", getActualLinksDetails());
    var authorityId = UUID.randomUUID();
    var authority = getAuthorityParsedRecordContent(UUID.randomUUID(), "130", Map.of("a", "test"));
    var secondAuthority = getAuthorityParsedRecordContent(authorityId, "110", Map.of("a", "test"));
    var thirdAuthority = getAuthorityParsedRecordContent(UUID.randomUUID(), "111", Map.of("a", "test"));
    when(authoritySourceFileService.getById(SOURCE_FILE_ID)).thenReturn(authoritySourceFile);

    linksSuggestionsService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority, secondAuthority, thirdAuthority),
        rules, linkingMatchSubfield, false);

    var bibField = bib.getFields().getFirst();
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.ACTUAL, linkDetails.getStatus());
    assertEquals(authorityId, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getAuthorityNaturalId());
    assertEquals(1, linkDetails.getLinkingRuleId());
    assertNull(linkDetails.getErrorCause());

    assertEquals(authorityId.toString(), bibField.getSubfields('9').getFirst().value());
    assertEquals(BASE_URL + NATURAL_ID, bibField.getSubfields('0').getFirst().value());
    assertFalse(bibField.hasSubfield('a'));
    assertTrue(bibField.hasSubfield('b'));
  }

  @ParameterizedTest
  @ValueSource(chars = {NATURAL_ID_SUBFIELD_CODE, ID_SUBFIELD_CODE})
  void fillLinkDetailsWithSuggestedAuthorities_shouldUpdateAndRemoveControlledSubfield(char linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var initialBibSubfields = new HashMap<String, String>();
    initialBibSubfields.put("c", "c value");
    var bib = getBibParsedRecordContent("100", initialBibSubfields, null);
    var authority = getAuthorityParsedRecordContent("100");
    when(authoritySourceFileService.getById(SOURCE_FILE_ID)).thenReturn(authoritySourceFile);

    linksSuggestionsService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var bibField = bib.getFields().getFirst();
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.NEW, linkDetails.getStatus());
    assertEquals(AUTHORITY_ID, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getAuthorityNaturalId());
    assertEquals(1, linkDetails.getLinkingRuleId());
    assertNull(linkDetails.getErrorCause());

    assertEquals(AUTHORITY_ID.toString(), bibField.getSubfields('9').getFirst().value());
    assertEquals(BASE_URL + NATURAL_ID, bibField.getSubfields('0').getFirst().value());
    assertFalse(bibField.hasSubfield('c'));
    assertTrue(bibField.hasSubfield('b'));
  }

  @ParameterizedTest
  @ValueSource(chars = {NATURAL_ID_SUBFIELD_CODE, ID_SUBFIELD_CODE})
  void fillLinkDetailsWithSuggestedAuthorities_shouldUpdateAndRemoveControlledSubfieldWithModification(
    char linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var initialBibSubfields = new HashMap<String, String>();
    initialBibSubfields.put("b", "b value");
    var bib = getBibParsedRecordContent("100", initialBibSubfields, null);
    var authority = getAuthorityParsedRecordContent("100");
    when(authoritySourceFileService.getById(SOURCE_FILE_ID)).thenReturn(authoritySourceFile);

    linksSuggestionsService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var bibField = bib.getFields().getFirst();
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.NEW, linkDetails.getStatus());
    assertEquals(AUTHORITY_ID, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getAuthorityNaturalId());
    assertEquals(1, linkDetails.getLinkingRuleId());
    assertNull(linkDetails.getErrorCause());

    assertEquals(1, bibField.getSubfields('b').size());
    assertEquals("test", bibField.getSubfields('b').getFirst().value());
    assertEquals(AUTHORITY_ID.toString(), bibField.getSubfields('9').getFirst().value());
    assertEquals(BASE_URL + NATURAL_ID, bibField.getSubfields('0').getFirst().value());
  }

  @ParameterizedTest
  @ValueSource(chars = {NATURAL_ID_SUBFIELD_CODE, ID_SUBFIELD_CODE})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withActualLink(char linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", getActualLinksDetails());
    var authority = getAuthorityParsedRecordContent("100");
    when(authoritySourceFileService.getById(SOURCE_FILE_ID)).thenReturn(authoritySourceFile);

    linksSuggestionsService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var bibField = bib.getFields().getFirst();
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.ACTUAL, linkDetails.getStatus());
    assertEquals(AUTHORITY_ID, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getAuthorityNaturalId());
    assertEquals(1, linkDetails.getLinkingRuleId());
    assertNull(linkDetails.getErrorCause());

    assertEquals(AUTHORITY_ID.toString(), bibField.getSubfields('9').getFirst().value());
    assertEquals(BASE_URL + NATURAL_ID, bibField.getSubfields('0').getFirst().value());
    assertFalse(bibField.hasSubfield('a'));
    assertTrue(bibField.hasSubfield('b'));
  }

  @ParameterizedTest
  @ValueSource(chars = {NATURAL_ID_SUBFIELD_CODE, ID_SUBFIELD_CODE})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withError_whenRequiredAuthoritySubfieldNotExist(
    char linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", null);
    var authority = getAuthorityParsedRecordContent("100", emptyMap());

    linksSuggestionsService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var linkDetails = bib.getFields().getFirst().getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(NO_SUGGESTIONS.getCode(), linkDetails.getErrorCause());
  }

  @ParameterizedTest
  @ValueSource(chars = {NATURAL_ID_SUBFIELD_CODE, ID_SUBFIELD_CODE})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withErrorMoreThenOneAuthoritiesFound(
    char linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", getActualLinksDetails());
    var authority = getAuthorityParsedRecordContent("100");
    var secondAuthority = getAuthorityParsedRecordContent("100");

    linksSuggestionsService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority, secondAuthority), rules,
        linkingMatchSubfield, false);

    var linkDetails = bib.getFields().getFirst().getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(MORE_THAN_ONE_SUGGESTIONS.getCode(), linkDetails.getErrorCause());
    assertNull(linkDetails.getAuthorityId());
    assertNull(linkDetails.getAuthorityNaturalId());
    assertNull(linkDetails.getLinkingRuleId());
  }

  @ParameterizedTest
  @ValueSource(chars = {NATURAL_ID_SUBFIELD_CODE, ID_SUBFIELD_CODE})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withErrorNoAuthoritiesFound(
    char linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    var bib = getBibParsedRecordContent("100", null);
    var authority = getAuthorityParsedRecordContent("110");

    linksSuggestionsService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var linkDetails = bib.getFields().getFirst().getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(NO_SUGGESTIONS.getCode(), linkDetails.getErrorCause());
  }

  @ParameterizedTest
  @ValueSource(chars = {NATURAL_ID_SUBFIELD_CODE, ID_SUBFIELD_CODE})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_withErrorDisabledAutoLinkingFeature(
    char linkingMatchSubfield) {
    var rules = getMapRule("600", "100");
    disableAutoLinkingFeature(rules.get("600"));

    var bib = getBibParsedRecordContent("600", null);
    var authority = getAuthorityParsedRecordContent("110");

    linksSuggestionsService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, false);

    var linkDetails = bib.getFields().getFirst().getLinkDetails();
    assertEquals(LinkStatus.ERROR, linkDetails.getStatus());
    assertEquals(DISABLED_AUTO_LINKING.getCode(), linkDetails.getErrorCause());
  }

  @ParameterizedTest
  @ValueSource(chars = {NATURAL_ID_SUBFIELD_CODE, ID_SUBFIELD_CODE})
  void fillLinkDetailsWithSuggestedAuthorities_shouldFillLinkDetails_onIgnoredAutoLinkingFeature(
    char linkingMatchSubfield) {
    var rules = getMapRule("100", "100");
    disableAutoLinkingFeature(rules.get("100"));
    var bib = getBibParsedRecordContent("100", getActualLinksDetails());
    var authority = getAuthorityParsedRecordContent("100");
    when(authoritySourceFileService.getById(SOURCE_FILE_ID)).thenReturn(authoritySourceFile);

    linksSuggestionsService
      .fillLinkDetailsWithSuggestedAuthorities(List.of(bib), List.of(authority), rules, linkingMatchSubfield, true);

    var bibField = bib.getFields().getFirst();
    var linkDetails = bibField.getLinkDetails();
    assertEquals(LinkStatus.ACTUAL, linkDetails.getStatus());
    assertEquals(AUTHORITY_ID, linkDetails.getAuthorityId());
    assertEquals(NATURAL_ID, linkDetails.getAuthorityNaturalId());
    assertEquals(1, linkDetails.getLinkingRuleId());
    assertNull(linkDetails.getErrorCause());

    assertEquals(AUTHORITY_ID.toString(), bibField.getSubfields('9').getFirst().value());
    assertEquals(BASE_URL + NATURAL_ID, bibField.getSubfields('0').getFirst().value());
    assertFalse(bibField.hasSubfield('a'));
    assertTrue(bibField.hasSubfield('b'));
  }

  @Test
  void shouldFillErrorDetailsWithNoSuggestions_onlyForFieldWithNoError() {
    var bibs = List.of(getBibParsedRecordContent("100", getActualLinksDetails()),
      getBibParsedRecordContent("101", getActualLinksDetails().errorCause("test")));

    linksSuggestionsService.fillErrorDetailsWithNoSuggestions(bibs, '0');

    assertEquals("101", bibs.getFirst().getFields().getFirst().getLinkDetails().getErrorCause());
    assertEquals("test", bibs.get(1).getFields().getFirst().getLinkDetails().getErrorCause());
  }

  @Test
  void shouldFillErrorDetailsWithDisabledAutoLinking() {
    var field = new FieldParsedContent("100", "//", "//",
      List.of(new ParsedSubfield('0', NATURAL_ID)), null);

    linksSuggestionsService.fillErrorDetailsWithDisabledAutoLinking(field, '0');

    assertEquals("103", field.getLinkDetails().getErrorCause());
  }

  @Test
  void shouldNotFillErrorDetailsWithDisabledAutoLinking_whenNoSubfield() {
    var field = new FieldParsedContent("100", "//", "//", new ArrayList<>(), null);

    linksSuggestionsService.fillErrorDetailsWithDisabledAutoLinking(field, '0');

    assertNull(field.getLinkDetails());
  }

  private AuthorityParsedContent getAuthorityParsedRecordContent(String authorityField) {
    return getAuthorityParsedRecordContent(authorityField, Map.of("a", "test"));
  }

  private AuthorityParsedContent getAuthorityParsedRecordContent(String authorityField,
                                                                 Map<String, String> subfields) {
    return getAuthorityParsedRecordContent(AUTHORITY_ID, authorityField, subfields);
  }

  private AuthorityParsedContent getAuthorityParsedRecordContent(UUID authorityId, String authorityField,
                                                                 Map<String, String> subfields) {
    var subfieldList = subfields.entrySet().stream()
      .map(entry -> new ParsedSubfield(entry.getKey().charAt(0), entry.getValue()))
      .toList();
    var field = new FieldParsedContent(authorityField, "//", "//", subfieldList, null);
    return new AuthorityParsedContent(authorityId,
      NATURAL_ID,
      "",
      List.of(field),
      SOURCE_FILE_ID);
  }

  private SourceParsedContent getBibParsedRecordContent(String bibField, LinkDetails linkDetails) {
    return getBibParsedRecordContent(bibField, new HashMap<>(), linkDetails);
  }

  private SourceParsedContent getBibParsedRecordContent(String bibField, Map<String, String> subfields,
                                                        LinkDetails linkDetails) {
    var subfieldList = subfields.entrySet().stream()
      .map(entry -> new ParsedSubfield(entry.getKey().charAt(0), entry.getValue()))
      .collect(Collectors.toList());
    subfieldList.add(new ParsedSubfield('0', NATURAL_ID));
    subfieldList.add(new ParsedSubfield('9', AUTHORITY_ID.toString()));
    var field = new FieldParsedContent(bibField, "//", "//", subfieldList, linkDetails);
    return new SourceParsedContent(UUID.randomUUID(), "", List.of(field));
  }

  private LinkDetails getActualLinksDetails() {
    return new LinkDetails().linkingRuleId(2)
      .status(LinkStatus.ACTUAL)
      .authorityId(UUID.randomUUID())
      .authorityNaturalId(NATURAL_ID);
  }

  private Map<String, List<InstanceAuthorityLinkingRule>> getMapRule(String bibField, String authorityField) {
    var modification = new SubfieldModification().source("a").target("b");
    var existence = Map.of("a", true);

    var rule = new InstanceAuthorityLinkingRule();
    rule.setId(1);
    rule.setBibField(bibField);
    rule.setAuthorityField(authorityField);
    rule.setAutoLinkingEnabled(true);
    rule.setAuthoritySubfields(new char[] {'a', 'c'});
    rule.setSubfieldModifications(List.of(modification));
    rule.setSubfieldsExistenceValidations(existence);

    return Map.of(bibField, List.of(rule));
  }

  private Map<String, List<InstanceAuthorityLinkingRule>> getMapRule(String bibField, List<String> authorityFields) {
    var modification = new SubfieldModification().source("a").target("b");
    var existence = Map.of("a", true);

    var rules = new ArrayList<InstanceAuthorityLinkingRule>();
    for (String authorityField : authorityFields) {
      var rule = new InstanceAuthorityLinkingRule();
      rule.setId(1);
      rule.setBibField(bibField);
      rule.setAuthorityField(authorityField);
      rule.setAutoLinkingEnabled(true);
      rule.setAuthoritySubfields(new char[] {'a', 'c'});
      rule.setSubfieldModifications(List.of(modification));
      rule.setSubfieldsExistenceValidations(existence);
      rules.add(rule);
    }

    return Map.of(bibField, rules);
  }

  private void disableAutoLinkingFeature(List<InstanceAuthorityLinkingRule> rules) {
    rules.forEach(rule -> rule.setAutoLinkingEnabled(false));
  }
}
