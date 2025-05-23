package org.folio.entlinks.service.links;

import static java.util.Collections.singletonList;
import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_LINKING_RULES_CACHE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.repository.LinkingRulesRepository;
import org.folio.entlinks.exception.LinkingRuleNotFoundException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.tenant.domain.dto.Parameter;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceAuthorityLinkingRulesService {

  private static final String MIN_AVAILABLE_AUTHORITY_FIELD = "100";
  private static final String MAX_AVAILABLE_AUTHORITY_FIELD = "155";
  private static final char REQUIRED_SUBFIELD = 'a';
  private static final char MIN_LETTER_SUBFIELD = 'a';
  private static final char MAX_LETTER_SUBFIELD = 'z';
  private static final char MIN_DIGIT_SUBFIELD = '1';
  private static final char MAX_DIGIT_SUBFIELD = '8';
  private final LinkingRulesRepository repository;

  @Cacheable(cacheNames = AUTHORITY_LINKING_RULES_CACHE,
             key = "@folioExecutionContext.tenantId",
             unless = "#result.isEmpty()")
  public List<InstanceAuthorityLinkingRule> getLinkingRules() {
    log.info("Loading linking rules");
    return repository.findAll(Sort.by("id").ascending());
  }

  @Cacheable(cacheNames = AUTHORITY_LINKING_RULES_CACHE,
             key = "@folioExecutionContext.tenantId + ':' + #authorityField", unless = "#result.isEmpty()")
  public List<InstanceAuthorityLinkingRule> getLinkingRulesByAuthorityField(String authorityField) {
    log.info("Loading linking rules for [authorityField: {}]", authorityField);
    return repository.findByAuthorityField(authorityField);
  }

  public InstanceAuthorityLinkingRule getLinkingRule(Integer ruleId) {
    log.info("Loading linking rule [ruleId: {}]", ruleId);
    return repository.findById(ruleId)
      .orElseThrow(() -> new LinkingRuleNotFoundException(ruleId));
  }

  @Transactional
  @CacheEvict(cacheNames = AUTHORITY_LINKING_RULES_CACHE, allEntries = true)
  public void patchLinkingRule(Integer ruleId, InstanceAuthorityLinkingRule linkingRulePatch) {
    log.info("Patch linking rule [ruleId: {}, change: {}]", ruleId, linkingRulePatch);
    var existedLinkingRule = repository.findById(ruleId)
      .orElseThrow(() -> new LinkingRuleNotFoundException(ruleId));
    // only autoLinkingEnabled flag and authoritySubfields are allowed to be updated
    if (linkingRulePatch.getAutoLinkingEnabled() != null) {
      existedLinkingRule.setAutoLinkingEnabled(linkingRulePatch.getAutoLinkingEnabled());
    }
    if (linkingRulePatch.getAuthoritySubfields() != null && linkingRulePatch.getAuthoritySubfields().length > 0) {
      validateAuthoritySubfieldUpdate(linkingRulePatch, existedLinkingRule);
      existedLinkingRule.setAuthoritySubfields(linkingRulePatch.getAuthoritySubfields());
    }
    repository.save(existedLinkingRule);
  }

  public String getMinAuthorityField() {
    return MIN_AVAILABLE_AUTHORITY_FIELD;
  }

  public String getMaxAuthorityField() {
    return MAX_AVAILABLE_AUTHORITY_FIELD;
  }

  private void validateAuthoritySubfieldUpdate(InstanceAuthorityLinkingRule linkingRulePatch,
                                               InstanceAuthorityLinkingRule existedLinkingRule) {
    validateBibFieldPrefix(existedLinkingRule);
    validateRequiredSubfieldA(linkingRulePatch);
    validateSubfieldCharacters(linkingRulePatch);

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
      boolean isValidLetter = subfield >= MIN_LETTER_SUBFIELD && subfield <= MAX_LETTER_SUBFIELD;
      boolean isValidDigit = subfield >= MIN_DIGIT_SUBFIELD && subfield <= MAX_DIGIT_SUBFIELD;

      if (!isValidLetter && !isValidDigit) {
        invalidSubfields.add(new Parameter()
          .key("authoritySubfields")
          .value(String.valueOf(subfield)));
      }
    }
    return invalidSubfields;
  }

  private void validateRequiredSubfieldA(InstanceAuthorityLinkingRule rule) {
    if (rule.getAuthoritySubfields()[0] != REQUIRED_SUBFIELD) {
      throw new RequestBodyValidationException(
        "Subfield 'a' is required.",
        createSubfieldsParameter(rule.getAuthoritySubfields())
      );
    }
  }

  private List<Parameter> createSubfieldsParameter(char[] subfields) {
    return singletonList(new Parameter()
      .key("authoritySubfields")
      .value(Arrays.toString(subfields)));
  }

  private void validateBibFieldPrefix(InstanceAuthorityLinkingRule linkingRule) {
    var bibField = linkingRule.getBibField();
    if (!bibField.startsWith("6")) {
      throw new RequestBodyValidationException(
        "Subfields could be updated only for 6XX fields.",
        singletonList(new Parameter()
          .key("bibField")
          .value(bibField))
      );
    }
  }

}
