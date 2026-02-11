package org.folio.entlinks.controller.delegate;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityFullDtoCollection;
import org.folio.entlinks.domain.dto.AuthorityIdDto;
import org.folio.entlinks.domain.dto.AuthorityIdDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.entity.AuthorityBase;
import org.folio.entlinks.domain.repository.AuthorityArchiveRepository;
import org.folio.entlinks.service.authority.AuthorityArchiveService;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.entlinks.service.settings.TenantSetting;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingCollection;
import org.folio.tenant.settings.service.TenantSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorityArchiveServiceDelegate {

  private final AuthorityArchiveService authorityArchiveService;
  private final AuthorityArchiveRepository authorityArchiveRepository;
  private final AuthorityDomainEventPublisher eventPublisher;
  private final AuthorityMapper authorityMapper;
  private final TenantSettingsService tenantSettingsService;

  public AuthorityFullDtoCollection retrieveAuthorityArchives(Integer offset, Integer limit, String cqlQuery,
                                                              Boolean idOnly) {
    if (Boolean.TRUE.equals(idOnly)) {
      var idsPage = authorityArchiveService.getAllIds(offset, limit, cqlQuery);
      var ids = idsPage.map(id -> new AuthorityIdDto().id(id)).toList();
      return new AuthorityIdDtoCollection(ids, (int) idsPage.getTotalElements());
    }

    var entitiesPage = authorityArchiveService.getAll(offset, limit, cqlQuery)
      .map(AuthorityBase.class::cast);
    return authorityMapper.toAuthorityCollection(entitiesPage);
  }

  @Transactional(readOnly = true)
  public void expire() {
    var retention = fetchAuthoritiesRetentionDuration();

    if (retention.isEmpty()) {
      return;
    }

    var tillDate = LocalDateTime.now().minusDays(retention.get());
    try (var archives = authorityArchiveRepository.streamByUpdatedTillDateAndSourcePrefix(tillDate)) {
      archives.forEach(this::process);
    }
  }

  private void process(AuthorityArchive archive) {
    authorityArchiveService.delete(archive);
    var dto = authorityMapper.toDto(archive);
    eventPublisher.publishHardDeleteEvent(dto);
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
}
