package org.folio.entlinks.controller.delegate.suggestion;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.List;
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
  private final AuthorityJdbcRepository jdbcRepository;

  public LinksSuggestionsByAuthorityNaturalId(InstanceAuthorityLinkingRulesService linkingRulesService,
                                              LinksSuggestionsService suggestionService,
                                              AuthorityRepository repository,
                                              SourceStorageClient sourceStorageClient,
                                              SourceContentMapper contentMapper,
                                              ConsortiumTenantExecutor executor,
                                              UserTenantsService tenantsService,
                                              FolioExecutionContext context, AuthorityJdbcRepository jdbcRepository) {
    super(linkingRulesService, suggestionService, sourceStorageClient,
      contentMapper, executor, tenantsService, context);
    this.authorityRepository = repository;
    this.jdbcRepository = jdbcRepository;
  }

  @Override
  protected char getSearchSubfield() {
    return FieldUtils.NATURAL_ID_SUBFIELD_CODE;
  }

  @Override
  protected List<Authority> findExistingAuthorities(Set<String> ids) {
    return authorityRepository.findByNaturalIdInAndDeletedFalse(ids);
  }

  @Override
  protected List<Authority> findExistingAuthoritiesForTenant(String tenantId, Set<String> ids) {
    return jdbcRepository.findByNaturalIdInAndDeletedFalse(ids, tenantId);
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
