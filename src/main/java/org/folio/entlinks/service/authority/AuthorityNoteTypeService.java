package org.folio.entlinks.service.authority;

import static org.folio.entlinks.utils.ServiceUtils.initId;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.entity.AuthorityNoteType;
import org.folio.entlinks.domain.repository.AuthorityNoteTypeRepository;
import org.folio.entlinks.exception.AuthorityNoteTypeNotFoundException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.data.OffsetRequest;
import org.folio.tenant.domain.dto.Parameter;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Log4j2
public class AuthorityNoteTypeService {

  private final AuthorityNoteTypeRepository repository;

  public Page<AuthorityNoteType> getAll(Integer offset, Integer limit, String cql) {
    log.debug("getAll:: Attempts to find all AuthorityNoteType by [offset: {}, limit: {}, cql: {}]",
      offset, limit, cql);

    if (StringUtils.isBlank(cql)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(cql, new OffsetRequest(offset, limit));
  }

  /**
   * Retrieves {@link AuthorityNoteType} by the given id.
   *
   * @param id {@link UUID} of the authority note type being retrieved
   * @return retrieved {@link AuthorityNoteType}
   *
   *   Note: This method assumes the authority note type exists for the given ID and thus throws exception in case
   *   no authority note is found
   */
  public AuthorityNoteType getById(UUID id) {
    log.debug("getById:: Loading Authority Note Type by ID [id: {}]", id);

    return repository.findById(id).orElseThrow(() -> new AuthorityNoteTypeNotFoundException(id));
  }

  /**
   * Searches for the Authority Note Type for the given ID.
   *
   * @param id {@link UUID} ID of the authority note type being searched
   * @return found {@link AuthorityNoteType} instance or null if it is not found
   */
  public AuthorityNoteType findById(UUID id) {
    log.debug("getById:: Querying Authority Note Type by ID [id: {}]", id);

    if (id == null) {
      return null;
    }

    return repository.findById(id).orElse(null);
  }

  /**
   * Searches for the Authority Note Type for the given name.
   *
   * @param name Name of the authority note type being searched
   * @return found {@link AuthorityNoteType} instance or null if it is not found
   */
  public AuthorityNoteType findByName(String name) {
    log.debug("getById:: Querying Authority Note Type by Name [name: {}]", name);

    if (StringUtils.isBlank(name)) {
      return null;
    }

    return repository.findByName(name).orElse(null);
  }

  @Transactional
  public AuthorityNoteType create(AuthorityNoteType entity) {
    log.debug("create:: Attempting to create AuthorityNoteType [entity: {}]", entity);

    initId(entity);

    return repository.save(entity);
  }

  @Transactional
  public AuthorityNoteType update(@NonNull UUID id, AuthorityNoteType authorityNoteType) {
    log.debug("update:: Attempting to update AuthorityNoteType [id: {}]", id);

    if (!Objects.equals(id, authorityNoteType.getId())) {
      throw new RequestBodyValidationException("Request should have id = " + id,
        List.of(new Parameter("id").value(String.valueOf(authorityNoteType.getId()))));
    }

    var existingEntity = repository.findById(id)
      .orElseThrow(() -> new AuthorityNoteTypeNotFoundException(id));

    existingEntity.setName(authorityNoteType.getName());
    existingEntity.setSource(authorityNoteType.getSource());

    return repository.save(existingEntity);
  }

  public void deleteById(UUID id) {
    log.debug("deleteById:: Attempt to delete AuthorityNoteType by [id: {}]", id);

    if (!repository.existsById(id)) {
      throw new AuthorityNoteTypeNotFoundException(id);
    }

    repository.deleteById(id);
  }
}
