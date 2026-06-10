package org.folio.entlinks.service.settings;

import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_EXTENDED_MAPPING_CACHE;
import static org.folio.entlinks.service.settings.TenantSetting.MAPPING_EXTENDED;

import java.util.Collections;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingCollection;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.mapper.TenantSettingsMapper;
import org.folio.tenant.settings.repository.SettingGroupRepository;
import org.folio.tenant.settings.repository.SettingRepository;
import org.folio.tenant.settings.service.TenantSettingsService;
import org.folio.tenant.settings.service.TenantSettingsValidator;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Primary
@NullMarked
public class SettingsService extends TenantSettingsService {

  private final FolioExecutionContext context;
  private final CacheManager cacheManager;

  public SettingsService(SettingGroupRepository settingGroupRepository,
                         SettingRepository settingRepository,
                         FolioExecutionContext context,
                         TenantSettingsMapper mapper,
                         TenantSettingsValidator validator,
                         CacheManager cacheManager) {
    super(settingGroupRepository, settingRepository, context, mapper, validator);
    this.context = context;
    this.cacheManager = cacheManager;
  }

  @Cacheable(cacheNames = AUTHORITY_EXTENDED_MAPPING_CACHE, key = "@folioExecutionContext.tenantId")
  public boolean isAuthorityExtendedMappingEnabled() {
    return getAuthorityExtendedCache()
      .map(cache -> cache.get(context.getTenantId(), Boolean.class))
      .orElseGet(() -> getGroupSettings(MAPPING_EXTENDED.getGroup())
        .map(SettingCollection::getSettings)
        .orElse(Collections.emptyList()).stream()
        .filter(setting -> MAPPING_EXTENDED.getKey().equals(setting.getKey()))
        .map(Setting::getValue)
        .map(Boolean.class::cast)
        .findFirst()
        .orElse(false));
  }

  @Override
  public Optional<Setting> updateGroupSetting(String groupId, String key, SettingUpdateRequest updateRequest) {
    if (MAPPING_EXTENDED.getGroup().equals(groupId) && MAPPING_EXTENDED.getKey().equals(key)) {
      log.debug("Clearing cache for authority extended mapping setting update for tenant: {}", context.getTenantId());
      getAuthorityExtendedCache().ifPresent(cache -> cache.evict(context.getTenantId()));
    }
    return super.updateGroupSetting(groupId, key, updateRequest);
  }

  private Optional<Cache> getAuthorityExtendedCache() {
    return Optional.ofNullable(cacheManager.getCache(AUTHORITY_EXTENDED_MAPPING_CACHE));
  }
}
