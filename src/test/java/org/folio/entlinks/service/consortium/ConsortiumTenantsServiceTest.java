package org.folio.entlinks.service.consortium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.CENTRAL_TENANT_ID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.folio.entlinks.client.ConsortiumTenantsClient;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class ConsortiumTenantsServiceTest {

  public static final String TEST_TENANT_ID = "testTenantId";
  public static final String TEST_CONSORTIUM_ID = "testConsortiumId";
  public static final int DEFAULT_REQUEST_LIMIT = 10000;
  public static final String TENANT = "tenant1";
  private @Mock ConsortiumTenantsClient tenantsClient;
  private @Mock UserTenantsService userTenantsService;
  private @Mock FolioExecutionContext context;
  private @InjectMocks ConsortiumTenantsService consortiumTenantsService;

  @Test
  void testGetConsortiumTenants() {
    String tenantId = TEST_TENANT_ID;
    String consortiumId = TEST_CONSORTIUM_ID;
    var consortiumTenantList = Collections.singletonList(
        new ConsortiumTenantsClient.ConsortiumTenant(TENANT, false)
    );
    var consortiumTenants = new ConsortiumTenantsClient.ConsortiumTenants(consortiumTenantList);

    when(userTenantsService.getConsortiumId(tenantId)).thenReturn(Optional.of(consortiumId));
    when(tenantsClient.getConsortiumTenants(consortiumId, DEFAULT_REQUEST_LIMIT)).thenReturn(consortiumTenants);

    List<String> result = consortiumTenantsService.getConsortiumTenants(tenantId);

    assertThat(result.getFirst()).isEqualTo(TENANT);
  }

  @Test
  void testGetConsortiumTenantsWithException() {
    String tenantId = TEST_TENANT_ID;

    doThrow(new RuntimeException("Simulated exception")).when(userTenantsService).getConsortiumId(tenantId);

    List<String> result = consortiumTenantsService.getConsortiumTenants(tenantId);

    assertThat(result).isEmpty();
  }

  @Test
  void testIsCentralTenantContext_WhenCurrentTenantIsCentral_ShouldReturnTrue() {
    when(context.getTenantId()).thenReturn(CENTRAL_TENANT_ID);
    when(userTenantsService.getCentralTenant(CENTRAL_TENANT_ID)).thenReturn(Optional.of(CENTRAL_TENANT_ID));

    var result = consortiumTenantsService.isCentralTenantContext();

    assertThat(result).isTrue();
  }

  @Test
  void testIsCentralTenantContext_WhenCurrentTenantIsNotCentral_ShouldReturnFalse() {
    when(context.getTenantId()).thenReturn(TEST_TENANT_ID);
    when(userTenantsService.getCentralTenant(TEST_TENANT_ID)).thenReturn(Optional.of(CENTRAL_TENANT_ID));

    var result = consortiumTenantsService.isCentralTenantContext();

    assertThat(result).isFalse();
  }

  @Test
  void testIsCentralTenantContext_WhenNoCentralTenantFound_ShouldReturnFalse() {
    when(context.getTenantId()).thenReturn(TEST_TENANT_ID);
    when(userTenantsService.getCentralTenant(TEST_TENANT_ID)).thenReturn(Optional.empty());

    var result = consortiumTenantsService.isCentralTenantContext();

    assertThat(result).isFalse();
  }
}
