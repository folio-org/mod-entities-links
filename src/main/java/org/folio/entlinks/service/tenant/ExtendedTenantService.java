package org.folio.entlinks.service.tenant;

import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.service.dataloader.ReferenceDataLoader;
import org.folio.entlinks.service.kafka.ExtendedKafkaAdminService;
import org.folio.entlinks.service.settings.SettingsOverrideService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Primary
@Service
@Log4j2
public class ExtendedTenantService extends TenantService {

  private final FolioExecutionContext folioExecutionContext;
  private final ExtendedKafkaAdminService kafkaAdminService;
  private final ReferenceDataLoader referenceDataLoader;
  private final SettingsOverrideService settingsOverrideService;

  public ExtendedTenantService(JdbcTemplate jdbcTemplate,
                               FolioExecutionContext context,
                               ExtendedKafkaAdminService kafkaAdminService,
                               FolioSpringLiquibase folioSpringLiquibase,
                               FolioExecutionContext folioExecutionContext,
                               ReferenceDataLoader referenceDataLoader,
                               SettingsOverrideService settingsOverrideService) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.folioExecutionContext = folioExecutionContext;
    this.kafkaAdminService = kafkaAdminService;
    this.referenceDataLoader = referenceDataLoader;
    this.settingsOverrideService = settingsOverrideService;
  }

  @Override
  public void loadReferenceData() {
    referenceDataLoader.loadRefData();
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    super.afterTenantUpdate(tenantAttributes);
    kafkaAdminService.createTopics(folioExecutionContext.getTenantId());
    kafkaAdminService.restartEventListeners();
    settingsOverrideService.overrideSettings();
  }

  @Override
  protected void afterTenantDeletion(TenantAttributes tenantAttributes) {
    var tenantId = context.getTenantId();
    kafkaAdminService.deleteTopics(tenantId);
  }
}
