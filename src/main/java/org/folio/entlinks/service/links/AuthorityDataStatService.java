package org.folio.entlinks.service.links;

import static org.folio.entlinks.utils.ServiceUtils.initId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.dto.LinkAction;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.domain.entity.AuthorityDataStatAction;
import org.folio.entlinks.domain.repository.AuthorityDataStatRepository;
import org.folio.entlinks.utils.DateUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorityDataStatService {

  private final AuthorityDataStatRepository statRepository;

  public List<AuthorityDataStat> createInBatch(List<AuthorityDataStat> stats) {
    for (var stat : stats) {
      initId(stat);
    }

    return statRepository.saveAll(stats);
  }

  public List<AuthorityDataStat> fetchDataStats(OffsetDateTime fromDate, OffsetDateTime toDate,
                                                LinkAction action, int limit) {
    var pageable = PageRequest.of(0, limit, Sort.by(Sort.Order.desc("startedAt")));
    return statRepository.findActualByActionAndDate(AuthorityDataStatAction.valueOf(action.getValue()),
      DateUtils.toTimestamp(fromDate), DateUtils.toTimestamp(toDate), pageable);
  }

  @Transactional
  public void deleteByAuthorityId(UUID authorityId) {
    log.info("deleteByAuthorityId:: [authorityId: {}]", authorityId);
    statRepository.deleteByAuthorityId(authorityId);
  }
}
