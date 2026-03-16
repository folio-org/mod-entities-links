package org.folio.entlinks.integration.kafka;

import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.diJobCanceledTopic;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.entlinks.integration.di.DataImportCanceledJobService;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class DataImportCanceledJobIT extends IntegrationTestBase {

  private @Autowired DataImportCanceledJobService dataImportCanceledJobService;

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @SneakyThrows
  @Test
  void shouldRegisterCancelledJobOnDiJobCancelledEvent_positive() {
    var tenantId = TENANT_ID;
    var jobId = UUID.randomUUID().toString();

    assertFalse(dataImportCanceledJobService.isJobCanceled(jobId, tenantId));

    sendKafkaMessage(diJobCanceledTopic(), jobId, new Object(), getDataImportCanceledJobKafkaHeaders(tenantId, jobId));

    awaitUntilAsserted(() -> assertTrue(dataImportCanceledJobService.isJobCanceled(jobId, tenantId)));
  }
}
