package org.folio.entlinks.service.consortium.propagation;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.consortium.propagation.model.AuthorityDataStatsPropagationData;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

@Service
public class ConsortiumAuthorityDataStatsPropagationService
  extends ConsortiumPropagationService<AuthorityDataStatsPropagationData> {

  private final AuthorityDataStatService authorityDataStatService;
  private final InstanceAuthorityLinkingService linkingService;

  protected ConsortiumAuthorityDataStatsPropagationService(ConsortiumTenantsService tenantsService,
                                                           SystemUserScopedExecutionService executionService,
                                                           FolioExecutionContext folioExecutionContext,
                                                           AuthorityDataStatService authorityDataStatService,
                                                           InstanceAuthorityLinkingService linkingService) {
    super(tenantsService, executionService, folioExecutionContext);
    this.authorityDataStatService = authorityDataStatService;
    this.linkingService = linkingService;
  }

  @Override
  protected void doPropagation(AuthorityDataStatsPropagationData propagationData,
                               PropagationType propagationType) {
    switch (propagationType) {
      case CREATE -> this.create(propagationData.authorityDataStats());
      case UPDATE -> this.update(propagationData.jobId(), propagationData.reports());
      case DELETE -> authorityDataStatService.deleteByAuthorityId(propagationData.authorityId());
      default -> throw new IllegalStateException("Unexpected value: " + propagationType);
    }
  }

  /**
   * Recalculates lbTotal to include links from current member tenant and creates AuthorityDataStats.
   *
   * @param authorityDataStats created for central tenant with lbTotal representing only central tenant links count.
   * */
  private void create(List<AuthorityDataStat> authorityDataStats) {
    authorityDataStats = authorityDataStats.stream()
      .map(AuthorityDataStat::copy)
      .collect(Collectors.toList());

    var authorityIds = authorityDataStats.stream()
      .map(AuthorityDataStat::getAuthorityId)
      .collect(Collectors.toSet());
    var linksNumberByAuthorityId = linkingService.countLinksByAuthorityIds(authorityIds);

    authorityDataStats.forEach(dataStat -> {
      var linksCount = linksNumberByAuthorityId.getOrDefault(
        dataStat.getAuthorityId(), 0) + dataStat.getLbTotal();
      dataStat.setLbTotal(linksCount);
    });

    authorityDataStatService.createInBatch(authorityDataStats);
  }

  private void update(UUID jobId, List<LinkUpdateReport> reports) {
    authorityDataStatService.updateOnlyStatsForReports(jobId, reports);
  }
}
