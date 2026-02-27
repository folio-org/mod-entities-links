package org.folio.entlinks.service.links;

import static java.util.Objects.isNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.entlinks.config.constants.ErrorCode.DISABLED_AUTO_LINKING;
import static org.folio.entlinks.config.constants.ErrorCode.MORE_THAN_ONE_SUGGESTIONS;
import static org.folio.entlinks.config.constants.ErrorCode.NO_SUGGESTIONS;
import static org.folio.entlinks.domain.dto.LinkStatus.ERROR;
import static org.folio.entlinks.domain.dto.LinkStatus.NEW;
import static org.folio.entlinks.utils.FieldUtils.createIdSubfield;
import static org.folio.entlinks.utils.FieldUtils.createNaturalIdSubfield;
import static org.folio.entlinks.utils.FieldUtils.getSubfield0Value;
import static org.folio.entlinks.utils.FieldUtils.isSystemSubfield;

import com.google.common.primitives.Chars;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.config.constants.ErrorCode;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.ParsedSubfield;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.entlinks.utils.FieldUtils;
import org.springframework.stereotype.Service;

/**
 * Service to handle suggestions of authority links for bibliographic records.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class LinksSuggestionsService {

  private final AuthorityRuleValidationService authorityRuleValidationService;
  private final AuthoritySourceFileService authoritySourceFileService;

  /**
   * Validate bib-authority fields by linking rules and fill bib fields with suggested links.
   *
   * @param marcBibsContent        list of bib records {@link SourceParsedContent}
   * @param marcAuthoritiesContent list of authorities {@link AuthorityParsedContent} that can be suggested as link
   * @param rules                  linking rules: Key - bib tag, Value - list of {@link InstanceAuthorityLinkingRule}
   * @param linkingMatchSubfield   bib field's subfield code to use as a matching point for links
   */
  public void fillLinkDetailsWithSuggestedAuthorities(List<SourceParsedContent> marcBibsContent,
                                                      List<AuthorityParsedContent> marcAuthoritiesContent,
                                                      Map<String, List<InstanceAuthorityLinkingRule>> rules,
                                                      char linkingMatchSubfield,
                                                      boolean ignoreAutoLinkingEnabled) {
    marcBibsContent.stream()
      .flatMap(bibContent -> bibContent.getFields().stream())
      .forEach(bibField -> Optional.ofNullable(rules.get(bibField.getTag()))
        .ifPresent(bibFieldRules -> suggestAuthorityForBibFields(
          List.of(bibField), marcAuthoritiesContent, bibFieldRules, linkingMatchSubfield, ignoreAutoLinkingEnabled)));
  }

  /**
   * Fill bib fields with no suggestions error detail, if it contains subfields $0.
   *
   * @param marcBibsContent list of bib records {@link SourceParsedContent}
   */
  public void fillErrorDetailsWithNoSuggestions(List<SourceParsedContent> marcBibsContent,
                                                char linkingMatchSubfield) {
    marcBibsContent.stream()
      .flatMap(bibContent -> bibContent.getFields().stream())
      .filter(fieldContent -> fieldContent.hasSubfield(linkingMatchSubfield))
      .filter(fieldContent -> Optional.ofNullable(fieldContent.getLinkDetails())
        .map(linkDetails -> isEmpty(linkDetails.getErrorCause()))
        .orElse(true))
      .forEach(bibField -> bibField.setLinkDetails(getErrorDetails(NO_SUGGESTIONS)));
  }

  /**
   * Fill bib fields with no suggestions error detail, if it contains subfields.
   *
   * @param field list of bib records {@link FieldParsedContent}
   */
  public void fillErrorDetailsWithDisabledAutoLinking(FieldParsedContent field,
                                                      char linkingMatchSubfield) {
    if (field.hasSubfield(linkingMatchSubfield)) {
      field.setLinkDetails(getErrorDetails(DISABLED_AUTO_LINKING));
    }
  }

  private void suggestAuthorityForBibFields(List<FieldParsedContent> bibFields,
                                            List<AuthorityParsedContent> marcAuthoritiesContent,
                                            List<InstanceAuthorityLinkingRule> rules,
                                            char linkingMatchSubfield,
                                            boolean ignoreAutoLinkingEnabled) {
    if (isNotEmpty(rules) && isNotEmpty(bibFields)) {
      for (FieldParsedContent bibField : bibFields) {
        if (isBibFieldLinkable(bibField, linkingMatchSubfield)) {
          suggestAuthorityForBibField(bibField, marcAuthoritiesContent, rules, ignoreAutoLinkingEnabled);
        }
      }
    }
  }

  private boolean isBibFieldLinkable(FieldParsedContent bibField, char linkingMatchSubfield) {
    var linkDetails = bibField.getLinkDetails();
    return bibField.hasSubfield(linkingMatchSubfield)
           && (isNull(linkDetails) || linkDetails.getStatus() != NEW);
  }

  private void suggestAuthorityForBibField(FieldParsedContent bibField,
                                           List<AuthorityParsedContent> marcAuthoritiesContent,
                                           List<InstanceAuthorityLinkingRule> rules,
                                           boolean ignoreAutoLinkingEnabled) {
    var suitableRules = rules.stream()
      .filter(rule -> rule.getAutoLinkingEnabled() || ignoreAutoLinkingEnabled)
      .toList();
    if (suitableRules.isEmpty()) {
      var errorDetails = getErrorDetails(DISABLED_AUTO_LINKING);
      bibField.setLinkDetails(errorDetails);
      log.info("Field {}: auto linking feature is disabled", bibField.getTag());
      return;
    }

    LinkDetails errorDetails = null;
    for (var rule : suitableRules) {
      var suitableAuthorities = filterSuitableAuthorities(bibField, marcAuthoritiesContent, rule);
      if (suitableAuthorities.size() == 1) {
        var authority = suitableAuthorities.getFirst();
        var linkDetails = getLinkDetails(bibField, authority, rule);
        actualizeBibSubfields(bibField, authority, rule);
        bibField.setLinkDetails(linkDetails);
        log.info("Field {}: Authority {} was suggested", bibField.getTag(), authority.getId());
        return;
      } else if (suitableAuthorities.isEmpty()) {
        errorDetails = getErrorDetails(NO_SUGGESTIONS);
        log.info("Field {}: No authorities to suggest", bibField.getTag());
      } else {
        errorDetails = getErrorDetails(MORE_THAN_ONE_SUGGESTIONS);
        log.info("Field {}: More than one authority to suggest", bibField.getTag());
      }
    }

    bibField.setLinkDetails(errorDetails);
  }

  private LinkDetails getLinkDetails(FieldParsedContent bibField,
                                     AuthorityParsedContent authority,
                                     InstanceAuthorityLinkingRule rule) {
    var linkDetails = bibField.getLinkDetails();
    if (isNull(linkDetails)) {
      linkDetails = new LinkDetails();
      linkDetails.setStatus(NEW);
    }
    linkDetails.setLinkingRuleId(rule.getId());
    linkDetails.setAuthorityId(authority.getId());
    linkDetails.setAuthorityNaturalId(authority.getNaturalId());
    return linkDetails;
  }

  private LinkDetails getErrorDetails(ErrorCode errorCode) {
    return new LinkDetails().status(ERROR).errorCause(errorCode.getCode());
  }

  /**
   * Updates the subfields of a bibliographic field based on authority data and specified
   * linking rules.
   *
   * @param bibField  the bibliographic field whose subfields are to be actualized
   * @param authority the authority data used to update the bibliographic subfields
   * @param rule      the rule specifying how the linking between bibliographic and authority
   *                  subfields should be performed
   */
  private void actualizeBibSubfields(FieldParsedContent bibField,
                                     AuthorityParsedContent authority,
                                     InstanceAuthorityLinkingRule rule) {

    var bibSubfields = bibField.getSubfieldList();
    var authorityField = authority.getFieldByTag(rule.getAuthorityField());
    if (authorityField.isEmpty()) {
      return;
    }

    var controlledSubfieldCodes = Chars.asList(rule.getAuthoritySubfields());
    var subfieldModifications = rule.getSubfieldModifications();
    var controlledSubfields = getControlledSubfields(authorityField.get(), controlledSubfieldCodes,
      subfieldModifications);
    var systemSubfields = getSystemSubfields(authority);
    var uncontrolledSubfields = getUncontrolledSubfields(bibSubfields, controlledSubfieldCodes, subfieldModifications);

    var newBibSubfields = new ArrayList<ParsedSubfield>();
    newBibSubfields.addAll(controlledSubfields);
    newBibSubfields.addAll(systemSubfields);
    newBibSubfields.addAll(uncontrolledSubfields);
    bibField.setSubfieldList(newBibSubfields);
  }

  private List<ParsedSubfield> getControlledSubfields(FieldParsedContent authorityField,
                                                      List<Character> controlledSubfieldCodes,
                                                      List<SubfieldModification> subfieldModifications) {
    List<ParsedSubfield> controlledSubfields = new ArrayList<>();

    var authoritySubfields = new ArrayList<>(authorityField.getSubfieldList());
    if (isNotEmpty(subfieldModifications)) {
      subfieldModifications.forEach(modification -> {
        var subfields = authorityField.getSubfields(modification.getSource().charAt(0));
        subfields.stream()
          .map(subfield -> new ParsedSubfield(modification.getTarget().charAt(0), subfield.value()))
          .forEach(controlledSubfields::add);
        authoritySubfields.removeAll(subfields);
      });
    }

    authoritySubfields.forEach(subfield -> {
      if (controlledSubfieldCodes.contains(subfield.code())) {
        controlledSubfields.add(subfield);
      }
    });
    return controlledSubfields;
  }

  private List<ParsedSubfield> getUncontrolledSubfields(List<ParsedSubfield> bibSubfields,
                                                        List<Character> controlledSubfieldCodes,
                                                        List<SubfieldModification> subfieldModifications) {
    var bibSubfieldsUncontrolled = new ArrayList<>(bibSubfields);
    var modificationSubfieldCodes = subfieldModifications == null
                                    ? Collections.<Character>emptySet()
                                    : subfieldModifications.stream()
                                      .map(subfieldModification -> subfieldModification.getTarget().charAt(0))
                                      .collect(Collectors.toSet());
    bibSubfieldsUncontrolled.removeIf(subfield -> controlledSubfieldCodes.contains(subfield.code())
                                                  || modificationSubfieldCodes.contains(subfield.code())
                                                  || isSystemSubfield(subfield.code()));
    return bibSubfieldsUncontrolled;
  }

  private List<ParsedSubfield> getSystemSubfields(AuthorityParsedContent authority) {
    var bibSubfieldsSystem = new ArrayList<ParsedSubfield>();
    bibSubfieldsSystem.add(createNaturalIdSubfield(getSubfieldZeroValue(authority)));
    bibSubfieldsSystem.add(createIdSubfield(authority.getId().toString()));
    return bibSubfieldsSystem;
  }

  private String getSubfieldZeroValue(AuthorityParsedContent authority) {
    var sourceFileId = authority.getSourceFileId();
    if (isNull(sourceFileId)) {
      return authority.getNaturalId();
    }

    var authoritySourceFile = authoritySourceFileService.getById(sourceFileId);
    return getSubfield0Value(authority.getNaturalId(), authoritySourceFile);
  }

  private List<AuthorityParsedContent> filterSuitableAuthorities(FieldParsedContent bibField,
                                                                 List<AuthorityParsedContent> marcAuthoritiesContent,
                                                                 InstanceAuthorityLinkingRule rule) {
    return marcAuthoritiesContent.stream()
      .filter(authorityContent -> validateSubfields(authorityContent, bibField))
      .filter(authorityContent -> authorityRuleValidationService.validateAuthorityFields(authorityContent, rule))
      .toList();
  }

  private boolean validateSubfields(AuthorityParsedContent marcAuthorityContent, FieldParsedContent bibField) {
    return isNaturalIdMatched(marcAuthorityContent, bibField) || isIdMatched(marcAuthorityContent, bibField);
  }

  private boolean isNaturalIdMatched(AuthorityParsedContent marcAuthorityContent,
                                     FieldParsedContent bibField) {
    return Optional.ofNullable(bibField.getNaturalIdSubfields())
      .map(subfields -> subfields.stream()
        .filter(Objects::nonNull)
        .map(ParsedSubfield::value)
        .map(FieldUtils::trimSubfield0Value)
        .anyMatch(zeroValue -> zeroValue.equals(marcAuthorityContent.getNaturalId())))
      .orElse(false);
  }

  private boolean isIdMatched(AuthorityParsedContent marcAuthorityContent, FieldParsedContent bibField) {
    return Optional.ofNullable(bibField.getIdSubfields())
      .map(subfields -> subfields.stream()
        .filter(Objects::nonNull)
        .map(ParsedSubfield::value)
        .anyMatch(nineValue -> nineValue.equals(marcAuthorityContent.getId().toString())))
      .orElse(false);
  }
}
