package org.folio.entlinks.controller.delegate;

import static org.folio.entlinks.domain.entity.AuthoritySourceFileSource.FOLIO;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.CREATE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.DELETE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.UPDATE;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFileHridDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.exception.AuthorityArchiveConstraintException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.integration.dto.event.DomainEventType;
import org.folio.entlinks.service.authority.AuthoritySourceFileDomainEventPublisher;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.consortium.UserTenantsService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthoritySourceFilePropagationService;
import org.folio.entlinks.service.consortium.propagation.model.AuthoritySourceFilePropagationData;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.tenant.domain.dto.Parameter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthoritySourceFileServiceDelegate {

  private static final String URL_PROTOCOL_PATTERN = "^(https?://www\\.|https?://|www\\.)";
  private static final String AUTHORITY_TABLE_NAME = "authority";
  private static final String AUTHORITY_ARCHIVE_TABLE_NAME = "authority_archive";

  private final @Qualifier("authoritySourceFileService") AuthoritySourceFileService service;
  private final AuthoritySourceFileMapper mapper;
  private final UserTenantsService tenantsService;
  private final AuthoritySourceFileDomainEventPublisher eventPublisher;
  private final ConsortiumAuthoritySourceFilePropagationService propagationService;
  private final FolioExecutionContext context;
  private final SystemUserScopedExecutionService executionService;
  private final ConsortiumTenantsService consortiumTenantsService;

  public AuthoritySourceFileDtoCollection getAuthoritySourceFiles(Integer offset, Integer limit, String cqlQuery) {
    var entities = service.getAll(offset, limit, cqlQuery);
    return mapper.toAuthoritySourceFileCollection(entities);
  }

  public AuthoritySourceFileDto getAuthoritySourceFileById(UUID id) {
    var entity = service.getById(id);
    return mapper.toDto(entity);
  }

  public AuthoritySourceFileDto createAuthoritySourceFile(AuthoritySourceFilePostDto authoritySourceFile) {
    log.debug("create:: Attempting to create AuthoritySourceFile [createDto: {}]", authoritySourceFile);
    validateActionRightsForTenant(DomainEventType.CREATE);
    var entity = mapper.toEntity(authoritySourceFile);
    normalizeBaseUrl(entity);
    var created = service.create(entity);

    service.createSequence(created.getSequenceName(), created.getHridStartNumber());

    propagationService.propagate(getPropagationData(created, null), CREATE, context.getTenantId());
    return mapper.toDto(created);
  }

  public void patchAuthoritySourceFile(UUID id, AuthoritySourceFilePatchDto partiallyModifiedDto) {
    log.debug("patch:: Attempting to patch AuthoritySourceFile [id: {}, patchDto: {}]", id, partiallyModifiedDto);
    var existingEntity = service.getById(id);
    validateActionRightsForTenant(DomainEventType.UPDATE);
    var hasAuthorityReferences = anyAuthoritiesExistForSourceFile(existingEntity);
    validatePatchRequest(partiallyModifiedDto, existingEntity, hasAuthorityReferences);

    var partialEntityUpdate = new AuthoritySourceFile(existingEntity);
    partialEntityUpdate = mapper.partialUpdate(partiallyModifiedDto, partialEntityUpdate);
    normalizeBaseUrl(partialEntityUpdate);

    var publishConsumer = publishRequired(hasAuthorityReferences, partiallyModifiedDto, existingEntity)
      ? getUpdatePublishConsumer() : null;
    var patched = service.update(id, partialEntityUpdate, publishConsumer);
    log.debug("patch:: Authority Source File partially updated: {}", patched);
    propagationService.propagate(getPropagationData(patched, publishConsumer), UPDATE, context.getTenantId());
  }

  public void deleteAuthoritySourceFileById(UUID id) {
    var entity = service.getById(id);
    validateActionRightsForTenant(DomainEventType.DELETE);

    if (anyAuthoritiesExistForSourceFile(entity)) {
      throw new RequestBodyValidationException(
        "Unable to delete. Authority source file has referenced authorities", Collections.emptyList());
    }

    if (anyReferenceForSourceFile(entity)) {
      throw new AuthorityArchiveConstraintException(
        "Unable to delete. Authority source file has referenced authority archives");
    }

    if (entity.getSequenceName() != null) {
      service.deleteSequence(entity.getSequenceName());
    }

    service.deleteById(id);
    propagationService.propagate(getPropagationData(entity, null), DELETE, context.getTenantId());
  }

  public AuthoritySourceFileHridDto getAuthoritySourceFileNextHrid(UUID id) {
    log.debug("nextHrid:: Attempting to get next AuthoritySourceFile HRID [id: {}]", id);
    var tenantId = tenantsService.getCentralTenant(context.getTenantId()).orElse(context.getTenantId());
    var hrid = executionService.executeSystemUserScoped(tenantId, () -> service.nextHrid(id));

    return new AuthoritySourceFileHridDto().id(id).hrid(hrid);
  }

  private void normalizeBaseUrl(AuthoritySourceFile entity) {
    var baseUrl = entity.getBaseUrl();
    if (StringUtils.isNotBlank(baseUrl)) {
      baseUrl = baseUrl.replaceFirst(URL_PROTOCOL_PATTERN, "");
      if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      entity.setBaseUrl(baseUrl);
    }
  }

  private void validateActionRightsForTenant(DomainEventType action) {
    var tenantId = context.getTenantId();
    var centralTenantId = tenantsService.getCentralTenant(tenantId);
    if (centralTenantId.isPresent() && !tenantId.equals(centralTenantId.get())) {
      throw new RequestBodyValidationException("Action '%s' is not supported for consortium member tenant"
        .formatted(action), List.of(new Parameter("tenantId").value(tenantId)));
    }
  }

  private void validatePatchRequest(AuthoritySourceFilePatchDto patchDto, AuthoritySourceFile existing,
                                    boolean hasAuthorityReferences) {
    var errorParameters = new LinkedList<Parameter>();

    if (!(existing.getSource().equals(FOLIO) || hasAuthorityReferences)) {
      return;
    }

    if (existing.getSource().equals(FOLIO) && patchDto.getName() != null) {
      errorParameters.add(new Parameter("name")
        .value(String.join(",", patchDto.getName())));
    }
    if (patchDto.getCode() != null) {
      errorParameters.add(new Parameter("code").value(patchDto.getCode()));
    }
    if (patchDto.getHridManagement() != null && patchDto.getHridManagement().getStartNumber() != null) {
      errorParameters.add(new Parameter("hridManagement.startNumber")
        .value(patchDto.getHridManagement().getStartNumber().toString()));
    }

    if (!errorParameters.isEmpty()) {
      throw new RequestBodyValidationException(
        "Unable to patch. Authority source file source is FOLIO or it has authority references", errorParameters);
    }
  }

  public boolean anyReferenceForSourceFile(AuthoritySourceFile sourceFile) {
    return anyDataExistForSourceFile(sourceFile.getId(), AUTHORITY_ARCHIVE_TABLE_NAME);
  }

  public boolean anyAuthoritiesExistForSourceFile(AuthoritySourceFile sourceFile) {
    var sourceFileId = sourceFile.getId();
    if (service.authoritiesExistForSourceFile(sourceFileId)) {
      return true;
    }
    return anyDataExistForSourceFile(sourceFileId, AUTHORITY_TABLE_NAME);
  }

  private boolean anyDataExistForSourceFile(UUID sourceFileId, String tableName) {
    var consortiumTenants = consortiumTenantsService.getConsortiumTenants(context.getTenantId());
    for (String memberTenant : consortiumTenants) {
      if (service.authoritiesExistForSourceFile(sourceFileId, memberTenant, tableName)) {
        return true;
      }
    }
    return false;
  }

  private AuthoritySourceFilePropagationData<AuthoritySourceFile> getPropagationData(
    AuthoritySourceFile authoritySourceFile, BiConsumer<AuthoritySourceFile, AuthoritySourceFile> publishConsumer) {
    return new AuthoritySourceFilePropagationData<>(authoritySourceFile, publishConsumer);
  }

  @NotNull
  private BiConsumer<AuthoritySourceFile, AuthoritySourceFile> getUpdatePublishConsumer() {
    return (newAsf, oldAsf) -> eventPublisher.publishUpdateEvent(mapper.toDto(newAsf), mapper.toDto(oldAsf));
  }

  private boolean publishRequired(boolean hasRefs, AuthoritySourceFilePatchDto modified, AuthoritySourceFile existed) {
    return modified.getBaseUrl() != null && hasRefs && !modified.getBaseUrl().equals(existed.getBaseUrl());
  }
}
