package org.folio.entlinks.service.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.service.tenant.TempSettingsMigrationService.AUTHORITIES_SETTINGS_GROUP;
import static org.folio.entlinks.service.tenant.TempSettingsMigrationService.MAPPING_EXTENDED_SETTING;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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

  @Captor
  private ArgumentCaptor<SettingUpdateRequest> settingUpdateRequestCaptor;

  @Test
  void migrateSettings_shouldUpdateSettingWithCorrectParameters() {
    ReflectionTestUtils.setField(service, "isAuthorityExtended", true);
    service.migrateSettings();

    verify(tenantSettingsService).updateGroupSetting(
      eq(AUTHORITIES_SETTINGS_GROUP),
      eq(MAPPING_EXTENDED_SETTING),
      settingUpdateRequestCaptor.capture()
    );

    var capturedRequest = settingUpdateRequestCaptor.getValue();
    assertThat(capturedRequest.getValue()).isInstanceOf(Boolean.class);
    assertThat(capturedRequest.getValue()).isEqualTo(Boolean.TRUE);
    assertThat(capturedRequest.getDescription()).isEqualTo("Enable extended mapping for authorities");
  }
}
