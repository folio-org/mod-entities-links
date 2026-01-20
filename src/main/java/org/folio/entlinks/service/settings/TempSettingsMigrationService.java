package org.folio.entlinks.service.settings;

import static org.folio.entlinks.service.settings.TenantSetting.ARCHIVES_EXPIRATION_ENABLED;
import static org.folio.entlinks.service.settings.TenantSetting.ARCHIVES_EXPIRATION_PERIOD;
import static org.folio.entlinks.service.settings.TenantSetting.MAPPING_EXTENDED;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.SettingsClient;
import org.folio.entlinks.config.properties.AuthorityArchiveProperties;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.integration.SettingsService;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.service.TenantSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Settings migration service that migrate existing settings to the new API contract.
 * This service should be removed in Umbrellalead release. https://folio-org.atlassian.net/browse/MODELINKS-380
 */
@Log4j2
@Component
@RequiredArgsConstructor
@Deprecated(forRemoval = true)
public class TempSettingsMigrationService {

  private static final String AUTHORITY_EXTENDED = "AUTHORITY_EXTENDED";

  private final TenantSettingsService tenantSettingsService;
  private final AuthorityArchiveProperties authorityArchiveProperties;
  private final SettingsService settingsService;

  @Value("${" + AUTHORITY_EXTENDED + ":false}")
  private boolean isAuthorityExtended;

  public void migrateSettings() {
    if (isAuthorityExtended) {
      var updateRequest = new SettingUpdateRequest()
        .value(Boolean.TRUE)
        .description("Enable extended mapping for authorities");
      doUpdate(MAPPING_EXTENDED, updateRequest);
    }

    migrateExpirationSettings();
  }

  private void migrateExpirationSettings() {
    Optional<SettingsClient.SettingEntry> expireSetting;
    try {
      expireSetting = settingsService.getAuthorityExpireSetting();
    } catch (FolioIntegrationException e) {
      log.warn("Exception during settings fetching: ", e);
      expireSetting = Optional.empty();
    }

    if (expireSetting.isPresent() && expireSetting.get().value() != null
        && Boolean.FALSE.equals(expireSetting.get().value().expirationEnabled())) {
      var updateRequest = new SettingUpdateRequest().value(Boolean.FALSE);
      doUpdate(ARCHIVES_EXPIRATION_ENABLED, updateRequest);
    }

    var retentionDays = expireSetting
      .map(SettingsClient.SettingEntry::value)
      .map(SettingsClient.AuthoritiesExpirationSettingValue::retentionInDays)
      .orElse(authorityArchiveProperties.getRetentionPeriodInDays());
    var updateRequest = new SettingUpdateRequest().value(retentionDays);
    doUpdate(ARCHIVES_EXPIRATION_PERIOD, updateRequest);
  }

  private void doUpdate(TenantSetting tenantSetting, SettingUpdateRequest updateRequest) {
    tenantSettingsService.updateGroupSetting(tenantSetting.getGroup(), tenantSetting.getKey(), updateRequest);
  }
}
