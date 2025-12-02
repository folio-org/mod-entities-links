package org.folio.entlinks.service.messaging.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.integration.dto.AuthoritySourceRecord;
import org.folio.entlinks.integration.dto.event.AuthorityDeleteEventSubType;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.folio.entlinks.integration.dto.event.DomainEventType;
import org.folio.entlinks.integration.internal.AuthoritySourceRecordService;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.handler.AuthorityChangeHandler;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityLinkUpdateServiceTest {

  private @Captor ArgumentCaptor<List<LinksChangeEvent>> eventCaptor;
  private @Captor ArgumentCaptor<List<AuthorityChangeHolder>> changeHolderCaptor;

  private @Mock EventProducer<LinksChangeEvent> eventProducer;
  private @Mock AuthorityDataStatService authorityDataStatService;

  private @Mock AuthorityChangeHandler updateHandler;
  private @Mock AuthorityChangeHandler deleteHandler;
  private @Mock AuthorityMappingRulesProcessingService mappingRulesProcessingService;
  private @Mock InstanceAuthorityLinkingService linkingService;
  private @Mock AuthoritySourceRecordService sourceRecordService;
  private @Mock ConsortiumTenantsService consortiumTenantsService;
  private @Mock FolioExecutionContext folioExecutionContext;
  private @Mock SystemUserScopedExecutionService executionService;

  private InstanceAuthorityLinkUpdateService service;

  @BeforeEach
  void setUp() {
    when(updateHandler.supportedAuthorityChangeType()).thenReturn(AuthorityChangeType.UPDATE);
    when(deleteHandler.supportedAuthorityChangeType()).thenReturn(AuthorityChangeType.DELETE);

    service = new InstanceAuthorityLinkUpdateService(authorityDataStatService,
      mappingRulesProcessingService, linkingService, eventProducer, List.of(updateHandler, deleteHandler),
      sourceRecordService, consortiumTenantsService, folioExecutionContext, executionService);
  }

  @Test
  void handleAuthoritiesChanges_positive_updateEvent() {
    final var id = UUID.randomUUID();
    final var authorityEvents = List.of(
      new AuthorityDomainEvent(id, null, new AuthorityDto().naturalId("new").personalName("test"),
        DomainEventType.UPDATE, TENANT_ID));
    final var sourceRecord = new AuthoritySourceRecord(null, null, null);

    var expected = new LinksChangeEvent().type(LinksChangeEvent.TypeEnum.UPDATE);
    when(linkingService.countLinksByAuthorityIds(Set.of(id))).thenReturn(Map.of(id, 1));
    when(sourceRecordService.getAuthoritySourceRecordById(any())).thenReturn(sourceRecord);
    when(updateHandler.handle(changeHolderCaptor.capture())).thenReturn(List.of(expected));

    service.handleAuthoritiesChanges(authorityEvents);

    verify(eventProducer).sendMessages(eventCaptor.capture());
    verify(authorityDataStatService).createInBatch(anyList());
    verifyNoMoreInteractions(authorityDataStatService);

    var changeHolders = changeHolderCaptor.getValue();

    assertThat(changeHolders)
      .isNotEmpty()
      .allMatch(changeHolder -> changeHolder.getSourceRecord() == sourceRecord);

    var messages = eventCaptor.getValue();
    assertThat(messages).hasSize(1);
    assertThat(messages.getFirst().getType()).isEqualTo(LinksChangeEvent.TypeEnum.UPDATE);
  }

  @Test
  void handleAuthoritiesChanges_positive_updateEventWhenNaturalIdChanged() {
    final var id = UUID.randomUUID();
    final var authorityEvents = List.of(
      new AuthorityDomainEvent(id, new AuthorityDto().naturalId("n101010").personalName("test")
        .sourceFileId(UUID.randomUUID()),
        new AuthorityDto().naturalId("101010").personalName("test"),
        DomainEventType.UPDATE, TENANT_ID));

    var expected = new LinksChangeEvent().type(LinksChangeEvent.TypeEnum.UPDATE);
    when(linkingService.countLinksByAuthorityIds(Set.of(id))).thenReturn(Map.of(id, 1));
    when(updateHandler.handle(changeHolderCaptor.capture())).thenReturn(List.of(expected));

    service.handleAuthoritiesChanges(authorityEvents);

    verify(eventProducer).sendMessages(eventCaptor.capture());
    verify(authorityDataStatService).createInBatch(anyList());
    verifyNoMoreInteractions(authorityDataStatService);

    var changeHolders = changeHolderCaptor.getValue();

    assertThat(changeHolders)
      .isNotEmpty()
      .allMatch(changeHolder -> changeHolder.getSourceRecord() == null);

    var messages = eventCaptor.getValue();
    assertThat(messages).hasSize(1);
    assertThat(messages.getFirst().getType()).isEqualTo(LinksChangeEvent.TypeEnum.UPDATE);
  }

  @Test
  void handleAuthoritiesChanges_positive_updateEventWhenNoLinksExistAndOnlyNaturalIdChanged() {
    final var id = UUID.randomUUID();
    final var authorityEvents = List.of(
      new AuthorityDomainEvent(id, null, new AuthorityDto().naturalId("new"), DomainEventType.UPDATE, TENANT_ID));

    when(linkingService.countLinksByAuthorityIds(Set.of(id))).thenReturn(Collections.emptyMap());

    service.handleAuthoritiesChanges(authorityEvents);

    verify(eventProducer, never()).sendMessages(eventCaptor.capture());
    verify(authorityDataStatService).createInBatch(anyList());
    verifyNoMoreInteractions(authorityDataStatService);
    verifyNoInteractions(sourceRecordService);
  }

  @Test
  void handleAuthoritiesChanges_positive_deleteEvent() {
    final var id = UUID.randomUUID();
    final var authorityEvents = List.of(
      new AuthorityDomainEvent(id, new AuthorityDto().naturalId("old"), null, DomainEventType.DELETE,
          AuthorityDeleteEventSubType.SOFT_DELETE, TENANT_ID));

    var changeEvent = new LinksChangeEvent().type(LinksChangeEvent.TypeEnum.DELETE);

    when(linkingService.countLinksByAuthorityIds(Set.of(id))).thenReturn(Map.of(id, 1));
    when(deleteHandler.handle(any())).thenReturn(List.of(changeEvent));

    service.handleAuthoritiesChanges(authorityEvents);

    verify(eventProducer).sendMessages(eventCaptor.capture());
    verify(authorityDataStatService).createInBatch(anyList());
    verifyNoInteractions(sourceRecordService);

    var messages = eventCaptor.getValue();
    assertThat(messages).hasSize(1);
    assertThat(messages.getFirst().getType()).isEqualTo(LinksChangeEvent.TypeEnum.DELETE);
  }

  @Test
  void handleAuthoritiesChanges_positive_shouldHandleHeadingTypeChangeUpdateEventAndSendDeleteLinksChangeEvent() {
    var id = UUID.randomUUID();
    final var authorityEvents = List.of(
        new AuthorityDomainEvent(
            id,
            new AuthorityDto().naturalId("n12345").corporateName("Beatles"),
            new AuthorityDto().naturalId("n12345").corporateNameTitle("Beatles mono"),
            DomainEventType.UPDATE,
            TENANT_ID)
    );
    var changeEvent = new LinksChangeEvent().type(LinksChangeEvent.TypeEnum.DELETE);
    when(linkingService.countLinksByAuthorityIds(Set.of(id))).thenReturn(Map.of(id, 1));
    when(deleteHandler.handle(changeHolderCaptor.capture())).thenReturn(List.of(changeEvent));
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);

    service.handleAuthoritiesChanges(authorityEvents);

    verify(updateHandler).supportedAuthorityChangeType();
    verify(deleteHandler).supportedAuthorityChangeType();
    verifyNoMoreInteractions(updateHandler);
    verify(eventProducer, times(1)).sendMessages(eventCaptor.capture());
    var changeHolders = changeHolderCaptor.getAllValues().stream().flatMap(Collection::stream).toList();
    assertThat(changeHolders)
        .hasSize(1)
        .extracting(AuthorityChangeHolder::getNumberOfLinks)
        .containsExactlyInAnyOrder(1);
    var messages = eventCaptor.getAllValues().stream().flatMap(Collection::stream).toList();
    assertThat(messages).hasSize(1);
    assertThat(messages.getFirst().getType()).isEqualTo(LinksChangeEvent.TypeEnum.DELETE);
  }

  @Test
  void handleAuthoritiesChanges_positive_updateEventOnConsortiumCentralTenant() {
    final var id = UUID.randomUUID();
    final var authorityEvents = List.of(
      new AuthorityDomainEvent(id, null, new AuthorityDto().naturalId("new").personalName("test"),
        DomainEventType.UPDATE, TENANT_ID));
    final var sourceRecord = new AuthoritySourceRecord(null, null, null);
    final var memberTenants = List.of("tenant1", "tenant2");

    var expected = new LinksChangeEvent().type(LinksChangeEvent.TypeEnum.UPDATE);
    when(linkingService.countLinksByAuthorityIds(Set.of(id)))
      .thenReturn(new HashMap<>(Map.of(id, 1)))
      .thenReturn(new HashMap<>(Map.of(id, 2)))
      .thenReturn(new HashMap<>(Map.of(id, 3)));
    when(linkingService.countLinksByAuthorityIds(Set.of(id), TENANT_ID))
      .thenReturn(new HashMap<>(Map.of(id, 2)));
    when(sourceRecordService.getAuthoritySourceRecordById(any())).thenReturn(sourceRecord);
    when(updateHandler.handle(changeHolderCaptor.capture())).thenReturn(List.of(expected));
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.fromString(USER_ID));
    when(consortiumTenantsService.getConsortiumTenants(TENANT_ID)).thenReturn(memberTenants);
    mockExecutionService();

    service.handleAuthoritiesChanges(authorityEvents);

    verify(eventProducer, times(3)).sendMessages(eventCaptor.capture());
    verify(executionService).executeSystemUserScoped(eq(memberTenants.get(0)), eq(USER_ID), any());
    verify(executionService).executeSystemUserScoped(eq(memberTenants.get(1)), eq(USER_ID), any());

    var messages = eventCaptor.getAllValues().stream().flatMap(Collection::stream).toList();
    assertThat(messages).hasSize(3);
    assertThat(messages.getFirst().getType()).isEqualTo(LinksChangeEvent.TypeEnum.UPDATE);

    var changeHolders = changeHolderCaptor.getAllValues().stream().flatMap(Collection::stream).toList();
    assertThat(changeHolders)
      .hasSize(3)
      .allMatch(changeHolder -> changeHolder.getSourceRecord() == sourceRecord)
      .extracting(AuthorityChangeHolder::getNumberOfLinks)
      .containsExactlyInAnyOrder(1, 4, 5);

    verify(authorityDataStatService, times(3)).createInBatch(anyList());
  }

  @SuppressWarnings("unchecked")
  private void mockExecutionService() {
    doAnswer(invocationOnMock -> ((Callable<Object>) invocationOnMock.getArgument(2)).call())
      .when(executionService).executeSystemUserScoped(any(), anyString(), any());
  }
}
