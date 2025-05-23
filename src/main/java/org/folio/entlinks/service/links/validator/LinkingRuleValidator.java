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

@Component
@RequiredArgsConstructor
public class LinkingRuleValidator {

  public void validateSubfieldsUpdate(InstanceAuthorityLinkingRule patch,
                                      InstanceAuthorityLinkingRule existing) {
    validateBibFieldPrefix(existing);
    validateRequiredSubfieldA(patch);
    validateSubfieldCharacters(patch);
  }

  private void validateBibFieldPrefix(InstanceAuthorityLinkingRule rule) {
    if (!rule.getBibField().startsWith("6")) {
      throw new RequestBodyValidationException(
        "Subfields could be updated only for 6XX fields.",
        createErrorParameters("bibField", rule.getBibField()));
    }
  }

  private void validateRequiredSubfieldA(InstanceAuthorityLinkingRule rule) {
    if (!SubfieldValidation.isRequiredSubfield(rule.getAuthoritySubfields()[0])) {
      throw new RequestBodyValidationException(
        "Subfield 'a' is required.",
        createErrorParameters("authoritySubfields", Arrays.toString(rule.getAuthoritySubfields())));
    }
  }

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
