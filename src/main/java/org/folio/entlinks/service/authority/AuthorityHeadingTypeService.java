package org.folio.entlinks.service.authority;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.entity.AuthorityHeadingType;
import org.folio.entlinks.domain.repository.AuthorityHeadingTypeRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Log4j2
public class AuthorityHeadingTypeService {

  private final AuthorityHeadingTypeRepository repository;

  public Page<AuthorityHeadingType> getAll(Integer offset, Integer limit, String cql) {
    log.debug("getAll:: Attempts to find all AuthorityHeadingType by [offset: {}, limit: {}, cql: {}]",
      offset, limit, cql);

    if (StringUtils.isBlank(cql)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(cql, new OffsetRequest(offset, limit));
  }
}
