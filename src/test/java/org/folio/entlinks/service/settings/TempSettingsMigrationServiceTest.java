package org.folio.entlinks.service.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.service.settings.TenantSetting.ARCHIVES_EXPIRATION_ENABLED;
import static org.folio.entlinks.service.settings.TenantSetting.ARCHIVES_EXPIRATION_PERIOD;
import static org.folio.entlinks.service.settings.TenantSetting.MAPPING_EXTENDED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.client.SettingsClient;
import org.folio.entlinks.config.properties.AuthorityArchiveProperties;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.integration.SettingsService;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.service.TenantSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TempSettingsMigrationServiceTest {

  @InjectMocks
  private TempSettingsMigrationService service;

  @Mock
  private TenantSettingsService tenantSettingsService;

  @Mock
  private SettingsService settingsService;

  @Mock
  private AuthorityArchiveProperties authorityArchiveProperties;

  @Captor
  private ArgumentCaptor<SettingUpdateRequest> settingUpdateRequestCaptor;

  @Test
  void migrateSettings_shouldUpdateMappingExtendedSetting_whenAuthorityExtendedIsTrue() {
    ReflectionTestUtils.setField(service, "isAuthorityExtended", true);
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.empty());
    when(authorityArchiveProperties.getRetentionPeriodInDays()).thenReturn(7);

    service.migrateSettings();

    verify(tenantSettingsService).updateGroupSetting(
      eq(MAPPING_EXTENDED.getGroup()),
      eq(MAPPING_EXTENDED.getKey()),
      settingUpdateRequestCaptor.capture()
    );

    var capturedRequest = settingUpdateRequestCaptor.getValue();
    assertThat(capturedRequest.getValue()).isInstanceOf(Boolean.class);
    assertThat(capturedRequest.getValue()).isEqualTo(Boolean.TRUE);
    assertThat(capturedRequest.getDescription()).isEqualTo("Enable extended mapping for authorities");
  }

  @Test
  void migrateSettings_shouldNotUpdateMappingExtendedSetting_whenAuthorityExtendedIsFalse() {
    ReflectionTestUtils.setField(service, "isAuthorityExtended", false);
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.empty());
    when(authorityArchiveProperties.getRetentionPeriodInDays()).thenReturn(7);

    service.migrateSettings();

    verify(tenantSettingsService).updateGroupSetting(
      eq(ARCHIVES_EXPIRATION_PERIOD.getGroup()),
      eq(ARCHIVES_EXPIRATION_PERIOD.getKey()),
      settingUpdateRequestCaptor.capture()
    );
    verifyNoMoreInteractions(tenantSettingsService);
  }

  @Test
  void migrateExpirationSettings_shouldUpdateExpirationPeriod_whenExpirationSettingIsEmpty() {
    ReflectionTestUtils.setField(service, "isAuthorityExtended", false);
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.empty());
    when(authorityArchiveProperties.getRetentionPeriodInDays()).thenReturn(14);

    service.migrateSettings();

    verify(tenantSettingsService).updateGroupSetting(
      eq(ARCHIVES_EXPIRATION_PERIOD.getGroup()),
      eq(ARCHIVES_EXPIRATION_PERIOD.getKey()),
      settingUpdateRequestCaptor.capture()
    );

    var capturedRequest = settingUpdateRequestCaptor.getValue();
    assertThat(capturedRequest.getValue()).isEqualTo(14);
  }

  @Test
  void migrateExpirationSettings_shouldDisableExpirationAndUpdatePeriod_whenExpirationIsDisabled() {
    ReflectionTestUtils.setField(service, "isAuthorityExtended", false);
    var settingValue = new SettingsClient.AuthoritiesExpirationSettingValue(false, 30);
    var settingEntry = new SettingsClient.SettingEntry(UUID.randomUUID(), "scope", "key", settingValue);
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.of(settingEntry));

    service.migrateSettings();

    verify(tenantSettingsService).updateGroupSetting(
      eq(ARCHIVES_EXPIRATION_ENABLED.getGroup()),
      eq(ARCHIVES_EXPIRATION_ENABLED.getKey()),
      settingUpdateRequestCaptor.capture()
    );

    var capturedRequest = settingUpdateRequestCaptor.getValue();
    assertThat(capturedRequest.getValue()).isEqualTo(Boolean.FALSE);
  }

  @Test
  void migrateExpirationSettings_shouldUpdatePeriodFromSetting_whenExpirationIsEnabled() {
    ReflectionTestUtils.setField(service, "isAuthorityExtended", false);
    var settingValue = new SettingsClient.AuthoritiesExpirationSettingValue(true, 21);
    var settingEntry = new SettingsClient.SettingEntry(UUID.randomUUID(), "scope", "key", settingValue);
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.of(settingEntry));

    service.migrateSettings();

    verify(tenantSettingsService).updateGroupSetting(
      eq(ARCHIVES_EXPIRATION_PERIOD.getGroup()),
      eq(ARCHIVES_EXPIRATION_PERIOD.getKey()),
      settingUpdateRequestCaptor.capture()
    );

    var capturedRequest = settingUpdateRequestCaptor.getValue();
    assertThat(capturedRequest.getValue()).isEqualTo(21);
  }

  @Test
  void migrateExpirationSettings_shouldUseDefaultRetentionPeriod_whenSettingValueIsNull() {
    ReflectionTestUtils.setField(service, "isAuthorityExtended", false);
    var settingEntry = new SettingsClient.SettingEntry(UUID.randomUUID(), "scope", "key", null);
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.of(settingEntry));
    when(authorityArchiveProperties.getRetentionPeriodInDays()).thenReturn(10);

    service.migrateSettings();

    verify(tenantSettingsService).updateGroupSetting(
      eq(ARCHIVES_EXPIRATION_PERIOD.getGroup()),
      eq(ARCHIVES_EXPIRATION_PERIOD.getKey()),
      settingUpdateRequestCaptor.capture()
    );

    var capturedRequest = settingUpdateRequestCaptor.getValue();
    assertThat(capturedRequest.getValue()).isEqualTo(10);
  }

  @Test
  void migrateExpirationSettings_shouldHandleException_whenSettingsServiceThrowsException() {
    ReflectionTestUtils.setField(service, "isAuthorityExtended", false);
    when(settingsService.getAuthorityExpireSetting())
      .thenThrow(new FolioIntegrationException("Failed to fetch settings"));
    when(authorityArchiveProperties.getRetentionPeriodInDays()).thenReturn(7);

    service.migrateSettings();

    verify(tenantSettingsService).updateGroupSetting(
      eq(ARCHIVES_EXPIRATION_PERIOD.getGroup()),
      eq(ARCHIVES_EXPIRATION_PERIOD.getKey()),
      settingUpdateRequestCaptor.capture()
    );

    var capturedRequest = settingUpdateRequestCaptor.getValue();
    assertThat(capturedRequest.getValue()).isEqualTo(7);
  }
}
