package org.folio.entlinks.controller.delegate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.service.authority.AuthoritiesBulkContext;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.entlinks.service.authority.AuthorityS3Service;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.entlinks.service.settings.TenantSetting;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingCollection;
import org.folio.tenant.settings.service.TenantSettingsService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class AuthorityServiceDelegate {

  private final AuthorityService service;
  private final AuthorityMapper mapper;
  private final AuthorityDomainEventPublisher eventPublisher;
  private final AuthorityS3Service authorityS3Service;
  private final LocalStorageProperties localStorageProperties;
  private final AuthorityDataStatService dataStatService;
  private final TenantSettingsService tenantSettingsService;

  public AuthorityServiceDelegate(AuthorityService service,
                                  AuthorityMapper mapper,
                                  AuthorityDomainEventPublisher eventPublisher,
                                  AuthorityS3Service authorityS3Service,
                                  LocalStorageProperties localStorageProperties,
                                  AuthorityDataStatService dataStatService,
                                  TenantSettingsService tenantSettingsService) {
    this.dataStatService = dataStatService;
    this.service = service;
    this.mapper = mapper;
    this.eventPublisher = eventPublisher;
    this.authorityS3Service = authorityS3Service;
    this.localStorageProperties = localStorageProperties;
    this.tenantSettingsService = tenantSettingsService;
  }

  public AuthorityFullDtoCollection retrieveAuthorityCollection(Integer offset, Integer limit, String cqlQuery,
                                                                Boolean idOnly, Boolean deleted) {
    if (Boolean.TRUE.equals(deleted)) {
      return retrieveDeletedCollection(offset, limit, cqlQuery, idOnly);
    }

    if (Boolean.TRUE.equals(idOnly)) {
      var idsPage = service.getAllIds(offset, limit, cqlQuery);
      var ids = idsPage.map(id -> new AuthorityIdDto().id(id)).toList();
      return new AuthorityIdDtoCollection(ids, (int) idsPage.getTotalElements());
    }

    var entitiesPage = service.getAll(offset, limit, cqlQuery);
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
    var updateResult = service.update(modifiedEntity);
    updateConsumer().accept(updateResult.newEntity(), updateResult.oldEntity());
  }

  /**
   * Deletes authority by id.
   * Deletes and propagates deletion for associated data stats.
   */
  public void deleteAuthorityById(UUID id) {
    var authority = service.deleteByIdSoft(id);
    dataStatService.deleteByAuthorityId(id);

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

  public void expire() {
    var retention = fetchAuthoritiesRetentionDuration();

    if (retention.isEmpty()) {
      return;
    }

    var tillDate = LocalDateTime.now().minusDays(retention.get());
    service.expireHardDeleted(tillDate, authority -> eventPublisher.publishHardDeleteEvent(mapper.toDto(authority)));
  }

  private AuthorityFullDtoCollection retrieveDeletedCollection(Integer offset, Integer limit, String cqlQuery,
                                                               Boolean idOnly) {
    if (Boolean.TRUE.equals(idOnly)) {
      var idsPage = service.getAllDeletedIds(offset, limit, cqlQuery);
      var ids = idsPage.map(id -> new AuthorityIdDto().id(id)).toList();
      return new AuthorityIdDtoCollection(ids, (int) idsPage.getTotalElements());
    }

    var entitiesPage = service.getAllDeleted(offset, limit, cqlQuery);
    return mapper.toAuthorityCollection(entitiesPage);
  }

  private Optional<Integer> fetchAuthoritiesRetentionDuration() {
    var groupSettings = tenantSettingsService.getGroupSettings(TenantSetting.ARCHIVES_EXPIRATION_ENABLED.getGroup());
    if (groupSettings.isEmpty()) {
      log.warn("No settings were found for the tenant");
      return Optional.empty();
    }

    var expirationEnabledSetting = getSetting(groupSettings.get(), TenantSetting.ARCHIVES_EXPIRATION_ENABLED);

    if (Boolean.FALSE.equals(expirationEnabledSetting.getValue())) {
      log.info("Authority archives expiration is disabled for the tenant through setting");
      return Optional.empty();
    }

    var expirationPeriodSetting = getSetting(groupSettings.get(), TenantSetting.ARCHIVES_EXPIRATION_PERIOD);
    return Optional.ofNullable((Integer) expirationPeriodSetting.getValue());
  }

  private Setting getSetting(SettingCollection groupSettings, TenantSetting tenantSetting) {
    return groupSettings.getSettings().stream()
      .filter(setting -> tenantSetting.getKey().equals(setting.getKey()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No %s setting was found".formatted(tenantSetting.getKey())));
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
