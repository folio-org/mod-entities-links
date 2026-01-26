package org.folio.entlinks.service.consortium.propagation.model;

import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.entity.AuthorityDataStat;

public record AuthorityDataStatsPropagationData(List<AuthorityDataStat> authorityDataStats, UUID jobId,
                                                List<LinkUpdateReport> reports, UUID authorityId) {
  public static AuthorityDataStatsPropagationData forCreate(List<AuthorityDataStat> authorityDataStats) {
    return new AuthorityDataStatsPropagationData(authorityDataStats, null, null, null);
  }

  public static AuthorityDataStatsPropagationData forUpdate(UUID jobId, List<LinkUpdateReport> reports) {
    return new AuthorityDataStatsPropagationData(null, jobId, reports, null);
  }

  public static AuthorityDataStatsPropagationData forDelete(UUID authorityId) {
    return new AuthorityDataStatsPropagationData(null, null, null, authorityId);
  }
}
