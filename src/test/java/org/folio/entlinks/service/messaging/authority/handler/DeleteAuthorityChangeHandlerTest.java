package org.folio.entlinks.service.messaging.authority.handler;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.folio.entlinks.domain.dto.LinksChangeEvent.TypeEnum;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.CORPORATE_NAME;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField.CORPORATE_NAME_TITLE;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.config.properties.InstanceAuthorityChangeProperties;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.ChangeTarget;
import org.folio.entlinks.domain.dto.ChangeTargetLink;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.folio.entlinks.integration.dto.event.DomainEventType;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChange;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.folio.spring.testing.type.UnitTest;
import org.folio.support.TestDataUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DeleteAuthorityChangeHandlerTest {

  private @Mock InstanceAuthorityLinkingService linkingService;
  private @Mock InstanceAuthorityChangeProperties properties;
  private @Mock AuthorityService authorityService;
  private @InjectMocks DeleteAuthorityChangeHandler handler;

  @Test
  void getReplyEventType_positive() {
    var actual = handler.getReplyEventType();

    assertEquals(TypeEnum.DELETE, actual);
  }

  @Test
  void supportedInventoryEventType_positive() {
    var actual = handler.supportedAuthorityChangeType();

    assertEquals(AuthorityChangeType.DELETE, actual);
  }

  @Test
  void handle_positive_shouldHardDeleteAuthorityAndLinks() {
    var id1 = UUID.randomUUID();
    var id2 = UUID.randomUUID();
    var authorityDomainEvent1 = prepareAuthorityDeleteEvent(id1);
    var authorityDomainEvent2 = prepareAuthorityDeleteEvent(id2);
    var events = List.of(
      new AuthorityChangeHolder(authorityDomainEvent1, emptyMap(), emptyMap(), 1),
      new AuthorityChangeHolder(authorityDomainEvent2, emptyMap(), emptyMap(), 1)
    );
    var instanceId1 = UUID.randomUUID();
    var instanceId2 = UUID.randomUUID();
    var instanceId3 = UUID.randomUUID();
    var link1 = TestDataUtils.Link.of(1, 1);
    var link2 = TestDataUtils.Link.of(0, 2);
    var link3 = TestDataUtils.Link.of(2, 1);

    doNothing().when(linkingService).deleteByAuthorityIdIn(anySet());
    when(properties.getNumPartitions()).thenReturn(1);
    when(linkingService.getLinksByAuthorityId(eq(id1), any()))
      .thenReturn(new PageImpl<>(List.of(link1.toEntity(instanceId1)), Pageable.ofSize(1), 2))
      .thenReturn(new PageImpl<>(List.of(link2.toEntity(instanceId2))));
    when(linkingService.getLinksByAuthorityId(eq(id2), any()))
      .thenReturn(new PageImpl<>(List.of(link3.toEntity(instanceId3))));

    var actual = handler.handle(events);

    verify(linkingService).deleteByAuthorityIdIn(Set.of(id1, id2));
    verify(linkingService, times(3)).getLinksByAuthorityId(any(UUID.class), any(Pageable.class));

    assertThat(actual)
      .hasSize(3)
      .extracting(LinksChangeEvent::getAuthorityId, LinksChangeEvent::getType, LinksChangeEvent::getUpdateTargets)
      .contains(
        tuple(id1, TypeEnum.DELETE, List.of(changeTarget(instanceId1, link1))),
        tuple(id1, TypeEnum.DELETE, List.of(changeTarget(instanceId2, link2))),
        tuple(id2, TypeEnum.DELETE, List.of(changeTarget(instanceId3, link3)))
      );
    verify(authorityService).deleteByIds(anyCollection());
  }

  @Test
  void handle_positive_shouldDeleteLinksOnlyWithoutDeletingAuthoritiesOnHeadingTypeChangeUpdateEvent() {
    var id = UUID.randomUUID();
    var authorityDomainEvent = new AuthorityDomainEvent(
      id,
      new AuthorityDto().naturalId("n12345").corporateName("Beatles"),
      new AuthorityDto().naturalId("n12345").corporateNameTitle("Beatles mono"),
      DomainEventType.UPDATE,
      TENANT_ID);
    var changes = Map.of(
      CORPORATE_NAME, new AuthorityChange(CORPORATE_NAME, null, "Beatles"),
      CORPORATE_NAME_TITLE, new AuthorityChange(CORPORATE_NAME, "Beatles mono", null)
    );
    var authorityEvents = List.of(new AuthorityChangeHolder(authorityDomainEvent, changes, emptyMap(), 1));
    var link = TestDataUtils.Link.of(1, 1);
    var instanceId = UUID.randomUUID();
    doNothing().when(linkingService).deleteByAuthorityIdIn(Set.of(id));
    when(properties.getNumPartitions()).thenReturn(1);
    when(linkingService.getLinksByAuthorityId(eq(id), any())).thenReturn(
      new PageImpl<>(List.of(link.toEntity(instanceId)), Pageable.ofSize(1), 1));

    var actual = handler.handle(authorityEvents);

    verify(linkingService).getLinksByAuthorityId(eq(id), any());
    verify(linkingService).deleteByAuthorityIdIn(Set.of(id));
    assertThat(actual)
      .hasSize(1)
      .extracting(LinksChangeEvent::getAuthorityId, LinksChangeEvent::getType, LinksChangeEvent::getUpdateTargets)
      .contains(tuple(id, TypeEnum.DELETE, List.of(changeTarget(instanceId, link))));
    verifyNoInteractions(authorityService);
  }

  @Test
  void handle_positive_emptyEventList() {
    var actual = handler.handle(emptyList());

    assertThat(actual).isEmpty();
  }

  @Test
  void handle_positive_nullEventList() {
    var actual = handler.handle(null);

    assertThat(actual).isEmpty();
  }

  private AuthorityDomainEvent prepareAuthorityDeleteEvent(UUID id) {
    var authorityDto = new AuthorityDto().naturalId("n12345").personalName("name");
    return new AuthorityDomainEvent(id, authorityDto, authorityDto, DomainEventType.DELETE, TENANT_ID);
  }

  private ChangeTarget changeTarget(UUID instanceId, TestDataUtils.Link link) {
    return new ChangeTarget().field(link.tag()).links(
      Collections.singletonList(new ChangeTargetLink().instanceId(instanceId)));
  }
}
