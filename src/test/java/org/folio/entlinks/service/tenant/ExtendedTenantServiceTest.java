package org.folio.entlinks.service.tenant;

import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.entlinks.service.settings.TempSettingsMigrationService;
import org.folio.entlinks.service.kafka.ExtendedKafkaAdminService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ExtendedTenantServiceTest {

  @InjectMocks
  private ExtendedTenantService tenantService;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private ExtendedKafkaAdminService kafkaAdminService;
  @Mock
  private OkapiSystemUserService prepareSystemUserService;
  @Mock
  private TempSettingsMigrationService settingsMigrationService;

  @Test
  void initializeTenant_positive() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
    doNothing().when(prepareSystemUserService).prepareSystemUser();
    doNothing().when(kafkaAdminService).createTopics(TENANT_ID);
    doNothing().when(kafkaAdminService).restartEventListeners();

    tenantService.afterTenantUpdate(tenantAttributes());

    verify(prepareSystemUserService).prepareSystemUser();
    verify(kafkaAdminService).createTopics(TENANT_ID);
    verify(kafkaAdminService).restartEventListeners();
    verify(settingsMigrationService).migrateSettings();
  }

  @Test
  void deleteTopicAfterTenantDeletion() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
    tenantService.afterTenantDeletion(tenantAttributes());
    verify(kafkaAdminService).deleteTopics(anyString());
  }

  private TenantAttributes tenantAttributes() {
    return new TenantAttributes().moduleTo("mod-entities-links");
  }
}
