package org.folio.entlinks.controller.delegate.suggestion;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.controller.converter.SourceContentMapper;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.repository.AuthorityJdbcRepository;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.ParsedSubfield;
import org.folio.entlinks.service.consortium.ConsortiumTenantExecutor;
import org.folio.entlinks.service.consortium.UserTenantsService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.LinksSuggestionsService;
import org.folio.entlinks.utils.FieldUtils;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class LinksSuggestionsByAuthorityNaturalId extends LinksSuggestionsServiceDelegateBase<String> {

  private final AuthorityRepository authorityRepository;
  private final UserTenantsService userTenantsService;
  private final FolioExecutionContext context;
  private final AuthorityJdbcRepository authorityJdbcRepository;

  public LinksSuggestionsByAuthorityNaturalId(InstanceAuthorityLinkingRulesService linkingRulesService,
                                              LinksSuggestionsService suggestionService,
                                              AuthorityRepository repository,
                                              SourceStorageClient sourceStorageClient,
                                              SourceContentMapper contentMapper,
                                              ConsortiumTenantExecutor executor,
                                              UserTenantsService userTenantsService,
                                              FolioExecutionContext context,
                                              AuthorityJdbcRepository authorityJdbcRepository) {
    super(linkingRulesService, suggestionService, sourceStorageClient, contentMapper, executor);
    this.authorityRepository = repository;
    this.userTenantsService = userTenantsService;
    this.context = context;
    this.authorityJdbcRepository = authorityJdbcRepository;
  }

  @Override
  protected char getSearchSubfield() {
    return FieldUtils.NATURAL_ID_SUBFIELD_CODE;
  }

  @Override
  protected Map<String, List<Authority>> findExistingAuthorities(Set<String> ids) {
    Map<String, List<Authority>> authoritiesMap = new HashMap<>();
    var authorities = authorityRepository.findByNaturalIdInAndDeletedFalse(ids);
    if (!authorities.isEmpty() && authorities.size() == ids.size()) {
      authoritiesMap.put("local", authorities);
      authoritiesMap.put("shared", Collections.emptyList());
      return authoritiesMap;
    }
    var centralTenant = userTenantsService.getCentralTenant(context.getTenantId());
    if (centralTenant.isEmpty() || centralTenant.get().equals(context.getTenantId())) {
      authoritiesMap.put("local", authorities);
      authoritiesMap.put("shared", Collections.emptyList());
      return authoritiesMap;
    }
    // logic to fetch authorities from central tenant as current tenant is member of consortium
    if (authorities.isEmpty()) {
      var sharedAuthorities = authorityJdbcRepository.findByNaturalIdInAndDeletedFalse(ids, centralTenant.get());
      authoritiesMap.put("local", Collections.emptyList());
      authoritiesMap.put("shared", sharedAuthorities);
      return authoritiesMap;
    }
    authoritiesMap.put("local", authorities);
    var authoritiesIds = authorities.stream()
        .map(authority -> authority.getId().toString())
        .collect(Collectors.toSet());
    var potentialSharedAuthorities = ids.stream()
        .filter(id -> !authoritiesIds.contains(id))
        .collect(Collectors.toSet());

    if (!potentialSharedAuthorities.isEmpty()) {
      var sharedAuthorities = authorityJdbcRepository.findByNaturalIdInAndDeletedFalse(potentialSharedAuthorities,
          centralTenant.get());
      authoritiesMap.put("shared", sharedAuthorities);
    }
    return authoritiesMap;
  }

  @Override
  protected String extractId(Authority authority) {
    return authority.getNaturalId();
  }

  @Override
  protected Set<String> extractIds(FieldParsedContent field) {
    var naturalIds = new HashSet<String>();
    var zeroValues = field.getNaturalIdSubfields();
    if (isNotEmpty(zeroValues)) {
      naturalIds.addAll(zeroValues.stream()
        .map(ParsedSubfield::value)
        .map(FieldUtils::trimSubfield0Value)
        .collect(Collectors.toSet()));
    }
    if (nonNull(field.getLinkDetails()) && !isEmpty(field.getLinkDetails().getAuthorityNaturalId())) {
      naturalIds.add(field.getLinkDetails().getAuthorityNaturalId());
    }
    return naturalIds;
  }
}
