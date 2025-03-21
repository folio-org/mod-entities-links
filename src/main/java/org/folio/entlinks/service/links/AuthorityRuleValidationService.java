package org.folio.entlinks.service.links;

import static java.util.Objects.isNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.dto.FieldContentValue;
import org.folio.entlinks.domain.dto.StrippedParsedRecord;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.service.links.model.AuthorityRuleValidationResult;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class AuthorityRuleValidationService {

  public AuthorityRuleValidationResult validateAuthorityData(Map<UUID, List<InstanceAuthorityLink>> linksByAuthorityId,
                                                             Map<UUID, Authority> authoritiesById,
                                                             Map<UUID, String> authorityNaturalIds,
                                                             List<StrippedParsedRecord> authoritySources) {
    var invalidLinks = new LinkedList<InstanceAuthorityLink>();
    var validAuthorityData = new HashSet<Authority>();

    for (var authority : authoritiesById.values()) {
      var authorityId = authority.getId();
      var naturalId = authorityNaturalIds.get(authorityId);
      var authorityRecord = findAuthorityById(authorityId, authoritySources);

      if (isNull(naturalId) || authorityRecord.isEmpty()) {
        invalidLinks.addAll(linksByAuthorityId.remove(authorityId));
      } else {
        var authorityLinks = linksByAuthorityId.get(authorityId);
        var invalidLinksForAuthority = removeValidAuthorityLinks(authorityRecord.get(), authorityLinks);

        if (!invalidLinksForAuthority.isEmpty()) {
          invalidLinks.addAll(invalidLinksForAuthority);
          authorityLinks.removeAll(invalidLinksForAuthority);
        }
        if (invalidLinksForAuthority.size() == authorityLinks.size()) {
          continue;
        }

        authority.setNaturalId(naturalId);
        validAuthorityData.add(authority);
      }
    }
    return new AuthorityRuleValidationResult(validAuthorityData, mapToValidLinkList(linksByAuthorityId), invalidLinks);
  }

  public boolean validateAuthorityFields(AuthorityParsedContent authorityContent, InstanceAuthorityLinkingRule rule) {
    log.info("validateAuthorityFields::Starting validation for AuthorityParsedContent {}", authorityContent.getId());
    var authorityFields = authorityContent.getFields().stream().filter(fieldParsedContent ->
      fieldParsedContent.getTag().equals(rule.getAuthorityField())).toList();

    if (validateAuthorityFields(authorityFields)) {
      var authorityField = authorityFields.getFirst();
      return validateAuthoritySubfieldsExistence(authorityField, rule);
    }
    return false;
  }

  public boolean validateAuthorityFields(StrippedParsedRecord authority, InstanceAuthorityLinkingRule rule) {
    log.info("validateAuthorityFields::Starting validation for StrippedParsedRecord {}", authority.getId());
    var authorityFields = authority.getParsedRecord().getContent().getFields().stream()
      .flatMap(fields -> fields.entrySet().stream())
      .filter(field -> rule.getAuthorityField().equals(field.getKey()))
      .map(Map.Entry::getValue)
      .toList();

    if (validateAuthorityFields(authorityFields)) {
      var authorityField = authorityFields.getFirst();
      return validateAuthoritySubfieldsExistence(authorityField, rule);
    }
    return false;
  }

  private boolean validateAuthorityFields(List<?> authorityFields) {
    if (isEmpty(authorityFields)) {
      log.warn("Validation failed: Authority does not contains linkable field");
      return false;
    }
    if (authorityFields.size() > 1) {
      log.warn("Validation failed: Authority contains more than one linkable fields");
      return false;
    }
    return true;
  }

  private List<InstanceAuthorityLink> removeValidAuthorityLinks(StrippedParsedRecord authority,
                                                                List<InstanceAuthorityLink> authorityLinks) {
    return authorityLinks.stream()
      .filter(link -> !validateAuthorityFields(authority, link.getLinkingRule()))
      .toList();
  }

  private boolean validateAuthoritySubfieldsExistence(FieldParsedContent authorityField,
                                                      InstanceAuthorityLinkingRule rule) {
    Predicate<String> containsSubfield = subfield -> authorityField.hasSubfield(subfield.charAt(0));

    return validateAuthoritySubfieldsExistence(rule, containsSubfield);
  }

  private boolean validateAuthoritySubfieldsExistence(FieldContentValue authorityField,
                                                      InstanceAuthorityLinkingRule rule) {
    var authoritySubfields = authorityField.getSubfields();
    Predicate<String> containsSubfield = subfield -> authoritySubfields.stream()
      .anyMatch(subfields -> subfields.containsKey(subfield));

    return validateAuthoritySubfieldsExistence(rule, containsSubfield);
  }

  private boolean validateAuthoritySubfieldsExistence(InstanceAuthorityLinkingRule rule, Predicate<String> contains) {
    var existValidation = rule.getSubfieldsExistenceValidations();
    if (isNotEmpty(existValidation)) {
      for (var subfieldExistence : existValidation.entrySet()) {
        var subfield = subfieldExistence.getKey();
        var doesItContains = contains.test(subfield);
        var shouldItContains = subfieldExistence.getValue();

        if (!shouldItContains.equals(doesItContains)) {
          logSubfieldExistenceValidationFailure(rule.getAuthorityField(), subfield, shouldItContains, doesItContains);
          return false;
        }
      }
    }
    return true;
  }

  private void logSubfieldExistenceValidationFailure(String authorityField, String subfield,
                                                     boolean shouldItContains, boolean doesItContains) {
    String shouldExist = shouldItContains ? "exist" : "not exist";
    String doesExist = doesItContains ? "does" : "does not";

    log.info("Subfield validation failed for authority field '{}'. Subfield '{}' should {}, but it {}",
      authorityField, subfield, shouldExist, doesExist);
  }

  private Optional<StrippedParsedRecord> findAuthorityById(UUID authorityId,
                                                           List<StrippedParsedRecord> authoritySources) {
    return authoritySources.stream()
      .filter(authorityRecord -> authorityRecord.getExternalIdsHolder().getAuthorityId().equals(authorityId))
      .findFirst();
  }

  private List<InstanceAuthorityLink> mapToValidLinkList(Map<UUID, List<InstanceAuthorityLink>> linksByAuthorityId) {
    return linksByAuthorityId.values().stream().flatMap(Collection::stream).toList();
  }
}
