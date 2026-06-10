package org.folio.entlinks.service.settings;

import static org.folio.entlinks.service.settings.TenantSetting.MAPPING_EXTENDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SettingsOverrideServiceTest {

  @Mock
  private SettingsService settingsService;

  @Test
  void overrideSettings_positive_whenAuthorityExtendedIsTrue() {
    // Arrange
    var service = new SettingsOverrideService(settingsService);
    ReflectionTestUtils.setField(service, "isAuthorityExtended", Optional.of(true));

    // Act
    service.overrideSettings();

    // Assert
    ArgumentCaptor<SettingUpdateRequest> requestCaptor = ArgumentCaptor.forClass(SettingUpdateRequest.class);
    verify(settingsService).updateGroupSetting(eq(MAPPING_EXTENDED.getGroup()), eq(MAPPING_EXTENDED.getKey()),
      requestCaptor.capture());

    var capturedRequest = requestCaptor.getValue();
    assertEquals(true, capturedRequest.getValue());
  }

  @Test
  void overrideSettings_positive_whenAuthorityExtendedIsFalse() {
    // Arrange
    var service = new SettingsOverrideService(settingsService);
    ReflectionTestUtils.setField(service, "isAuthorityExtended", Optional.of(false));

    // Act
    service.overrideSettings();

    // Assert
    ArgumentCaptor<SettingUpdateRequest> requestCaptor = ArgumentCaptor.forClass(SettingUpdateRequest.class);
    verify(settingsService).updateGroupSetting(eq(MAPPING_EXTENDED.getGroup()), eq(MAPPING_EXTENDED.getKey()),
      requestCaptor.capture());

    var capturedRequest = requestCaptor.getValue();
    assertEquals(false, capturedRequest.getValue());
  }

  @Test
  void overrideSettings_negative_whenAuthorityExtendedIsNotPresent() {
    // Arrange
    var service = new SettingsOverrideService(settingsService);
    ReflectionTestUtils.setField(service, "isAuthorityExtended", Optional.empty());

    // Act
    service.overrideSettings();

    // Assert
    verifyNoInteractions(settingsService);
  }
}
