package org.folio.entlinks.integration;

import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_EXTENDED_MAPPING_CACHE;

import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.entlinks.client.SettingsClient;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.service.settings.TenantSetting;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingCollection;
import org.folio.tenant.settings.service.TenantSettingsService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class SettingsService {

  public static final String AUTHORITIES_EXPIRE_SETTING_KEY = "authority-archives-expiration";
  public static final String AUTHORITIES_EXPIRE_SETTING_SCOPE = "authority-storage.manage";

  private static final String AUTHORITIES_EXPIRE_SETTING_FETCH_QUERY =
    "(scope=authority-storage.manage AND key=authority-archives-expiration)";

  private static final int DEFAULT_REQUEST_LIMIT = 10000;

  private final SettingsClient settingsClient;
  private final TenantSettingsService tenantSettingsService;

  @Cacheable(cacheNames = AUTHORITY_EXTENDED_MAPPING_CACHE, key = "@folioExecutionContext.tenantId")
  public boolean isAuthorityExtendedMappingEnabled() {
    return tenantSettingsService.getGroupSettings(TenantSetting.MAPPING_EXTENDED.getGroup())
      .map(SettingCollection::getSettings)
      .orElse(Collections.emptyList()).stream()
      .filter(setting -> TenantSetting.MAPPING_EXTENDED.getKey().equals(setting.getKey()))
      .map(Setting::getValue)
      .map(Boolean.class::cast)
      .findFirst()
      .orElse(false);
  }

  @Deprecated(forRemoval = true)
  public Optional<SettingsClient.SettingEntry> getAuthorityExpireSetting() throws FolioIntegrationException {
    var settingsEntries = fetchSettingsEntries();

    if (settingsEntries == null || CollectionUtils.isEmpty(settingsEntries.items())) {
      return Optional.empty();
    }

    return settingsEntries.items().stream()
      .filter(entry -> entry.scope().equals(AUTHORITIES_EXPIRE_SETTING_SCOPE))
      .filter(entry -> entry.key().equals(AUTHORITIES_EXPIRE_SETTING_KEY))
      .findFirst();
  }

  private SettingsClient.SettingsEntries fetchSettingsEntries() {
    try {
      return settingsClient.getSettingsEntries(AUTHORITIES_EXPIRE_SETTING_FETCH_QUERY, DEFAULT_REQUEST_LIMIT);
    } catch (Exception e) {
      throw new FolioIntegrationException("Failed to fetch settings", e);
    }
  }
}
