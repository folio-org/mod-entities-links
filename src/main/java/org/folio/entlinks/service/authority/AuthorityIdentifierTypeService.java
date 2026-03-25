package org.folio.entlinks.service.authority;

import static org.folio.entlinks.utils.ServiceUtils.initId;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.entity.AuthorityIdentifierType;
import org.folio.entlinks.domain.repository.AuthorityIdentifierTypeRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Log4j2
public class AuthorityIdentifierTypeService {

  private final AuthorityIdentifierTypeRepository repository;

  public Page<AuthorityIdentifierType> getAll(Integer offset, Integer limit, String cql) {
    log.debug("getAll:: Attempts to find all AuthorityIdentifierType by [offset: {}, limit: {}, cql: {}]",
      offset, limit, cql);

    if (StringUtils.isBlank(cql)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(cql, new OffsetRequest(offset, limit));
  }

  public AuthorityIdentifierType findById(UUID id) {
    log.debug("findById:: Querying Authority Identifier Type by ID [id: {}]", id);

    if (id == null) {
      return null;
    }

    return repository.findById(id).orElse(null);
  }

  public AuthorityIdentifierType findByName(String name) {
    log.debug("findByName:: Querying Authority Identifier Type by Name [name: {}]", name);

    if (StringUtils.isBlank(name)) {
      return null;
    }

    return repository.findByName(name).orElse(null);
  }

  @Transactional
  public AuthorityIdentifierType create(AuthorityIdentifierType entity) {
    log.debug("create:: Attempting to create AuthorityIdentifierType [entity: {}]", entity);

    initId(entity);

    return repository.save(entity);
  }
}
