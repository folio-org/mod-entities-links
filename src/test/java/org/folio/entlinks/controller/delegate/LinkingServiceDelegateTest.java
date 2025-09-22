package org.folio.entlinks.controller.delegate;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.assertj.core.api.Assertions.tuple;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;
import static org.folio.support.TestDataUtils.links;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.TestDataUtils.stats;
import static org.folio.support.base.TestConstants.CONSORTIUM_SOURCE_PREFIX;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.entlinks.controller.converter.DataStatsMapper;
import org.folio.entlinks.controller.converter.InstanceAuthorityLinkMapper;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.domain.dto.LinksCountDto;
import org.folio.entlinks.domain.dto.UuidCollection;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.integration.internal.InstanceStorageService;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityPropagationService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumLinksPropagationService;
import org.folio.entlinks.service.consortium.propagation.model.LinksPropagationData;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.folio.support.TestDataUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinkingServiceDelegateTest {

  private static final UUID INSTANCE_ID = randomUUID();

  private @Mock InstanceAuthorityLinkingService linkingService;
  private @Mock InstanceAuthorityLinkMapper mapper;
  private @Mock InstanceStorageService instanceService;
  private @Mock DataStatsMapper statsMapper;
  private @Mock ConsortiumLinksPropagationService propagationService;
  private @Mock FolioExecutionContext context;
  private @Mock ConsortiumTenantsService tenantsService;

  private @InjectMocks LinkingServiceDelegate delegate;

  @BeforeEach
  void setUp() {
    lenient().when(context.getTenantId()).thenReturn(TENANT_ID);
  }

  @Test
  void getLinks_positive() {
    var linkData = TestDataUtils.Link.of(0, 0);
    var link = linkData.toEntity(INSTANCE_ID);
    var links = List.of(link);
    var linkDto = linkData.toDto(INSTANCE_ID);

    when(linkingService.getLinksByInstanceId(INSTANCE_ID)).thenReturn(links);
    when(mapper.convertToDto(links)).thenReturn(
      new InstanceLinkDtoCollection().links(List.of(linkDto)).totalRecords(1));

    var actual = delegate.getLinks(INSTANCE_ID);

    assertThat(actual).isNotNull()
      .extracting(InstanceLinkDtoCollection::getTotalRecords)
      .isEqualTo(1);

    assertThat(actual.getLinks())
      .hasSize(1)
      .containsExactlyInAnyOrder(linkDto);
  }

  @Test
  void getLinkedBibUpdateStats_positive() {
    var linksMock = links(3, "error");
    var linksForStats = linksMock.subList(0, 2);
    var instanceIds = linksForStats.stream()
      .map(InstanceAuthorityLink::getInstanceId)
      .map(UUID::toString)
      .toList();
    var instanceData = new HashMap<String, Pair<String, String>>();
    instanceData.put(instanceIds.get(0), instanceData(true));
    instanceData.put(instanceIds.get(1), instanceData(false));
    var nextLinkTime = fromTimestamp(linksMock.getLast().getUpdatedAt());

    testGetLinkedBibUpdateStats_positive(linksMock, linksForStats, instanceIds, instanceData, nextLinkTime);
  }

  @Test
  void getLinkedBibUpdateStats_positive_centralTenant() {
    var linksMock = links(3, "error");
    var linksForStats = linksMock.subList(0, 2);
    var instanceIds = linksForStats.stream()
      .map(InstanceAuthorityLink::getInstanceId)
      .map(UUID::toString)
      .toList();
    var instanceData = instanceIds.stream()
      .collect(Collectors.toMap(id -> id, id -> instanceData()));
    var nextLinkTime = fromTimestamp(linksMock.getLast().getUpdatedAt());

    when(tenantsService.isCentralTenantContext()).thenReturn(true);
    testGetLinkedBibUpdateStats_positive(linksMock, linksForStats, instanceIds, instanceData, nextLinkTime);
  }

  @Test
  void getLinkedBibUpdateStats_positive_noNext() {
    var linksMock = links(2, "error");
    var instanceIds = linksMock.stream()
      .map(InstanceAuthorityLink::getInstanceId)
      .map(UUID::toString)
      .toList();
    var instanceData = instanceIds.stream()
      .collect(Collectors.toMap(id -> id, id -> instanceData()));

    testGetLinkedBibUpdateStats_positive(linksMock, linksMock, instanceIds, instanceData, null);
  }

  @Test
  void getLinkedBibUpdateStats_positive_sameInstance() {
    var linksMock = links(2, "error");
    var instanceId = linksMock.get(0).getInstanceId();
    linksMock.get(1).setInstanceId(instanceId);
    var instanceData = Map.of(instanceId.toString(), instanceData());

    testGetLinkedBibUpdateStats_positive(linksMock, linksMock,
      singletonList(instanceId.toString()), instanceData, null);
  }

  @Test
  void getLinkedBibUpdateStats_positive_noInstanceData() {
    var linksMock = links(2, "error");
    var instanceIds = linksMock.stream()
      .map(InstanceAuthorityLink::getInstanceId)
      .map(UUID::toString)
      .toList();
    var instanceData = Map.of(instanceIds.getFirst(), instanceData());

    testGetLinkedBibUpdateStats_positive(linksMock, linksMock, instanceIds, instanceData, null);
  }

  @Test
  void getLinkedBibUpdateStats_negative_invalidDates() {
    var status = LinkStatus.ACTUAL;
    var fromDate = OffsetDateTime.now();
    var toDate = fromDate.minusDays(1);
    var limit = 2;

    var exception = Assertions.assertThrows(RequestBodyValidationException.class,
      () -> delegate.getLinkedBibUpdateStats(fromDate, toDate, status, limit));

    assertThat(exception)
      .hasMessage("'to' date should be not less than 'from' date.")
      .extracting(RequestBodyValidationException::getInvalidParameters)
      .returns(2, from(List::size));
  }

  @Test
  void updateLinks_positive() {
    final var links = links(INSTANCE_ID,
      TestDataUtils.Link.of(0, 0),
      TestDataUtils.Link.of(1, 1),
      TestDataUtils.Link.of(2, 2),
      TestDataUtils.Link.of(3, 3)
    );
    final var dtoCollection = linksDtoCollection(linksDto(INSTANCE_ID,
      TestDataUtils.Link.of(0, 0),
      TestDataUtils.Link.of(1, 1),
      TestDataUtils.Link.of(2, 3),
      TestDataUtils.Link.of(3, 2)
    ));
    final var propagationData = new LinksPropagationData(INSTANCE_ID, links);

    doNothing().when(linkingService).updateLinks(INSTANCE_ID, links);
    when(mapper.convertDto(dtoCollection.getLinks())).thenReturn(links);

    delegate.updateLinks(INSTANCE_ID, dtoCollection);

    verify(linkingService).updateLinks(INSTANCE_ID, links);
    verify(propagationService).propagate(propagationData, ConsortiumAuthorityPropagationService.PropagationType.UPDATE,
        TENANT_ID);
  }

  @Test
  void updateLinks_positive_emptyLinks() {
    final var links = links(INSTANCE_ID);
    final var dtoCollection = linksDtoCollection(linksDto(INSTANCE_ID));
    final var propagationData = new LinksPropagationData(INSTANCE_ID, links);

    doNothing().when(linkingService).updateLinks(INSTANCE_ID, links);

    delegate.updateLinks(INSTANCE_ID, dtoCollection);

    verify(linkingService).updateLinks(INSTANCE_ID, links);
    verify(propagationService).propagate(propagationData, ConsortiumAuthorityPropagationService.PropagationType.UPDATE,
        TENANT_ID);
  }

  @Test
  void updateLinks_negative_whenInstanceIdIsNotSameForIncomingLinks() {
    var incomingLinks = linksDtoCollection(linksDto(randomUUID(),
      TestDataUtils.Link.of(0, 0),
      TestDataUtils.Link.of(1, 1),
      TestDataUtils.Link.of(2, 3),
      TestDataUtils.Link.of(3, 2)
    ));

    var exception = Assertions.assertThrows(RequestBodyValidationException.class,
      () -> delegate.updateLinks(INSTANCE_ID, incomingLinks));

    assertThat(exception)
      .hasMessage("Link should have instanceId = " + INSTANCE_ID)
      .extracting(RequestBodyValidationException::getInvalidParameters)
      .returns(4, from(List::size));
  }

  @Test
  void countLinksByAuthorityIds_positive() {
    var ids = List.of(randomUUID(), randomUUID(), randomUUID());

    when(linkingService.countLinksByAuthorityIds(new HashSet<>(ids))).thenReturn(
      Map.of(ids.get(0), 2, ids.get(1), 1));
    when(mapper.convert(anyMap())).thenCallRealMethod();

    var actual = delegate.countLinksByAuthorityIds(new UuidCollection().ids(ids));

    assertThat(actual.getLinks())
      .hasSize(ids.size())
      .extracting(LinksCountDto::getId, LinksCountDto::getTotalLinks)
      .containsExactlyInAnyOrder(tuple(ids.get(0), 2), tuple(ids.get(1), 1), tuple(ids.get(2), 0));
  }

  private void testGetLinkedBibUpdateStats_positive(List<InstanceAuthorityLink> linksMock,
                                                    List<InstanceAuthorityLink> linksForStats,
                                                    List<String> instanceIds,
                                                    Map<String, Pair<String, String>> instanceData,
                                                    OffsetDateTime next) {
    var status = LinkStatus.ACTUAL;
    var fromDate = OffsetDateTime.now();
    var toDate = fromDate.plusDays(1);
    var limit = 2;
    var expectedStats = stats(linksForStats);

    when(linkingService.getLinks(status, fromDate, toDate, limit + 1))
      .thenReturn(linksMock);
    when(statsMapper.convertToDto(linksForStats))
      .thenReturn(expectedStats);
    when(instanceService.getInstanceData(instanceIds))
      .thenReturn(instanceData);

    expectedStats.forEach(bibStatsDto -> {
      var instanceId = bibStatsDto.getInstanceId();
      var title = Optional.ofNullable(instanceData.get(instanceId.toString()))
        .map(Pair::getLeft)
        .orElse(null);
      bibStatsDto.setInstanceTitle(title);
      var source = Optional.ofNullable(instanceData.get(instanceId.toString()))
        .map(Pair::getRight)
        .orElse("");
      bibStatsDto.setShared(source.startsWith(CONSORTIUM_SOURCE_PREFIX));
    });

    var actual = delegate.getLinkedBibUpdateStats(fromDate, toDate, status, limit);

    assertThat(actual)
      .isEqualTo(new BibStatsDtoCollection()
        .stats(expectedStats)
        .next(next));
  }

  private static Pair<String, String> instanceData() {
    return instanceData(false);
  }

  private static Pair<String, String> instanceData(boolean shared) {
    var source = RandomStringUtils.insecure().nextAlphanumeric(5);
    if (shared) {
      source = CONSORTIUM_SOURCE_PREFIX + source;
    }
    return Pair.of(RandomStringUtils.insecure().nextAlphanumeric(5), source);
  }
}
