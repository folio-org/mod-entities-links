package org.folio.entlinks.service.links;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.domain.repository.LinkingRulesRepository;
import org.folio.entlinks.exception.LinkingRuleNotFoundException;
import org.folio.entlinks.service.links.validator.LinkingRuleValidator;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceAuthorityLinkingRulesService {

  private final LinkingRulesRepository repository;
  private final LinkingRuleValidator validator;

  public List<InstanceAuthorityLinkingRule> getLinkingRules() {
    log.info("Loading linking rules");
    return repository.findAll(Sort.by("id").ascending());
  }

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
  public void updateLinkingRule(Integer ruleId, InstanceAuthorityLinkingRule linkingRulePatch) {
    log.info("Updating linking rule [ruleId: {}, change: {}]", ruleId, linkingRulePatch);
    var existingRule = getLinkingRule(ruleId);

    updateAutoLinkingIfPresent(existingRule, linkingRulePatch);
    updateSubfieldsIfPresent(existingRule, linkingRulePatch);

    repository.save(existingRule);
  }

  private void updateAutoLinkingIfPresent(InstanceAuthorityLinkingRule existing,
                                          InstanceAuthorityLinkingRule patch) {
    if (patch.getAutoLinkingEnabled() != null) {
      existing.setAutoLinkingEnabled(patch.getAutoLinkingEnabled());
    }
  }

  private void updateSubfieldsIfPresent(InstanceAuthorityLinkingRule existing,
                                        InstanceAuthorityLinkingRule patch) {
    char[] newSubfields = patch.getAuthoritySubfields();
    if (newSubfields != null && newSubfields.length > 0) {
      validator.validateSubfieldsUpdate(patch, existing);
      existing.setAuthoritySubfields(newSubfields);
    }
  }
}

