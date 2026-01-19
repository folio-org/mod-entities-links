package org.folio.entlinks.controller.delegate;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;
import static org.folio.support.TestDataUtils.links;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.TestDataUtils.stats;
import static org.folio.support.base.TestConstants.CONSORTIUM_SOURCE_PREFIX;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.HashMap;
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
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.integration.internal.InstanceStorageService;
import org.folio.entlinks.service.consortium.UserTenantsService;
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
  private @Mock FolioExecutionContext context;
  private @Mock UserTenantsService userTenantsService;

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

  /**
   * One link is outside the limit so next link time is filled and 1 link is being filtered out.
   * */
  @Test
  void getLinkedBibUpdateStats_positive() {
    var linksMock = links(4, "error");
    var expectedLinks = linksMock.subList(0, 3);
    var instanceIds = linksMock.stream()
      .map(InstanceAuthorityLink::getInstanceId)
      .map(UUID::toString)
      .toList();
    var instanceData = instanceIds.stream()
      .collect(Collectors.toMap(id -> id, id -> instanceData()));
    var nextLinkTime = fromTimestamp(linksMock.getLast().getUpdatedAt());

    testGetLinkedBibUpdateStats_positive(linksMock, expectedLinks, instanceIds, instanceData, nextLinkTime);
  }

  /**
   * 4 links are retrieved from the database while the limit is 3.
   * 4 links are used to get instance data.
   * 1 instance data is missing, 1 is instance shadow copy.
   * So the result contains only 2 links because limit is happening after filtering.
   * */
  @Test
  void getLinkedBibUpdateStats_positive_noNext_shadowFiltered() {
    var linksMock = links(4, "error");
    var instanceIds = linksMock.stream()
      .map(InstanceAuthorityLink::getInstanceId)
      .map(UUID::toString)
      .toList();
    var instanceData = new HashMap<String, Pair<String, String>>();
    instanceData.put(instanceIds.get(0), instanceData(false));
    instanceData.put(instanceIds.get(1), instanceData(true));
    instanceData.put(instanceIds.get(3), instanceData(false));
    var expectedLinks = List.of(linksMock.get(0), linksMock.get(3));

    testGetLinkedBibUpdateStats_positive(linksMock, expectedLinks, instanceIds, instanceData, null);
  }

  @Test
  void getLinkedBibUpdateStats_positive_noNext() {
    var linksMock = links(3, "error");
    var instanceIds = linksMock.stream()
      .map(InstanceAuthorityLink::getInstanceId)
      .map(UUID::toString)
      .toList();
    var instanceData = instanceIds.stream()
      .collect(Collectors.toMap(id -> id, id -> instanceData()));

    testGetLinkedBibUpdateStats_positive(linksMock, instanceIds, instanceData);
  }

  @Test
  void getLinkedBibUpdateStats_positive_sameInstance() {
    var linksMock = links(2, "error");
    var instanceId = linksMock.get(0).getInstanceId();
    linksMock.get(1).setInstanceId(instanceId);
    var instanceData = Map.of(instanceId.toString(), instanceData());

    testGetLinkedBibUpdateStats_positive(linksMock, singletonList(instanceId.toString()), instanceData);
  }

  @Test
  void getLinkedBibUpdateStats_positive_emptyInstanceData() {
    var linksMock = links(3, "error");
    var instanceIds = linksMock.stream()
      .map(InstanceAuthorityLink::getInstanceId)
      .map(UUID::toString)
      .toList();
    var instanceData = Map.<String, Pair<String, String>>of();

    testGetLinkedBibUpdateStats_positive(linksMock, List.of(), instanceIds, instanceData, null);
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

    doNothing().when(linkingService).updateLinks(INSTANCE_ID, links);
    when(mapper.convertDto(dtoCollection.getLinks())).thenReturn(links);

    delegate.updateLinks(INSTANCE_ID, dtoCollection);

    verify(linkingService).updateLinks(INSTANCE_ID, links);
  }

  @Test
  void updateLinks_positive_emptyLinks() {
    final var links = links(INSTANCE_ID);
    final var dtoCollection = linksDtoCollection(linksDto(INSTANCE_ID));

    doNothing().when(linkingService).updateLinks(INSTANCE_ID, links);

    delegate.updateLinks(INSTANCE_ID, dtoCollection);

    verify(linkingService).updateLinks(INSTANCE_ID, links);
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

  private void testGetLinkedBibUpdateStats_positive(List<InstanceAuthorityLink> linksMock,
                                                    List<String> instanceIds,
                                                    Map<String, Pair<String, String>> instanceData) {
    testGetLinkedBibUpdateStats_positive(linksMock, linksMock, instanceIds, instanceData, null);
  }

  private void testGetLinkedBibUpdateStats_positive(List<InstanceAuthorityLink> linksMock,
                                                    List<InstanceAuthorityLink> expectedLinks,
                                                    List<String> instanceIds,
                                                    Map<String, Pair<String, String>> instanceData,
                                                    OffsetDateTime next) {
    var status = LinkStatus.ACTUAL;
    var fromDate = OffsetDateTime.now();
    var toDate = fromDate.plusDays(1);
    var limit = 3;
    var statsDtos = stats(linksMock);

    when(linkingService.getLinks(status, fromDate, toDate, limit + 1))
      .thenReturn(linksMock);
    when(statsMapper.convertToDto(linksMock))
      .thenReturn(statsDtos);
    when(instanceService.getInstanceData(instanceIds))
      .thenReturn(instanceData);

    var expectedStatsDtos = stats(expectedLinks);
    expectedStatsDtos.forEach(bibStatsDto -> {
      var instanceId = bibStatsDto.getInstanceId();
      var title = Optional.ofNullable(instanceData.get(instanceId.toString()))
        .map(Pair::getLeft)
        .orElse(null);
      bibStatsDto.setInstanceTitle(title);
    });

    var actual = delegate.getLinkedBibUpdateStats(fromDate, toDate, status, limit);

    assertThat(actual)
      .isEqualTo(new BibStatsDtoCollection()
        .stats(expectedStatsDtos)
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
