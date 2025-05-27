package org.folio.entlinks.service.links.validator;

import static org.folio.entlinks.utils.ErrorUtils.createErrorParameter;
import static org.folio.entlinks.utils.ErrorUtils.createErrorParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.tenant.domain.dto.Parameter;
import org.springframework.stereotype.Component;

/**
 * Validator responsible for validating updates to instance-authority linking rules.
 * Ensures that the rules conform to specified constraints and standards by
 * performing validations on bibliographic fields, required subfields, and
 * character constraints within subfields.
 */
@Component
@RequiredArgsConstructor
public class LinkingRuleValidator {

  public void validateSubfieldsUpdate(InstanceAuthorityLinkingRule patch,
                                      InstanceAuthorityLinkingRule existing) {
    validateBibField(existing);
    validateRequiredSubfieldA(patch);
    validateSubfieldCharacters(patch);
  }

  /**
   * Validates that the bibliographic field of the given linking rule starts with the digit '6',
   * indicating that it pertains to the 6XX range of MARC fields. If the validation fails,
   * a {@link RequestBodyValidationException} is thrown.
   *
   * @param rule the {@link InstanceAuthorityLinkingRule} whose bibliographic field is being validated
   * @throws RequestBodyValidationException if the bibliographic field does not start with '6'
   */
  private void validateBibField(InstanceAuthorityLinkingRule rule) {
    if (!rule.getBibField().startsWith("6")) {
      throw new RequestBodyValidationException(
        "Subfields could be updated only for 6XX fields.",
        createErrorParameters("bibField", rule.getBibField()));
    }
  }

  /**
   * Validates that the first authority subfield of the provided linking rule is subfield 'a'.
   * If subfield 'a' is not present, a {@link RequestBodyValidationException} is thrown.
   *
   * @param rule the {@link InstanceAuthorityLinkingRule} containing authority subfields to validate
   * @throws RequestBodyValidationException if subfield 'a' is missing
   */
  private void validateRequiredSubfieldA(InstanceAuthorityLinkingRule rule) {
    if (!SubfieldValidation.isRequiredSubfield(rule.getAuthoritySubfields()[0])) {
      throw new RequestBodyValidationException(
        "Subfield 'a' is required.",
        createErrorParameters("authoritySubfields", Arrays.toString(rule.getAuthoritySubfields())));
    }
  }

  /**
   * Validates the characters in the authority subfields of the given linking rule.
   * Ensures that all characters conform to expected constraints, otherwise throws
   * a {@link RequestBodyValidationException}.
   *
   * @param linkingRule the {@link InstanceAuthorityLinkingRule} whose authority subfields are being validated
   * @throws RequestBodyValidationException if invalid subfield characters are detected
   */
  private void validateSubfieldCharacters(InstanceAuthorityLinkingRule linkingRule) {
    var invalidSubfields = findInvalidSubfields(linkingRule.getAuthoritySubfields());
    if (!invalidSubfields.isEmpty()) {
      throw new RequestBodyValidationException("Invalid subfield value.", invalidSubfields);
    }
  }

  private List<Parameter> findInvalidSubfields(char[] subfields) {
    List<Parameter> invalidSubfields = new ArrayList<>();
    for (char subfield : subfields) {

      if (!SubfieldValidation.isValidSubfield(subfield)) {
        invalidSubfields.add(createErrorParameter("authoritySubfields", String.valueOf(subfield)));
      }
    }
    return invalidSubfields;
  }

}
