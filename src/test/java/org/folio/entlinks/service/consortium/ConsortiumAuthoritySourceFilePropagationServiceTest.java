package org.folio.entlinks.service.consortium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.entity.AuthoritySourceFileSource.LOCAL;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.TEST_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthoritySourceFilePropagationService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService;
import org.folio.entlinks.service.consortium.propagation.model.AuthoritySourceFilePropagationData;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ConsortiumAuthoritySourceFilePropagationServiceTest {

  private @Mock AuthoritySourceFileService authorityService;
  private @Mock ConsortiumTenantsService tenantsService;
  private @Mock SystemUserScopedExecutionService executionService;
  private @Mock FolioExecutionContext context;
  private @InjectMocks ConsortiumAuthoritySourceFilePropagationService propagationService;

  @BeforeEach
  void setUp() {
    when(context.getUserId()).thenReturn(TEST_ID);
  }

  @Test
  void testPropagateCreate() throws FolioIntegrationException {
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setSource(LOCAL);
    doMocks();
    propagationService.propagate(getMockData(sourceFile),
      ConsortiumPropagationService.PropagationType.CREATE, TENANT_ID);

    assertThat(sourceFile.getSource()).isEqualTo(LOCAL);
    verify(tenantsService).getConsortiumTenants(TENANT_ID);
    verify(executionService, times(3)).executeAsyncSystemUserScoped(any(), anyString(), any());
    verify(authorityService, times(3)).create(sourceFile);
  }

  @Test
  void testPropagateUpdate() throws FolioIntegrationException {
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setId(UUID.randomUUID());
    sourceFile.setSource(LOCAL);
    doMocks();
    propagationService.propagate(getMockData(sourceFile),
      ConsortiumPropagationService.PropagationType.UPDATE, TENANT_ID);

    assertThat(sourceFile.getSource()).isEqualTo(LOCAL);
    verify(tenantsService, times(1)).getConsortiumTenants(any());
    verify(executionService, times(3)).executeAsyncSystemUserScoped(any(), anyString(), any());
    verify(authorityService, times(3)).update(sourceFile.getId(), sourceFile, null);
  }

  @Test
  void testPropagateDelete() throws FolioIntegrationException {
    var sourceFile = new AuthoritySourceFile();
    sourceFile.setId(UUID.randomUUID());
    sourceFile.setSource(LOCAL);
    doMocks();
    propagationService.propagate(getMockData(sourceFile),
      ConsortiumPropagationService.PropagationType.DELETE, TENANT_ID);

    assertThat(sourceFile.getSource()).isEqualTo(LOCAL);
    verify(tenantsService, times(1)).getConsortiumTenants(any());
    verify(executionService, times(3)).executeAsyncSystemUserScoped(any(), anyString(), any());
    verify(authorityService, times(3)).deleteById(sourceFile.getId());
  }

  @Test
  void testPropagateException() throws FolioIntegrationException {
    Mockito.doThrow(FolioIntegrationException.class).when(tenantsService).getConsortiumTenants(any());

    var sourceFile = new AuthoritySourceFile();
    propagationService.propagate(getMockData(sourceFile),
      ConsortiumPropagationService.PropagationType.CREATE, TENANT_ID);

    verify(tenantsService, times(1)).getConsortiumTenants(any());
    verify(executionService, times(0)).executeAsyncSystemUserScoped(any(), any());
    verify(authorityService, times(0)).create(any());
  }

  private void doMocks() {
    when(tenantsService.getConsortiumTenants(TENANT_ID)).thenReturn(List.of("t1", "t2", "t3"));
    doAnswer(invocation -> {
      ((Runnable) invocation.getArgument(2)).run();
      return null;
    }).when(executionService).executeAsyncSystemUserScoped(any(), any(), any());
  }

  private AuthoritySourceFilePropagationData<AuthoritySourceFile> getMockData(AuthoritySourceFile asf) {
    return new AuthoritySourceFilePropagationData<>(asf, null);
  }
}
