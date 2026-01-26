package org.folio.entlinks.service.consortium.propagation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.CREATE;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.consortium.propagation.model.AuthorityDataStatsPropagationData;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

//todo: implement missing test cases
@UnitTest
@ExtendWith(MockitoExtension.class)
class ConsortiumAuthorityDataStatsPropagationServiceTest {

  private @Mock ConsortiumTenantsService tenantsService;
  private @Mock SystemUserScopedExecutionService executionService;
  private @Mock FolioExecutionContext folioExecutionContext;
  private @Mock AuthorityDataStatService authorityDataStatService;
  private @Mock InstanceAuthorityLinkingService linkingService;
  private @InjectMocks ConsortiumAuthorityDataStatsPropagationService propagationService;

  @Test
  void testPropagateCreate() {
    var propagationData = propagationData();

    doMocks();
    propagationService.propagate(propagationData, CREATE, TENANT_ID);

    var statsCaptor = ArgumentCaptor.<List<AuthorityDataStat>>captor();
    verify(tenantsService).getConsortiumTenants(TENANT_ID);
    verify(executionService, times(3)).executeAsyncSystemUserScoped(any(), any(), any());
    verify(authorityDataStatService, times(3)).createInBatch(statsCaptor.capture());

    var actualStats = statsCaptor.getAllValues();
    assertThat(actualStats)
      .hasSize(3)
      .allMatch(stats -> stats != propagationData.authorityDataStats())
      .allMatch(stats -> propagationData.authorityDataStats().toString().equals(stats.toString()));
  }

  @Test
  void testPropagateException() {
    doThrow(FolioIntegrationException.class).when(tenantsService).getConsortiumTenants(any());

    var propagationData = propagationData();

    propagationService.propagate(propagationData, CREATE, TENANT_ID);

    verify(tenantsService, times(1)).getConsortiumTenants(any());
    verify(executionService, times(0)).executeAsyncSystemUserScoped(any(), any(), any());
    verify(authorityDataStatService, times(0)).createInBatch(any());
  }

  private void doMocks() {
    when(tenantsService.getConsortiumTenants(TENANT_ID)).thenReturn(List.of("t1", "t2", "t3"));
    doAnswer(invocation -> {
      ((Runnable) invocation.getArgument(2)).run();
      return null;
    }).when(executionService).executeAsyncSystemUserScoped(any(), any(), any());
  }

  private AuthorityDataStatsPropagationData propagationData() {
    return AuthorityDataStatsPropagationData.forCreate(List.of(new AuthorityDataStat()));
  }
}
