package org.folio.entlinks.controller.delegate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.config.properties.LocalStorageProperties;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityBulkRequest;
import org.folio.entlinks.domain.dto.AuthorityBulkResponse;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityFullDtoCollection;
import org.folio.entlinks.domain.dto.AuthorityIdDto;
import org.folio.entlinks.domain.dto.AuthorityIdDtoCollection;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityBase;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.service.authority.AuthoritiesBulkContext;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.entlinks.service.authority.AuthorityS3Service;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.entlinks.service.consortium.UserTenantsService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityDataStatsPropagationService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService;
import org.folio.entlinks.service.consortium.propagation.model.AuthorityDataStatsPropagationData;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.spring.FolioExecutionContext;
import org.folio.tenant.domain.dto.Parameter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Log4j2
@Service
@RequestScope
public class AuthorityServiceDelegate {

  private final AuthorityService service;
  private final AuthorityMapper mapper;
  private final FolioExecutionContext context;
  private final AuthorityDomainEventPublisher eventPublisher;
  private final AuthorityS3Service authorityS3Service;
  private final LocalStorageProperties localStorageProperties;
  private final AuthorityDataStatService dataStatService;
  private final ConsortiumAuthorityDataStatsPropagationService dataStatPropagationService;

  public AuthorityServiceDelegate(@Qualifier("authorityService") AuthorityService service,
                                  @Qualifier("consortiumAuthorityService") AuthorityService consortiumService,
                                  AuthorityMapper mapper, FolioExecutionContext context,
                                  AuthorityDomainEventPublisher eventPublisher,
                                  AuthorityS3Service authorityS3Service,
                                  LocalStorageProperties localStorageProperties,
                                  UserTenantsService userTenantsService,
                                  AuthorityDataStatService dataStatService,
                                  ConsortiumAuthorityDataStatsPropagationService dataStatPropagationService) {
    this.dataStatService = dataStatService;
    this.dataStatPropagationService = dataStatPropagationService;
    this.service = userTenantsService.getCentralTenant(context.getTenantId()).isEmpty()
                   ? service
                   : consortiumService;
    this.mapper = mapper;
    this.context = context;
    this.eventPublisher = eventPublisher;
    this.authorityS3Service = authorityS3Service;
    this.localStorageProperties = localStorageProperties;
  }

  public AuthorityFullDtoCollection retrieveAuthorityCollection(Integer offset, Integer limit, String cqlQuery,
                                                                Boolean idOnly) {
    if (Boolean.TRUE.equals(idOnly)) {
      var idsPage = service.getAllIds(offset, limit, cqlQuery);
      var ids = idsPage.map(id -> new AuthorityIdDto().id(id)).toList();
      return new AuthorityIdDtoCollection(ids, (int) idsPage.getTotalElements());
    }

    var entitiesPage = service.getAll(offset, limit, cqlQuery)
      .map(AuthorityBase.class::cast);
    return mapper.toAuthorityCollection(entitiesPage);
  }

  public AuthorityDto getAuthorityById(UUID id) {
    var entity = service.getById(id);
    return mapper.toDto(entity);
  }

  public AuthorityDto createAuthority(AuthorityDto authorityDto) {
    var entity = mapper.toEntity(authorityDto);
    var created = service.create(entity);
    createConsumer().accept(created);
    return mapper.toDto(created);
  }

  public void updateAuthority(UUID id, AuthorityDto authorityDto) {
    if (!Objects.equals(id, authorityDto.getId())) {
      throw new RequestBodyValidationException("Request should have id = " + id,
        List.of(new Parameter("id").value(String.valueOf(authorityDto.getId()))));
    }
    var modifiedEntity = mapper.toEntity(authorityDto);
    var updateResult = service.update(modifiedEntity, false);
    updateConsumer().accept(updateResult.newEntity(), updateResult.oldEntity());
  }

  /**
   * Deletes authority by id.
   * Deletes and propagates deletion for associated data stats.
   * */
  public void deleteAuthorityById(UUID id) {
    var authority = service.deleteById(id);
    dataStatService.deleteByAuthorityId(id);
    var propagationData = AuthorityDataStatsPropagationData.forDelete(id);
    dataStatPropagationService.propagate(propagationData, ConsortiumPropagationService.PropagationType.DELETE,
      context.getTenantId());

    eventPublisher.publishSoftDeleteEvent(mapper.toDto(authority));
  }

  @SneakyThrows
  public AuthorityBulkResponse createAuthorities(AuthorityBulkRequest createRequest) {
    var bulkContext = new AuthoritiesBulkContext(
        createRequest.getRecordsFileName(), localStorageProperties.getS3LocalSubPath());
    var errorsCount = authorityS3Service.processAuthorities(bulkContext, this::upsertAuthorities);

    var authorityBulkCreateResponse = new AuthorityBulkResponse()
      .errorsNumber(errorsCount);
    if (errorsCount > 0) {
      authorityBulkCreateResponse
        .errorRecordsFileName(bulkContext.getFailedEntitiesFilePath())
        .errorsFileName(bulkContext.getErrorsFilePath());
    }
    return authorityBulkCreateResponse;
  }

  private void upsertAuthorities(List<Authority> authorities) {
    service.upsert(authorities).forEach(authorityUpdateResult -> {
      if (authorityUpdateResult.oldEntity() == null) {
        createConsumer().accept(authorityUpdateResult.newEntity());
      } else {
        updateConsumer().accept(authorityUpdateResult.newEntity(), authorityUpdateResult.oldEntity());
      }
    });
  }

  @NotNull
  private Consumer<Authority> createConsumer() {
    return authority -> eventPublisher.publishCreateEvent(mapper.toDto(authority));
  }

  @NotNull
  private BiConsumer<Authority, Authority> updateConsumer() {
    return (newAuthority, oldAuthority) ->
        eventPublisher.publishUpdateEvent(mapper.toDto(oldAuthority), mapper.toDto(newAuthority));
  }
}
