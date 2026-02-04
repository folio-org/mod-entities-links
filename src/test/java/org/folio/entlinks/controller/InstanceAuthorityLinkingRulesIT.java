package org.folio.entlinks.controller;

import static java.lang.Integer.MAX_VALUE;
import static org.folio.support.FileTestUtils.readFile;
import static org.folio.support.JsonTestUtils.asJson;
import static org.folio.support.JsonTestUtils.toObject;
import static org.folio.support.MatchUtils.errorCodeMatch;
import static org.folio.support.MatchUtils.errorMessageMatch;
import static org.folio.support.MatchUtils.errorParameterMatch;
import static org.folio.support.MatchUtils.errorTotalMatch;
import static org.folio.support.MatchUtils.errorTypeMatch;
import static org.folio.support.base.TestConstants.linkingRulesEndpoint;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.dto.LinkingRuleDto;
import org.folio.entlinks.domain.dto.LinkingRulePatchRequest;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.dto.SubfieldValidation;
import org.folio.entlinks.exception.type.ErrorType;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.json.JsonCompareMode;
import tools.jackson.core.type.TypeReference;

@Log4j2
@IntegrationTest
class InstanceAuthorityLinkingRulesIT extends IntegrationTestBase {

  private static final String AUTHORITY_RULES_PATH = "classpath:linking-rules/instance-authority.json";
  private static final TypeReference<List<LinkingRuleDto>> RULES_TYPE_REFERENCE = new TypeReference<>() { };

  private List<LinkingRuleDto> defaultRules;

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @BeforeEach
  void setUp() {
    defaultRules = toObject(readFile(AUTHORITY_RULES_PATH), RULES_TYPE_REFERENCE, objectMapper);
  }

  @Test
  @SneakyThrows
  void getLinkingRules_positive() {
    doGet(linkingRulesEndpoint())
      .andExpect(content().json(asJson(defaultRules, objectMapper), JsonCompareMode.STRICT));
  }

  @Test
  @SneakyThrows
  void getLinkingRulesById_positive() {
    doGet(linkingRulesEndpoint(1))
      .andExpect(content().json(asJson(defaultRules.getFirst(), objectMapper), JsonCompareMode.STRICT));
  }

  @Test
  @SneakyThrows
  void getLinkingRulesById_negative_notFound() {
    tryGet(linkingRulesEndpoint(MAX_VALUE))
      .andExpect(status().isNotFound())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("LinkingRuleNotFoundException")))
      .andExpect(errorCodeMatch(is(ErrorType.NOT_FOUND_ERROR.getValue())))
      .andExpect(errorMessageMatch(is(String.format("Linking rule with ID [%s] was not found", MAX_VALUE))));
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_positive_shouldUpdateautoLinkingEnabled() {
    var request = new LinkingRulePatchRequest().id(1).autoLinkingEnabled(false);
    try {
      doPatch(linkingRulesEndpoint(1), request);

      doGet(linkingRulesEndpoint(1))
        .andExpect(content().json(asJson(defaultRules.getFirst().autoLinkingEnabled(false), objectMapper)));
    } finally {
      request.autoLinkingEnabled(true);
      doPatch(linkingRulesEndpoint(1), request);
    }
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_positive_shouldUpdateAuthoritySubfields() {
    var defaultAuthoritySubfields = new HashSet<>(defaultRules.get(9).getAuthoritySubfields());
    var request = new LinkingRulePatchRequest().id(10).authoritySubfields(Set.of("a", "b"));
    try {
      doPatch(linkingRulesEndpoint(10), request);

      doGet(linkingRulesEndpoint(10))
        .andExpect(content().json(asJson(defaultRules.get(9).authoritySubfields(List.of("a", "b")), objectMapper)));
    } finally {
      request.authoritySubfields(defaultAuthoritySubfields);
      doPatch(linkingRulesEndpoint(10), request);
    }
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_positive_shouldNotUpdateUnexpectedFields() {
    var request = new LinkingRuleDto().id(1)
      .authorityField("abc")
      .addSubfieldModificationsItem(new SubfieldModification().source("1").target("2"))
      .validation(new SubfieldValidation().addExistenceItem(Map.of("s", true)))
      .autoLinkingEnabled(true);
    doPatch(linkingRulesEndpoint(1), request);

    doGet(linkingRulesEndpoint(1))
      .andExpect(content().json(asJson(defaultRules.getFirst(), objectMapper), JsonCompareMode.STRICT));
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_negative_notFound() {
    var request = new LinkingRulePatchRequest().id(MAX_VALUE).autoLinkingEnabled(false);
    tryPatch(linkingRulesEndpoint(MAX_VALUE), request)
      .andExpect(status().isNotFound())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("LinkingRuleNotFoundException")))
      .andExpect(errorCodeMatch(is(ErrorType.NOT_FOUND_ERROR.getValue())))
      .andExpect(errorMessageMatch(is(String.format("Linking rule with ID [%s] was not found", MAX_VALUE))));
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_negative_requestBodyValidation() {
    var request = new LinkingRulePatchRequest().autoLinkingEnabled(false);
    tryPatch(linkingRulesEndpoint(MAX_VALUE), request)
      .andExpect(status().isUnprocessableContent())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("RequestBodyValidationException")))
      .andExpect(errorCodeMatch(is(ErrorType.VALIDATION_ERROR.getValue())))
      .andExpect(errorParameterMatch(is("id")))
      .andExpect(errorMessageMatch(is(String.format("Request should have id = %s", MAX_VALUE))));
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_negative_invalidBibField() {
    var request = new LinkingRulePatchRequest()
      .id(1)
      .authoritySubfields(Set.of("a", "b"));

    tryPatch(linkingRulesEndpoint(1), request)
      .andExpect(status().isUnprocessableContent())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("RequestBodyValidationException")))
      .andExpect(errorCodeMatch(is(ErrorType.VALIDATION_ERROR.getValue())))
      .andExpect(errorParameterMatch(is("bibField")))
      .andExpect(errorMessageMatch(is("Subfields could be updated only for 6XX fields.")));
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_negative_missingRequiredSubfieldA() {
    var request = new LinkingRulePatchRequest()
      .id(10)
      .authoritySubfields(Set.of("b", "c"));

    tryPatch(linkingRulesEndpoint(10), request)
      .andExpect(status().isUnprocessableContent())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("RequestBodyValidationException")))
      .andExpect(errorCodeMatch(is(ErrorType.VALIDATION_ERROR.getValue())))
      .andExpect(errorParameterMatch(is("authoritySubfields")))
      .andExpect(errorMessageMatch(is("Subfield 'a' is required.")));
  }

  @Test
  @SneakyThrows
  void patchLinkingRulesById_negative_invalidSubfieldCharacters() {
    var request = new LinkingRulePatchRequest()
      .id(10)
      .authoritySubfields(Set.of("a", "9", "x")); // '9' is invalid as per SubfieldValidation

    tryPatch(linkingRulesEndpoint(10), request)
      .andExpect(status().isUnprocessableContent())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("RequestBodyValidationException")))
      .andExpect(errorCodeMatch(is(ErrorType.VALIDATION_ERROR.getValue())))
      .andExpect(errorParameterMatch(is("authoritySubfields")))
      .andExpect(errorMessageMatch(is("Invalid subfield value.")));
  }
}
