package org.folio.entlinks.controller.delegate.suggestion;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.controller.converter.SourceContentMapper;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityBase;
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
public class LinksSuggestionsByAuthorityId extends LinksSuggestionsServiceDelegateBase<UUID> {

  private static final Pattern UUID_REGEX =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  private final AuthorityRepository authorityRepository;
  private final UserTenantsService userTenantsService;
  private final FolioExecutionContext context;
  private final AuthorityJdbcRepository authorityJdbcRepository;

  public LinksSuggestionsByAuthorityId(InstanceAuthorityLinkingRulesService linkingRulesService,
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
    return FieldUtils.ID_SUBFIELD_CODE;
  }

  @Override
  protected Set<UUID> extractIds(FieldParsedContent field) {
    var ids = new HashSet<UUID>();
    var subfieldValues = field.getIdSubfields();
    if (isNotEmpty(subfieldValues)) {
      ids.addAll(subfieldValues.stream()
        .map(ParsedSubfield::value)
        .filter(id -> UUID_REGEX.matcher(id).matches())
        .map(UUID::fromString)
        .collect(Collectors.toSet()));
    }
    if (nonNull(field.getLinkDetails()) && nonNull(field.getLinkDetails().getAuthorityId())) {
      ids.add(field.getLinkDetails().getAuthorityId());
    }
    return ids;
  }

  @Override
  protected Map<String, List<Authority>> findExistingAuthorities(Set<UUID> ids) {
    var authorities = authorityRepository.findAllByIdInAndDeletedFalse(ids);
    var localAuthorities = authorities.stream()
        .filter(a -> !a.isConsortiumShadowCopy())
        .toList();
    var shadowAuthorities = authorities.stream()
        .filter(AuthorityBase::isConsortiumShadowCopy)
        .toList();

    var authoritiesMap = new HashMap<String, List<Authority>>();
    authoritiesMap.put("local", localAuthorities);
    authoritiesMap.put("shared", shadowAuthorities);

    if (authorities.size() == ids.size()) {
      return authoritiesMap;
    }

    var centralTenant = userTenantsService.getCentralTenant(context.getTenantId());
    if (centralTenant.isEmpty() || centralTenant.get().equals(context.getTenantId())) {
      return authoritiesMap;
    }

    if (authorities.isEmpty()) {
      authoritiesMap.put("shared", authorityJdbcRepository.findAllByIdInAndDeletedFalse(ids, centralTenant.get()));
      authoritiesMap.put("local", Collections.emptyList());
      return authoritiesMap;
    }

    var existingIds = authorities.stream()
        .map(Authority::getId)
        .collect(Collectors.toSet());
    var missingIds = ids.stream()
        .filter(id -> !existingIds.contains(id))
        .collect(Collectors.toSet());

    if (!missingIds.isEmpty()) {
      var sharedAuthorities = authorityJdbcRepository.findAllByIdInAndDeletedFalse(missingIds, centralTenant.get());
      if (sharedAuthorities.isEmpty()) {
        authoritiesMap.put("shared", shadowAuthorities);
        return authoritiesMap;
      }
      sharedAuthorities.addAll(shadowAuthorities);
      authoritiesMap.put("shared", sharedAuthorities);
    }
    return authoritiesMap;
  }

  @Override
  protected UUID extractId(Authority authority) {
    return authority.getId();
  }
}
