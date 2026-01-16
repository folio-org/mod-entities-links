package org.folio.entlinks.service.tenant;

import lombok.RequiredArgsConstructor;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.service.TenantSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Settings migration service that migrate existing settings to the new API contract.
 * This service should be removed in Umbrellalead release. //todo insert Jira
 */
@Component
@RequiredArgsConstructor
public class TempSettingsMigrationService {

  public static final String AUTHORITY_EXTENDED = "AUTHORITY_EXTENDED";
  public static final String AUTHORITIES_SETTINGS_GROUP = "authorities";
  public static final String MAPPING_EXTENDED_SETTING = "mapping.extended";

  private final TenantSettingsService tenantSettingsService;

  @Value("${" + AUTHORITY_EXTENDED + ":false}")
  private boolean isAuthorityExtended;

  public void migrateSettings() {
    if (isAuthorityExtended) {
      var updateRequest = new SettingUpdateRequest()
        .value(Boolean.TRUE)
        .description("Enable extended mapping for authorities");
      tenantSettingsService.updateGroupSetting(AUTHORITIES_SETTINGS_GROUP, MAPPING_EXTENDED_SETTING, updateRequest);
    }
  }
}
