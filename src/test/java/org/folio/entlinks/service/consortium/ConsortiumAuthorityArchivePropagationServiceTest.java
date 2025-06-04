package org.folio.entlinks.service.consortium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.AUTHORITY_CONSORTIUM_SOURCE;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.service.authority.AuthorityArchiveService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityArchivePropagationService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityPropagationService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ConsortiumAuthorityArchivePropagationServiceTest {

  @Mock
  private AuthorityArchiveService authorityArchiveService;

  @Mock
  private ConsortiumTenantsService tenantsService;

  @Mock
  private SystemUserScopedExecutionService executionService;

  @InjectMocks
  private ConsortiumAuthorityArchivePropagationService propagationService;

  @Test
  void testPropagationDelete() {
    var archive = authorityArchive();
    archive.setId(UUID.randomUUID());
    doMocks();

    propagationService.propagate(archive, ConsortiumAuthorityPropagationService.PropagationType.DELETE, TENANT_ID);

    assertThat(archive.getSource()).isEqualTo(AUTHORITY_CONSORTIUM_SOURCE);
    verify(tenantsService).getConsortiumTenants(TENANT_ID);
    verify(executionService, times(3)).executeAsyncSystemUserScoped(any(), any());
    verify(authorityArchiveService, times(3)).delete(archive);
  }

  @Test
  void testPropagateIllegalPropagationType() {
    var archive = authorityArchive();
    archive.setId(UUID.randomUUID());

    doMocks();

    var exception = assertThrows(IllegalArgumentException.class,
        () -> propagationService.propagate(archive, ConsortiumAuthorityPropagationService.PropagationType.CREATE,
            TENANT_ID));
    assertEquals("Propagation type 'CREATE' is not supported for authority archives.", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class,
        () -> propagationService.propagate(archive, ConsortiumAuthorityPropagationService.PropagationType.UPDATE,
            TENANT_ID));
    assertEquals("Propagation type 'UPDATE' is not supported for authority archives.", exception.getMessage());
  }

  private AuthorityArchive authorityArchive() {
    var archive = new AuthorityArchive();
    archive.setSource("SOURCE");
    return archive;
  }

  private void doMocks() {
    when(tenantsService.getConsortiumTenants(TENANT_ID)).thenReturn(List.of("t1", "t2", "t3"));
    doAnswer(invocation -> {
      ((Runnable) invocation.getArgument(1)).run();
      return null;
    }).when(executionService).executeAsyncSystemUserScoped(any(), any());
  }
}
