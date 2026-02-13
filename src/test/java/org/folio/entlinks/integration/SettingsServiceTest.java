package org.folio.entlinks.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.client.SettingsClient;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.service.settings.TenantSetting;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingCollection;
import org.folio.tenant.settings.service.TenantSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

  @Mock
  private SettingsClient settingsClient;
  
  @Mock
  private TenantSettingsService tenantSettingsService;

  @InjectMocks
  private SettingsService service;

  @Test
  void shouldNotReturnAuthorityExpirationSeetingWhenNoSettingsExist() {
    when(settingsClient.getSettingsEntries(any(String.class), any(Integer.class)))
        .thenReturn(new SettingsClient.SettingsEntries(List.of(), null));

    var setting = service.getAuthorityExpireSetting();

    assertEquals(Optional.empty(), setting);
  }

  @Test
  void shouldThrowIntegrationExceptionWhenFetchingSettingsFailed() {
    when(settingsClient.getSettingsEntries(any(String.class), any(Integer.class)))
        .thenThrow(new RuntimeException());

    assertThrows(FolioIntegrationException.class, () -> service.getAuthorityExpireSetting());
  }

  @Test
  void shouldReturnAuthorityExpirationSeeting() {
    var settingValue = new SettingsClient.AuthoritiesExpirationSettingValue(true, 5);
    var settingEntry = new SettingsClient.SettingEntry(UUID.randomUUID(),
        SettingsService.AUTHORITIES_EXPIRE_SETTING_SCOPE, SettingsService.AUTHORITIES_EXPIRE_SETTING_KEY, settingValue);
    when(settingsClient.getSettingsEntries(any(String.class), any(Integer.class)))
        .thenReturn(new SettingsClient.SettingsEntries(List.of(settingEntry), new SettingsClient.ResultInfo(1)));

    var setting = service.getAuthorityExpireSetting();

    assertEquals(Optional.of(settingEntry), setting);
  }

  @Test
  void isAuthorityExtendedMappingEnabled_positive_settingIsTrue() {
    // Arrange
    var setting = new Setting()
      .key(TenantSetting.MAPPING_EXTENDED.getKey())
      .value(true);
    var settingCollection = new SettingCollection()
      .settings(List.of(setting));

    when(tenantSettingsService.getGroupSettings(TenantSetting.MAPPING_EXTENDED.getGroup()))
      .thenReturn(Optional.of(settingCollection));

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  void isAuthorityExtendedMappingEnabled_positive_settingIsFalse() {
    // Arrange
    var setting = new Setting()
      .key(TenantSetting.MAPPING_EXTENDED.getKey())
      .value(false);
    var settingCollection = new SettingCollection()
      .settings(List.of(setting));

    when(tenantSettingsService.getGroupSettings(TenantSetting.MAPPING_EXTENDED.getGroup()))
      .thenReturn(Optional.of(settingCollection));

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void isAuthorityExtendedMappingEnabled_negative_settingNotFound() {
    // Arrange
    var differentSetting = new Setting()
      .key("some.other.key")
      .value(true);
    var settingCollection = new SettingCollection()
      .settings(List.of(differentSetting));

    when(tenantSettingsService.getGroupSettings(TenantSetting.MAPPING_EXTENDED.getGroup()))
      .thenReturn(Optional.of(settingCollection));

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void isAuthorityExtendedMappingEnabled_negative_emptySettings() {
    // Arrange
    var settingCollection = new SettingCollection()
      .settings(Collections.emptyList());

    when(tenantSettingsService.getGroupSettings(TenantSetting.MAPPING_EXTENDED.getGroup()))
      .thenReturn(Optional.of(settingCollection));

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void isAuthorityExtendedMappingEnabled_negative_noGroupSettings() {
    // Arrange
    when(tenantSettingsService.getGroupSettings(TenantSetting.MAPPING_EXTENDED.getGroup()))
      .thenReturn(Optional.empty());

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  void isAuthorityExtendedMappingEnabled_negative_nullSettings() {
    // Arrange
    var settingCollection = new SettingCollection()
      .settings(null);

    when(tenantSettingsService.getGroupSettings(TenantSetting.MAPPING_EXTENDED.getGroup()))
      .thenReturn(Optional.of(settingCollection));

    // Act
    var result = service.isAuthorityExtendedMappingEnabled();

    // Assert
    assertThat(result).isFalse();
  }
}
