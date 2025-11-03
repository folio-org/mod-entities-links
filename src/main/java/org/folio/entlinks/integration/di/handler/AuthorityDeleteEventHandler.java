package org.folio.entlinks.integration.di.handler;

import io.vertx.core.json.JsonObject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.ActionProfile;
import org.folio.DataImportEventPayload;
import org.folio.DataImportEventTypes;
import org.folio.entlinks.controller.delegate.AuthorityServiceDelegate;
import org.folio.processing.events.services.handler.EventHandler;
import org.folio.rest.jaxrs.model.ProfileSnapshotWrapper;
import org.folio.rest.jaxrs.model.ProfileType;
import org.springframework.stereotype.Component;

/**
 * Data import event handler for authority create events.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class AuthorityDeleteEventHandler implements EventHandler {

  private static final String AUTHORITY_RECORD_ID = "AUTHORITY_RECORD_ID";

  private final AuthorityServiceDelegate delegate;

  @Override
  public CompletableFuture<DataImportEventPayload> handle(DataImportEventPayload payload) {
    var id = UUID.fromString(payload.getContext().get(AUTHORITY_RECORD_ID));
    delegate.deleteAuthorityById(id);
    preparePayload(payload);
    return CompletableFuture.completedFuture(payload);
  }

  @Override
  public boolean isEligible(DataImportEventPayload payload) {
    var currentNode = payload.getCurrentNode();
    return ProfileType.ACTION_PROFILE == currentNode.getContentType()
           && isEligibleActionProfile(currentNode);
  }

  private void preparePayload(DataImportEventPayload payload) {
    payload.setEventType(DataImportEventTypes.DI_INVENTORY_AUTHORITY_CREATED.value());
    payload.getEventsChain().add(payload.getEventType());
    payload.setCurrentNode(payload.getCurrentNode().getChildSnapshotWrappers().getFirst());
  }

  private boolean isEligibleActionProfile(ProfileSnapshotWrapper currentNode) {
    var actionProfile = JsonObject.mapFrom(currentNode.getContent()).mapTo(ActionProfile.class);
    return ActionProfile.Action.DELETE == actionProfile.getAction()
           && ActionProfile.FolioRecord.MARC_AUTHORITY == actionProfile.getFolioRecord();
  }
}
