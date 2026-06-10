package org.folio.entlinks.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_EXTENDED_MAPPING_CACHE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.folio.entlinks.service.settings.SettingsService;
import org.folio.entlinks.service.settings.TenantSetting;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.entity.SettingEntity;
import org.folio.tenant.settings.mapper.TenantSettingsMapper;
import org.folio.tenant.settings.repository.SettingGroupRepository;
import org.folio.tenant.settings.repository.SettingRepository;
import org.folio.tenant.settings.service.TenantSettingsValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

  private static final String MAPPING_EXTENDED_GROUP = TenantSetting.MAPPING_EXTENDED.getGroup();
  private static final String MAPPING_EXTENDED_KEY = TenantSetting.MAPPING_EXTENDED.getKey();

  @Mock
  private SettingGroupRepository settingGroupRepository;
  @Mock
  private SettingRepository settingRepository;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private TenantSettingsMapper mapper;
  @Mock
  private TenantSettingsValidator validator;
  @Mock
  private CacheManager cacheManager;
  @Mock
  private Cache cache;
  @Mock
  private SettingEntity settingEntity;

  @InjectMocks
  private SettingsService service;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(settingGroupRepository, settingRepository, mapper, validator);
  }

  @Test
  void isAuthorityExtendedMappingEnabled_positive_settingIsTrue() {
    // Arrange
    var setting = new Setting().key(MAPPING_EXTENDED_KEY).value(true);
    when(settingGroupRepository.existsById(MAPPING_EXTENDED_GROUP)).thenReturn(true);
    when(settingRepository.findByGroupId(MAPPING_EXTENDED_GROUP)).thenReturn(List.of(settingEntity));
    when(mapper.toDto(settingEntity)).thenReturn(setting);

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  void isAuthorityExtendedMappingEnabled_positive_settingIsFalse() {
    // Arrange
    var setting = new Setting().key(MAPPING_EXTENDED_KEY).value(false);
    when(settingGroupRepository.existsById(MAPPING_EXTENDED_GROUP)).thenReturn(true);
    when(settingRepository.findByGroupId(MAPPING_EXTENDED_GROUP)).thenReturn(List.of(settingEntity));
    when(mapper.toDto(settingEntity)).thenReturn(setting);

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void isAuthorityExtendedMappingEnabled_positive_cached() {
    // Arrange
    when(cacheManager.getCache(AUTHORITY_EXTENDED_MAPPING_CACHE)).thenReturn(cache);
    when(cache.get(context.getTenantId(), Boolean.class)).thenReturn(true);

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  void isAuthorityExtendedMappingEnabled_negative_settingNotFound() {
    // Arrange
    var differentSetting = new Setting().key("some.other.key").value(true);
    when(settingGroupRepository.existsById(MAPPING_EXTENDED_GROUP)).thenReturn(true);
    when(settingRepository.findByGroupId(MAPPING_EXTENDED_GROUP)).thenReturn(List.of(settingEntity));
    when(mapper.toDto(settingEntity)).thenReturn(differentSetting);

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void isAuthorityExtendedMappingEnabled_negative_emptySettings() {
    // Arrange
    when(settingGroupRepository.existsById(MAPPING_EXTENDED_GROUP)).thenReturn(true);
    when(settingRepository.findByGroupId(MAPPING_EXTENDED_GROUP)).thenReturn(Collections.emptyList());

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void isAuthorityExtendedMappingEnabled_negative_noGroupSettings() {
    // Arrange — settingGroupRepository.existsById returns false by default

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isFalse();
    verify(settingGroupRepository).existsById(MAPPING_EXTENDED_GROUP);
  }

  @Test
  void updateGroupSetting_positive_cacheIsClearedWhenMappingExtendedUpdated() {
    // Arrange
    var updateRequest = new SettingUpdateRequest();
    when(cacheManager.getCache(AUTHORITY_EXTENDED_MAPPING_CACHE)).thenReturn(cache);
    when(settingRepository.findByGroupIdAndKey(MAPPING_EXTENDED_GROUP, MAPPING_EXTENDED_KEY))
      .thenReturn(Optional.empty());

    // Act
    service.updateGroupSetting(MAPPING_EXTENDED_GROUP, MAPPING_EXTENDED_KEY, updateRequest);

    // Assert
    verify(cache).evict(context.getTenantId());
  }

  @Test
  void updateGroupSetting_positive_cacheNotClearedWhenGroupDoesNotMatch() {
    // Arrange
    var otherGroup = "other.group";
    var updateRequest = new SettingUpdateRequest();
    when(settingRepository.findByGroupIdAndKey(otherGroup, MAPPING_EXTENDED_KEY))
      .thenReturn(Optional.empty());

    // Act
    service.updateGroupSetting(otherGroup, MAPPING_EXTENDED_KEY, updateRequest);

    // Assert — no cache interaction
    verifyNoInteractions(cacheManager, cache);
  }

  @Test
  void updateGroupSetting_positive_cacheNotClearedWhenKeyDoesNotMatch() {
    // Arrange
    var otherKey = "other.key";
    var updateRequest = new SettingUpdateRequest();
    when(settingRepository.findByGroupIdAndKey(MAPPING_EXTENDED_GROUP, otherKey))
      .thenReturn(Optional.empty());

    // Act
    service.updateGroupSetting(MAPPING_EXTENDED_GROUP, otherKey, updateRequest);

    // Assert — no cache interaction
    verifyNoInteractions(cacheManager, cache);
  }
}
