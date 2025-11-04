package org.folio.entlinks.integration.di.handler;

import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.folio.ActionProfile.FolioRecord.AUTHORITY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.DataImportEventPayload;
import org.folio.DataImportEventTypes;
import org.folio.MappingProfile;
import org.folio.entlinks.controller.delegate.AuthorityServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.di.AuthoritySourceMapper;
import org.folio.entlinks.utils.ConsortiumUtils;
import org.folio.processing.events.services.handler.EventHandler;
import org.folio.processing.exceptions.EventProcessingException;
import org.folio.rest.jaxrs.model.EntityType;
import org.folio.rest.jaxrs.model.ProfileSnapshotWrapper;
import org.folio.rest.jaxrs.model.ProfileType;
import org.springframework.stereotype.Component;

/**
 * Data import event handler for authority update events.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class AuthorityUpdateEventHandler implements EventHandler {

  private static final String SHADOW_COPY_UPDATE_RESTRICTED_MSG =
    "Shared MARC authority record cannot be updated from this tenant";

  private final ObjectMapper objectMapper;
  private final AuthorityServiceDelegate delegate;
  private final AuthoritySourceMapper sourceMapper;

  @Override
  public CompletableFuture<DataImportEventPayload> handle(DataImportEventPayload payload) {
    var updatedAuthority = sourceMapper.map(payload);
    var recordId = updatedAuthority.getId();
    var currentAuthority = delegate.getAuthorityById(recordId);
    if (ConsortiumUtils.isConsortiumShadowCopy(currentAuthority.getSource())) {
      return failedFuture(new EventProcessingException(SHADOW_COPY_UPDATE_RESTRICTED_MSG));
    }
    updatedAuthority.setVersion(currentAuthority.getVersion());
    delegate.updateAuthority(recordId, updatedAuthority);
    preparePayload(payload, updatedAuthority);
    return CompletableFuture.completedFuture(payload);
  }

  @Override
  public boolean isEligible(DataImportEventPayload payload) {
    var currentNode = payload.getCurrentNode();
    return ProfileType.MAPPING_PROFILE == currentNode.getContentType()
           && isEligibleMappingProfile(currentNode);
  }

  private void preparePayload(DataImportEventPayload payload, AuthorityDto createdAuthority) {
    try {
      payload.getContext().put(AUTHORITY.value(), objectMapper.writeValueAsString(createdAuthority));
      payload.setEventType(DataImportEventTypes.DI_INVENTORY_AUTHORITY_UPDATED.value());
      payload.getEventsChain().add(payload.getEventType());
    } catch (JsonProcessingException e) {
      throw new EventProcessingException("Failed to prepare payload for DI event", e);
    }
  }

  private boolean isEligibleMappingProfile(ProfileSnapshotWrapper profile) {
    var mappingProfile = JsonObject.mapFrom(profile.getContent()).mapTo(MappingProfile.class);
    return EntityType.MARC_AUTHORITY == mappingProfile.getExistingRecordType();
  }
}
