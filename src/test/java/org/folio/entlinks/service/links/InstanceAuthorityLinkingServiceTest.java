package org.folio.entlinks.service.links;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus.ACTUAL;
import static org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus.ERROR;
import static org.folio.support.TestDataUtils.links;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.TestDataUtils.report;
import static org.folio.support.TestDataUtils.reports;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.entity.projection.InstanceLinkView;
import org.folio.entlinks.domain.entity.projection.LinkCountView;
import org.folio.entlinks.domain.entity.projection.LinkCountViewImpl;
import org.folio.entlinks.domain.repository.InstanceLinkRepository;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.spring.testing.type.UnitTest;
import org.folio.support.TestDataUtils;
import org.folio.support.TestDataUtils.Link;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityLinkingServiceTest {

  private static final String REPORT_ERROR = "error";

  @Mock private InstanceLinkRepository instanceLinkRepository;
  @Mock private AuthorityService authorityService;

  @InjectMocks
  private InstanceAuthorityLinkingService service;

  @Test
  void getLinksByInstanceId_positive_foundWhenExist() {
    var instanceId = randomUUID();
    var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    );
    var linkViews = existedLinks.stream()
      .map(link -> instanceLinkView(link, link.getAuthorityNaturalId()))
      .toList();

    when(instanceLinkRepository.findByInstanceId(any(UUID.class))).thenReturn(linkViews);

    var result = service.getLinksByInstanceId(instanceId);

    assertThat(result)
      .hasSize(existedLinks.size())
      .extracting(link -> link.getLinkingRule().getId())
      .containsOnly(Link.RULE_IDS);
  }

  @Test
  void getLinksByInstanceId_positive_nothingFound() {
    var instanceId = randomUUID();
    when(instanceLinkRepository.findByInstanceId(any(UUID.class))).thenReturn(emptyList());

    var result = service.getLinksByInstanceId(instanceId);

    assertThat(result).isEmpty();
  }

  @Test
  void getLinksByAuthorityId_positive_foundWhenExist() {
    var instanceId = randomUUID();
    var links = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1)
    );

    var linkPage = new PageImpl<>(links);

    when(instanceLinkRepository.findByAuthorityId(any(UUID.class), any(Pageable.class))).thenReturn(linkPage);

    var result = service.getLinksByAuthorityId(UUID.randomUUID(), Pageable.ofSize(2));

    assertThat(result)
      .hasSize(links.size())
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1]);
  }

  @Test
  void getLinksByIds_positive_foundWhenExist() {
    var links = links(2);
    var ids = links.stream()
      .map(link -> link.getId().intValue())
      .toList();
    var longIds = links.stream()
      .map(InstanceAuthorityLink::getId)
      .toList();

    when(instanceLinkRepository.findAllById(longIds)).thenReturn(links);

    var result = service.getLinksByIds(ids);

    assertThat(result)
      .containsOnly(links.get(0), links.get(1));
  }

  @Test
  void getLinksByIds_positive_filterNullIds() {
    var links = links(1);
    var ids = asList(links.get(0).getId().intValue(), null, null);
    var longIds = List.of(links.get(0).getId());

    when(instanceLinkRepository.findAllById(longIds)).thenReturn(links);

    var result = service.getLinksByIds(ids);

    assertThat(result).hasSize(1);
    verify(instanceLinkRepository).findAllById(longIds);
  }

  @Test
  void updateLinks_negative_throwsException_whenAuthorityNotFound() {
    var instanceId = randomUUID();
    var authorityId = randomUUID();
    var incomingLinks = links(instanceId, Link.of(0, 0));

    var authoritiesExist = Map.of(authorityId, false);
    when(authorityService.authoritiesExist(anySet())).thenReturn(authoritiesExist);
    when(authorityService.authoritiesExistForCentralIfOnMember(anySet())).thenReturn(emptyMap());

    assertThatThrownBy(() -> service.updateLinks(instanceId, incomingLinks))
      .isInstanceOf(AuthorityNotFoundException.class);

    verify(instanceLinkRepository, never()).saveAll(anyList());
    verify(instanceLinkRepository, never()).deleteAllInBatch(anyList());
  }

  @Test
  void updateLinks_positive_withSharedAuthorities() {
    var instanceId = randomUUID();
    var incomingLinks = links(instanceId, Link.of(0, 0), Link.of(1, 1));
    final var authorityIds = incomingLinks.stream()
      .map(InstanceAuthorityLink::getAuthorityId)
      .collect(Collectors.toSet());

    var localAuthorities = new HashMap<UUID, Boolean>();
    localAuthorities.put(incomingLinks.get(0).getAuthorityId(), true);
    localAuthorities.put(incomingLinks.get(1).getAuthorityId(), false);

    var sharedAuthorities = Map.of(
      incomingLinks.get(0).getAuthorityId(), false,
      incomingLinks.get(1).getAuthorityId(), true
    );

    when(instanceLinkRepository.findByInstanceId(any(UUID.class))).thenReturn(emptyList());
    when(authorityService.authoritiesExist(anySet())).thenReturn(localAuthorities);
    when(authorityService.authoritiesExistForCentralIfOnMember(anySet())).thenReturn(sharedAuthorities);
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());

    service.updateLinks(instanceId, incomingLinks);

    verify(authorityService).authoritiesExist(authorityIds);
    verify(authorityService).authoritiesExistForCentralIfOnMember(authorityIds);
    verify(instanceLinkRepository).saveAll(anyList());
  }

  @Test
  void updateLinks_positive_saveIncomingLinks_whenAnyExist() {
    final var instanceId = randomUUID();
    final var incomingLinks = links(instanceId, Link.of(0, 0), Link.of(1, 1));

    when(instanceLinkRepository.findByInstanceId(any(UUID.class))).thenReturn(emptyList());
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());
    mockAuthorities(incomingLinks);

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(2)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1]);

    assertThat(deleteCaptor.getValue()).isEmpty();
  }

  @Test
  void updateLinks_positive_deleteAllLinks_whenIncomingIsEmpty() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId, Link.of(0, 0), Link.of(1, 1));
    final var incomingLinks = Collections.<InstanceAuthorityLink>emptyList();
    var linkViews = existedLinks.stream()
      .map(link -> instanceLinkView(link, link.getAuthorityNaturalId()))
      .toList();

    when(instanceLinkRepository.findByInstanceId(any(UUID.class))).thenReturn(linkViews);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).isEmpty();

    assertThat(deleteCaptor.getValue()).hasSize(2)
      .extracting(link -> link.getLinkingRule().getId())
      .containsOnly(Link.RULE_IDS[0], Link.RULE_IDS[1]);
  }

  @Test
  void updateLinks_positive_deleteAllExistedAndSaveAllIncomingLinks() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    );
    final var incomingLinks = links(instanceId,
      Link.of(0, 1),
      Link.of(1, 0),
      Link.of(2, 3),
      Link.of(3, 2)
    );

    var linkViews = existedLinks.stream()
      .map(link -> instanceLinkView(link, link.getAuthorityNaturalId()))
      .toList();

    when(instanceLinkRepository.findByInstanceId(instanceId)).thenReturn(linkViews);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());
    mockAuthorities(incomingLinks);

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getBibField())
      .containsOnly(Link.TAGS[0], Link.TAGS[1], Link.TAGS[2], Link.TAGS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getId())
      .containsOnly(Link.RULE_IDS);
  }

  @Test
  void updateLinks_positive_saveOnlyNewLinks() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1)
    );
    final var incomingLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    );
    var linkViews = existedLinks.stream()
      .map(link -> instanceLinkView(link, link.getAuthorityNaturalId()))
      .toList();

    when(instanceLinkRepository.findByInstanceId(instanceId)).thenReturn(linkViews);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());
    mockAuthorities(incomingLinks);

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getId())
      .containsOnly(Link.RULE_IDS[0], Link.RULE_IDS[1], Link.RULE_IDS[2], Link.RULE_IDS[3]);

    assertThat(deleteCaptor.getValue()).isEmpty();
  }

  @Test
  void updateLinks_positive_deleteAndSaveLinks_whenHaveDifference() {
    final var instanceId = randomUUID();
    final var existedLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 2),
      Link.of(3, 3)
    );
    final var incomingLinks = links(instanceId,
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    );

    var linkViews = existedLinks.stream()
      .map(link -> instanceLinkView(link, link.getAuthorityNaturalId()))
      .toList();

    when(instanceLinkRepository.findByInstanceId(instanceId)).thenReturn(linkViews);
    doNothing().when(instanceLinkRepository).deleteAllInBatch(any());
    when(instanceLinkRepository.saveAll(any())).thenReturn(emptyList());
    mockAuthorities(incomingLinks);

    service.updateLinks(instanceId, incomingLinks);

    var saveCaptor = linksCaptor();
    var deleteCaptor = linksCaptor();
    verify(instanceLinkRepository).saveAll(saveCaptor.capture());
    verify(instanceLinkRepository).deleteAllInBatch(deleteCaptor.capture());

    assertThat(saveCaptor.getValue()).hasSize(4)
      .extracting(link -> link.getLinkingRule().getId())
      .containsOnly(Link.RULE_IDS[0], Link.RULE_IDS[1], Link.RULE_IDS[2], Link.RULE_IDS[3]);

    assertThat(deleteCaptor.getValue()).hasSize(2)
      .extracting(link -> link.getLinkingRule().getId())
      .containsOnly(Link.RULE_IDS[2], Link.RULE_IDS[3]);
  }

  @Test
  void countLinksByAuthorityIds_positive() {
    var authorityId1 = randomUUID();
    var authorityId2 = randomUUID();
    var authorityId3 = randomUUID();
    var resultSet = List.<LinkCountView>of(
      linkCountView(authorityId1, 10),
      linkCountView(authorityId2, 15)
    );

    when(instanceLinkRepository.countLinksByAuthorityIds(anySet())).thenReturn(resultSet);

    var authorityIds = Set.of(authorityId1, authorityId2, authorityId3);
    var result = service.countLinksByAuthorityIds(authorityIds);

    assertThat(result)
      .hasSize(2)
      .contains(entry(authorityId1, 10), entry(authorityId2, 15));
  }

  @Test
  void deleteByAuthorityIdIn_positive() {
    var authorityId = randomUUID();
    var authorityIds = Set.of(authorityId);

    service.deleteByAuthorityIdIn(authorityIds);

    verify(instanceLinkRepository).deleteByAuthorityIds(authorityIds);
  }

  @Test
  void saveAll_positive() {
    var instanceId = UUID.randomUUID();
    var links = links(2);

    service.saveAll(instanceId, links);

    verify(instanceLinkRepository).saveAll(links);
  }

  @Test
  @SuppressWarnings("unchecked")
  void getLinks_positive() {
    var status = LinkStatus.ACTUAL;
    var fromDate = OffsetDateTime.now();
    var toDate = fromDate.plusDays(1);
    var limit = 1;
    var pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("updatedAt")));
    var authorityNaturalId = "n12345";
    var link = InstanceAuthorityLink.builder()
      .id(1L)
      .build();
    var linkView = instanceLinkView(link, authorityNaturalId);

    when(instanceLinkRepository.findLinksWithAuthorityNaturalId(
      eq(InstanceAuthorityLinkStatus.ACTUAL),
      any(java.sql.Timestamp.class),
      any(java.sql.Timestamp.class),
      eq(pageable)))
      .thenReturn(new PageImpl<>(List.of(linkView), pageable, 1));

    var links = service.getLinks(status, fromDate, toDate, limit);

    assertThat(links)
      .hasSize(1)
      .first()
      .satisfies(l -> {
        assertThat(l.getId()).isEqualTo(1L);
        assertThat(l.getAuthorityNaturalId()).isEqualTo(authorityNaturalId);
      });
  }

  @Test
  void getLinks_positive_withNullParameters() {
    var limit = 10;
    var pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("updatedAt")));
    var link = InstanceAuthorityLink.builder()
      .id(2L)
      .build();
    var linkView = instanceLinkView(link, "n67890");

    when(instanceLinkRepository.findLinksWithAuthorityNaturalId(
      eq(null),
      eq(null),
      eq(null),
      eq(pageable)))
      .thenReturn(new PageImpl<>(List.of(linkView), pageable, 1));

    var links = service.getLinks(null, null, null, limit);

    assertThat(links).hasSize(1);
    verify(instanceLinkRepository).findLinksWithAuthorityNaturalId(null, null, null, pageable);
  }

  @Test
  void setNaturalIdForSharedAuthority_positive_setsNaturalIdsAndSkipsExisting() {
    var authorityId1 = randomUUID();
    var authorityId2 = randomUUID();
    var authorityId3 = randomUUID();

    // Link with null natural ID - should be set
    var link1 = InstanceAuthorityLink.builder()
      .authorityId(authorityId1)
      .authorityNaturalId(null)
      .build();

    // Link with existing natural ID - should be skipped
    var link2 = InstanceAuthorityLink.builder()
      .authorityId(authorityId2)
      .authorityNaturalId("existingNaturalId")
      .build();

    // Link with null natural ID but partial match - should remain null
    var link3 = InstanceAuthorityLink.builder()
      .authorityId(authorityId3)
      .authorityNaturalId(null)
      .build();

    var links = List.of(link1, link2, link3);

    // Only authorityId1 has a natural ID in the response (partial match scenario)
    var naturalIdsMap = Map.of(authorityId1, "naturalId1");

    when(authorityService.findNaturalIdsByIdInAndDeletedFalseForCentralIfOnMember(anyList()))
      .thenReturn(naturalIdsMap);

    service.setNaturalIdForSharedAuthority(links);

    assertThat(link1.getAuthorityNaturalId()).isEqualTo("naturalId1");
    assertThat(link2.getAuthorityNaturalId()).isEqualTo("existingNaturalId");
    assertThat(link3.getAuthorityNaturalId()).isNull();
    verify(authorityService).findNaturalIdsByIdInAndDeletedFalseForCentralIfOnMember(
      List.of(authorityId1, authorityId3));
  }

  @Test
  void setNaturalIdForSharedAuthority_positive_handlesEmptyList() {
    List<InstanceAuthorityLink> links = emptyList();

    service.setNaturalIdForSharedAuthority(links);

    verifyNoInteractions(authorityService);
  }

  @Test
  void setNaturalIdForSharedAuthority_positive_handlesEmptyNaturalIdsMap() {
    var authorityId = randomUUID();
    var link = InstanceAuthorityLink.builder()
      .authorityId(authorityId)
      .authorityNaturalId(null)
      .build();
    var links = List.of(link);

    when(authorityService.findNaturalIdsByIdInAndDeletedFalseForCentralIfOnMember(anyList()))
      .thenReturn(emptyMap());

    service.setNaturalIdForSharedAuthority(links);

    assertThat(link.getAuthorityNaturalId()).isNull();
    verify(authorityService).findNaturalIdsByIdInAndDeletedFalseForCentralIfOnMember(List.of(authorityId));
  }

  @Test
  void updateForReports_positive_updateLinks_forSuccess() {
    var jobId = UUID.randomUUID();
    var reports = reports(jobId);

    when(instanceLinkRepository.findAllById(anyList())).thenReturn(TestDataUtils.links(2, REPORT_ERROR));

    service.updateForReports(jobId, reports);

    var linksCaptor = linksCaptor();
    verify(instanceLinkRepository, times(2)).saveAll(linksCaptor.capture());
    var links = linksCaptor.getAllValues().stream().flatMap(List::stream).toList();
    assertThat(links)
      .hasSize(4)
      .allSatisfy(linkAsserter(ACTUAL, null));
  }

  @Test
  void updateForReports_positive_updateLinks_forFail_shouldTrimFailCause() {
    var jobId = UUID.randomUUID();
    var reports = reports(jobId, LinkUpdateReport.StatusEnum.FAIL, "  " + REPORT_ERROR + "  ");

    when(instanceLinkRepository.findAllById(anyList())).thenReturn(TestDataUtils.links(2));

    service.updateForReports(jobId, reports);

    var linksCaptor = linksCaptor();
    verify(instanceLinkRepository, times(2)).saveAll(linksCaptor.capture());
    var links = linksCaptor.getAllValues().stream().flatMap(List::stream).toList();
    assertThat(links)
      .hasSize(4)
      .allSatisfy(linkAsserter(ERROR, REPORT_ERROR));
  }

  @Test
  void updateForReports_positive_skipsReportWithEmptyOrNullLinkIds() {
    var jobId = UUID.randomUUID();
    var reportWithEmptyLinkIds = report("tenant1", jobId, LinkUpdateReport.StatusEnum.SUCCESS, null, emptyList());
    var reportWithNullLinkIds = report("tenant2", jobId, LinkUpdateReport.StatusEnum.SUCCESS, null, null);
    var reports = List.of(reportWithEmptyLinkIds, reportWithNullLinkIds);

    service.updateForReports(jobId, reports);

    verify(instanceLinkRepository, never()).findAllById(anyList());
    verify(instanceLinkRepository, never()).saveAll(anyList());
  }

  private ArgumentCaptor<List<InstanceAuthorityLink>> linksCaptor() {
    @SuppressWarnings("unchecked") var listClass = (Class<List<InstanceAuthorityLink>>) (Class<?>) List.class;
    return ArgumentCaptor.forClass(listClass);
  }

  private LinkCountViewImpl linkCountView(UUID id, int totalLinks) {
    var view = new LinkCountViewImpl();
    view.setId(id);
    view.setTotalLinks(totalLinks);
    return view;
  }

  private InstanceLinkView instanceLinkView(InstanceAuthorityLink link, String authorityNaturalId) {
    return new InstanceLinkView() {
      @Override
      public InstanceAuthorityLink getLink() {
        return link;
      }

      @Override
      public String getAuthorityNaturalId() {
        return authorityNaturalId;
      }
    };
  }

  private void mockAuthorities(List<InstanceAuthorityLink> links) {
    var authoritiesExistence = links.stream()
        .map(InstanceAuthorityLink::getAuthorityId)
        .collect(Collectors.toMap(id -> id, id -> true));
    when(authorityService.authoritiesExist(anySet())).thenReturn(authoritiesExistence);
  }

  private Consumer<InstanceAuthorityLink> linkAsserter(InstanceAuthorityLinkStatus status, String errorCause) {
    return link -> {
      assertThat(link.getStatus()).isEqualTo(status);
      assertThat(link.getErrorCause()).isEqualTo(errorCause);
    };
  }
}
