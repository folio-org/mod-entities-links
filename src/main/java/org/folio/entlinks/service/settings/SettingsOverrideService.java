package org.folio.entlinks.service.settings;

import static org.folio.entlinks.service.settings.TenantSetting.MAPPING_EXTENDED;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Overrides tenant settings based on application properties.
 * Currently used for enabling extended authority mapping for all tenants
 * to avoid manual configuration after deployment.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class SettingsOverrideService {

  private static final String AUTHORITY_EXTENDED = "AUTHORITY_EXTENDED";

  private final SettingsService settingsService;

  @Value("${" + AUTHORITY_EXTENDED + ":#{null}}")
  private Optional<Boolean> isAuthorityExtended;

  public void overrideSettings() {
    if (isAuthorityExtended.isPresent()) {
      log.info("Overriding tenant settings for authority extended mapping based on existed environment variable: {}",
        AUTHORITY_EXTENDED);
      var updateRequest = new SettingUpdateRequest()
        .value(isAuthorityExtended.get());
      settingsService.updateGroupSetting(MAPPING_EXTENDED.getGroup(), MAPPING_EXTENDED.getKey(), updateRequest);
    }
  }
}
