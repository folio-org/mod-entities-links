package org.folio.entlinks.service.messaging.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.entlinks.domain.dto.AuthorityInventoryRecord;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.handler.AuthorityChangeHandler;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityLinkUpdateServiceTest {

  private static final UUID ID = UUID.randomUUID();
  private @Captor ArgumentCaptor<List<LinksChangeEvent>> argumentCaptor;

  private @Mock EventProducer<LinksChangeEvent> eventProducer;
  private @Mock AuthorityDataStatService authorityDataStatService;

  private @Mock AuthorityChangeHandler updateHandler;
  private @Mock AuthorityChangeHandler deleteHandler;
  private @Mock AuthorityMappingRulesProcessingService mappingRulesProcessingService;
  private @Mock InstanceAuthorityLinkingService linkingService;
  private InstanceAuthorityLinkUpdateService service;

  @BeforeEach
  void setUp() {
    when(updateHandler.supportedAuthorityChangeType()).thenReturn(AuthorityChangeType.UPDATE);
    when(deleteHandler.supportedAuthorityChangeType()).thenReturn(AuthorityChangeType.DELETE);

    service = new InstanceAuthorityLinkUpdateService(authorityDataStatService,
      mappingRulesProcessingService, linkingService, eventProducer, List.of(updateHandler, deleteHandler));
  }

  @Test
  void handleAuthoritiesChanges_positive_updateEvent() {
    final var id = UUID.randomUUID();
    final var inventoryEvents = List.of(new InventoryEvent().id(id)
      .type("UPDATE")._new(new AuthorityInventoryRecord().naturalId("new")));

    var expected = new LinksChangeEvent().type(LinksChangeEvent.TypeEnum.UPDATE);
    when(linkingService.countLinksByAuthorityIds(Set.of(id))).thenReturn(Map.of(id, 1));
    when(updateHandler.handle(anyList())).thenReturn(List.of(expected));

    service.handleAuthoritiesChanges(inventoryEvents);

    verify(eventProducer).sendMessages(argumentCaptor.capture());
    verify(authorityDataStatService).createInBatch(anyList());

    var messages = argumentCaptor.getValue();
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getType()).isEqualTo(LinksChangeEvent.TypeEnum.UPDATE);
  }

  @Test
  void handleAuthoritiesChanges_positive_updateEventWhenNoLinksExist() {
    final var inventoryEvents = List.of(new InventoryEvent().id(ID)
      .type("UPDATE")._new(new AuthorityInventoryRecord().naturalId("new")));

    when(linkingService.countLinksByAuthorityIds(Set.of(ID))).thenReturn(Collections.emptyMap());

    service.handleAuthoritiesChanges(inventoryEvents);

    verify(eventProducer, never()).sendMessages(argumentCaptor.capture());
    verify(authorityDataStatService).createInBatch(anyList());
  }

  @ParameterizedTest
  @MethodSource("linksTestCases")
  void handleAuthoritiesChanges_positive_deleteEventWithAndWithoutLinks(Map<UUID, Integer> map, int messageSize) {
    final var inventoryEvents = List.of(new InventoryEvent().id(ID)
      .type("DELETE").old(new AuthorityInventoryRecord().naturalId("old")));

    var changeEvent = new LinksChangeEvent().type(LinksChangeEvent.TypeEnum.DELETE);

    when(linkingService.countLinksByAuthorityIds(Set.of(ID))).thenReturn(map);
    when(deleteHandler.handle(any())).thenReturn(List.of(changeEvent));

    service.handleAuthoritiesChanges(inventoryEvents);

    if (messageSize != 0) {
      // when authority has links
      verify(eventProducer).sendMessages(argumentCaptor.capture());
      verify(authorityDataStatService).createInBatch(anyList());
      var messages = argumentCaptor.getValue();
      assertThat(messages).hasSize(messageSize);
      assertThat(messages.get(0).getType()).isEqualTo(LinksChangeEvent.TypeEnum.DELETE);
    } else {
      // when authority doesn’t have links
      verify(eventProducer, never()).sendMessages(argumentCaptor.capture());
      verify(authorityDataStatService).createInBatch(anyList());
    }
  }

  public static Stream<Arguments> linksTestCases() {
    return Stream.of(
      Arguments.of(Map.of(ID, 1), 1),
      Arguments.of(Collections.emptyMap(), 0)
    );
  }
}
