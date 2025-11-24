package org.folio.entlinks.controller.delegate.suggestion;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.controller.converter.SourceContentMapper;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.domain.dto.StrippedParsedRecordCollection;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.integration.dto.AuthorityParsedContent;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.SourceParsedContent;
import org.folio.entlinks.service.consortium.ConsortiumTenantExecutor;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.LinksSuggestionsService;
import org.folio.entlinks.service.links.model.AuthorityFieldConstants;
import org.springframework.stereotype.Service;

/**
 * Base class for link suggestions delegates.
 * Inheritors are supposed to define authority field to extract from incoming data
 * and to search authorities by.
 * T generic is intended to define authority field data type.
 * */
@Log4j2
@Service
@RequiredArgsConstructor
public abstract class LinksSuggestionsServiceDelegateBase<T> implements LinksSuggestionServiceDelegate {

  private final InstanceAuthorityLinkingRulesService linkingRulesService;
  private final LinksSuggestionsService suggestionService;
  private final SourceStorageClient sourceStorageClient;
  private final SourceContentMapper contentMapper;
  private final ConsortiumTenantExecutor executor;

  public ParsedRecordContentCollection suggestLinksForMarcRecords(
      ParsedRecordContentCollection contentCollection, Boolean ignoreAutoLinkingEnabled) {
    log.info("{}: Links suggestion started for {} bibs",
      this.getClass().getSimpleName(), contentCollection.getRecords().size());
    var rules = rulesToBibFieldMap(linkingRulesService.getLinkingRules());
    var marcBibsContent = contentMapper.convertToParsedContent(contentCollection);

    var authoritySearchIds = extractIdsOfLinkableFields(marcBibsContent, rules, ignoreAutoLinkingEnabled);
    log.info("{} authority search ids was extracted", authoritySearchIds.size());

    var authoritiesMap = findExistingAuthorities(authoritySearchIds);

    if (!authoritiesMap.isEmpty()) {
      var marcAuthoritiesContent = getAuthoritiesParsedContent(authoritiesMap);
      suggestionService.fillLinkDetailsWithSuggestedAuthorities(marcBibsContent, marcAuthoritiesContent, rules,
          getSearchSubfield(), ignoreAutoLinkingEnabled);
    } else {
      suggestionService.fillErrorDetailsWithNoSuggestions(marcBibsContent, getSearchSubfield());
    }
    log.info("suggestLinksForMarcRecords: Links suggestion finished for {} bibs : {}",
        marcBibsContent.size(), marcBibsContent);
    return contentMapper.convertToParsedContentCollection(marcBibsContent);
  }

  protected abstract char getSearchSubfield();

  protected abstract Map<String, List<Authority>> findExistingAuthorities(Set<T> ids);

  protected abstract T extractId(Authority authorityData);

  private List<AuthorityParsedContent> getAuthoritiesParsedContent(Map<String, List<Authority>> authorities) {
    var shadowCopyAuthorities = authorities.get("shared");
    var localCopyAuthorities = authorities.get("local");
    var marcRecordsForShadowCopyAuthorities = isEmpty(shadowCopyAuthorities) ? new StrippedParsedRecordCollection() :
        executor.executeAsCentralTenant(() -> fetchAuthorityParsedRecords(shadowCopyAuthorities));
    var marcRecordsForLocalCopyAuthorities = fetchAuthorityParsedRecords(localCopyAuthorities);
    log.info("getAuthoritiesParsedContent:: fetched {} marc records for {} shadow copy authorities and {}"
            + " marc records for {} local copy authorities",
        marcRecordsForShadowCopyAuthorities.getRecords().size(),
        isEmpty(shadowCopyAuthorities) ? 0 : shadowCopyAuthorities.size(),
        marcRecordsForLocalCopyAuthorities.getRecords().size(),
        isEmpty(localCopyAuthorities) ? 0 : localCopyAuthorities.size());
    return Stream.of(
        contentMapper.convertToAuthorityParsedContent(marcRecordsForShadowCopyAuthorities, shadowCopyAuthorities),
        contentMapper.convertToAuthorityParsedContent(marcRecordsForLocalCopyAuthorities, localCopyAuthorities)
    )
        .flatMap(List::stream)
        .toList();
  }

  private StrippedParsedRecordCollection fetchAuthorityParsedRecords(List<Authority> authorities) {
    if (isEmpty(authorities)) {
      return new StrippedParsedRecordCollection(Collections.emptyList(), 0);
    }

    var ids = authorities.stream().map(Authority::getId).collect(Collectors.toSet());
    var authorityFetchRequest = sourceStorageClient.buildBatchFetchRequestForAuthority(ids,
        AuthorityFieldConstants.MIN_FIELD,
        AuthorityFieldConstants.MAX_FIELD);
    return sourceStorageClient.fetchParsedRecordsInBatch(authorityFetchRequest);
  }

  private Set<T> extractIdsOfLinkableFields(List<SourceParsedContent> contentCollection,
                                            Map<String, List<InstanceAuthorityLinkingRule>> rules,
                                            Boolean ignoreAutoLinkingEnabled) {
    return contentCollection.stream()
      .flatMap(bibRecord -> bibRecord.getFields().stream())
      .filter(field -> isAutoLinkingEnabled(field, rules, ignoreAutoLinkingEnabled))
      .map(this::extractIds)
      .filter(CollectionUtils::isNotEmpty)
      .flatMap(Set::stream)
      .collect(Collectors.toSet());
  }

  protected abstract Set<T> extractIds(FieldParsedContent field);

  private boolean isAutoLinkingEnabled(FieldParsedContent field, Map<String, List<InstanceAuthorityLinkingRule>> rules,
                                       Boolean ignoreAutoLinkingEnabled) {
    var rulesForField = rules.get(field.getTag());
    if (isNull(rulesForField)) {
      return false;
    }

    if (Boolean.TRUE.equals(ignoreAutoLinkingEnabled)
        || rulesForField.stream().anyMatch(InstanceAuthorityLinkingRule::getAutoLinkingEnabled)) {
      return true;
    }

    suggestionService.fillErrorDetailsWithDisabledAutoLinking(field, getSearchSubfield());
    return false;
  }

  private Map<String, List<InstanceAuthorityLinkingRule>> rulesToBibFieldMap(List<InstanceAuthorityLinkingRule> rules) {
    return rules.stream().collect(groupingBy(InstanceAuthorityLinkingRule::getBibField));
  }
}
