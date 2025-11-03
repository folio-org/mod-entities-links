package org.folio.entlinks.integration.di.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.ActionProfile;
import org.folio.DataImportEventPayload;
import org.folio.DataImportEventTypes;
import org.folio.entlinks.controller.delegate.AuthorityServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.di.AuthoritySourceMapper;
import org.folio.processing.events.services.handler.EventHandler;
import org.folio.processing.exceptions.EventProcessingException;
import org.folio.rest.jaxrs.model.ProfileSnapshotWrapper;
import org.folio.rest.jaxrs.model.ProfileType;
import org.springframework.stereotype.Component;

/**
 * Data import event handler for authority create events.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class AuthorityCreateEventHandler implements EventHandler {

  private final ObjectMapper objectMapper;
  private final AuthorityServiceDelegate delegate;
  private final AuthoritySourceMapper sourceMapper;

  @Override
  public CompletableFuture<DataImportEventPayload> handle(DataImportEventPayload payload) {
    var authority = sourceMapper.map(payload);
    var createdAuthority = delegate.createAuthority(authority);
    preparePayload(payload, createdAuthority);
    return CompletableFuture.completedFuture(payload);
  }

  @Override
  public boolean isEligible(DataImportEventPayload payload) {
    var currentNode = payload.getCurrentNode();
    return ProfileType.ACTION_PROFILE == currentNode.getContentType()
           && isEligibleActionProfile(currentNode);
  }

  @Override
  public boolean isPostProcessingNeeded() {
    return true;
  }

  @Override
  public String getPostProcessingInitializationEventType() {
    return DataImportEventTypes.DI_INVENTORY_AUTHORITY_CREATED_READY_FOR_POST_PROCESSING.value();
  }

  private void preparePayload(DataImportEventPayload payload, AuthorityDto createdAuthority) {
    try {
      payload.getContext()
        .put(ActionProfile.FolioRecord.AUTHORITY.value(), objectMapper.writeValueAsString(createdAuthority));
      payload.setEventType(DataImportEventTypes.DI_INVENTORY_AUTHORITY_CREATED.value());
      payload.getEventsChain().add(payload.getEventType());
      payload.setCurrentNode(payload.getCurrentNode().getChildSnapshotWrappers().getFirst());
    } catch (JsonProcessingException e) {
      throw new EventProcessingException("Failed to prepare payload for DI event", e);
    }
  }

  private boolean isEligibleActionProfile(ProfileSnapshotWrapper currentNode) {
    var actionProfile = JsonObject.mapFrom(currentNode.getContent()).mapTo(ActionProfile.class);
    return ActionProfile.Action.CREATE == actionProfile.getAction()
           && ActionProfile.FolioRecord.AUTHORITY == actionProfile.getFolioRecord();
  }
}
