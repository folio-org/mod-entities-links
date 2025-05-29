package org.folio.entlinks.service.links.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class LinkingRuleValidatorTest {

  private final LinkingRuleValidator linkingRuleValidator = new LinkingRuleValidator();

  @Test
  void shouldThrowExceptionWhenBibFieldPrefixInvalid() {
    var existingRule = InstanceAuthorityLinkingRule.builder()
      .bibField("500")
      .authoritySubfields(new char[] {'a', 'b'})
      .build();

    var patchRule = InstanceAuthorityLinkingRule.builder().build();

    assertThrows(RequestBodyValidationException.class,
      () -> linkingRuleValidator.validateSubfieldsUpdate(patchRule, existingRule));
  }

  @Test
  void shouldThrowExceptionWhenRequiredSubfieldIsMissing() {
    var patchRule = InstanceAuthorityLinkingRule.builder()
      .authoritySubfields(new char[] {'b', 'c'})
      .build();

    var existingRule = InstanceAuthorityLinkingRule.builder()
      .bibField("600")
      .authoritySubfields(new char[] {'b', 'c'})
      .build();

    assertThrows(RequestBodyValidationException.class,
      () -> linkingRuleValidator.validateSubfieldsUpdate(patchRule, existingRule));
  }

  @Test
  void shouldThrowExceptionWhenInvalidSubfieldCharactersPresent() {
    var patchRule = InstanceAuthorityLinkingRule.builder()
      .authoritySubfields(new char[] {'a', '1', '?'})
      .build();

    var existingRule = InstanceAuthorityLinkingRule.builder()
      .bibField("650")
      .authoritySubfields(new char[] {'a', '1', '?'})
      .build();

    assertThrows(RequestBodyValidationException.class,
      () -> linkingRuleValidator.validateSubfieldsUpdate(patchRule, existingRule));
  }

  @Test
  void shouldPassValidationWhenSubfieldsAreValid() {
    var patchRule = InstanceAuthorityLinkingRule.builder()
      .authoritySubfields(new char[] {'a', 'b', 'c'})
      .build();

    var existingRule = InstanceAuthorityLinkingRule.builder()
      .bibField("650")
      .authoritySubfields(new char[] {'a', 'b', 'c'})
      .build();

    assertDoesNotThrow(() -> linkingRuleValidator.validateSubfieldsUpdate(patchRule, existingRule));
  }
}
