package org.folio.entlinks.controller.delegate.suggestion;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
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
public class LinksSuggestionsByAuthorityId extends LinksSuggestionsServiceDelegateBase<UUID> {

  private static final Pattern UUID_REGEX =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  private final AuthorityRepository authorityRepository;
  private final AuthorityJdbcRepository jdbcRepository;

  public LinksSuggestionsByAuthorityId(InstanceAuthorityLinkingRulesService linkingRulesService,
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
  protected List<Authority> findExistingAuthorities(Set<UUID> ids) {
    return authorityRepository.findAllByIdInAndDeletedFalse(ids);
  }

  @Override
  protected List<Authority> findExistingAuthoritiesForTenant(String tenantId, Set<UUID> ids) {
    return jdbcRepository.findAllByIdInAndDeletedFalse(ids, tenantId);
  }

  @Override
  protected UUID extractId(Authority authority) {
    return authority.getId();
  }
}
