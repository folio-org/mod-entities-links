package org.folio.entlinks.integration.di.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.folio.ActionProfile;
import org.folio.DataImportEventPayload;
import org.folio.DataImportEventTypes;
import org.folio.entlinks.controller.delegate.AuthorityServiceDelegate;
import org.folio.rest.jaxrs.model.ProfileSnapshotWrapper;
import org.folio.rest.jaxrs.model.ProfileType;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityDeleteEventHandlerTest {

  private static final String AUTHORITY_RECORD_ID = "AUTHORITY_RECORD_ID";
  private static final UUID AUTHORITY_ID = UUID.randomUUID();

  @Mock
  private AuthorityServiceDelegate delegate;

  @InjectMocks
  private AuthorityDeleteEventHandler handler;

  private DataImportEventPayload payload;

  @BeforeEach
  void setUp() {
    var childWrapper = new ProfileSnapshotWrapper();
    childWrapper.setContentType(ProfileType.MAPPING_PROFILE);

    var actionProfile = new ActionProfile();
    actionProfile.setAction(ActionProfile.Action.DELETE);
    actionProfile.setFolioRecord(ActionProfile.FolioRecord.MARC_AUTHORITY);

    var parentWrapper = new ProfileSnapshotWrapper();
    parentWrapper.setContentType(ProfileType.ACTION_PROFILE);
    parentWrapper.setContent(actionProfile);
    parentWrapper.setChildSnapshotWrappers(List.of(childWrapper));

    var context = new HashMap<String, String>();
    context.put(AUTHORITY_RECORD_ID, AUTHORITY_ID.toString());

    payload = new DataImportEventPayload()
        .withContext(context)
        .withEventsChain(new ArrayList<>())
        .withCurrentNode(parentWrapper);
  }

  @Test
  void handle_positive() throws ExecutionException, InterruptedException {
    var future = handler.handle(payload);
    var result = future.get();

    verify(delegate).deleteAuthorityById(AUTHORITY_ID);
    assertEquals(DataImportEventTypes.DI_INVENTORY_AUTHORITY_CREATED.value(), result.getEventType());
    assertThat(result.getEventsChain()).contains(DataImportEventTypes.DI_INVENTORY_AUTHORITY_CREATED.value());
  }

  @Test
  void isEligible_positive() {
    assertTrue(handler.isEligible(payload));
  }

  @Test
  void isEligible_negative_notActionProfile() {
    payload.getCurrentNode().setContentType(ProfileType.MAPPING_PROFILE);
    assertFalse(handler.isEligible(payload));
  }

  @Test
  void isEligible_negative_notDeleteAction() {
    var actionProfile = (ActionProfile) payload.getCurrentNode().getContent();
    actionProfile.setAction(ActionProfile.Action.UPDATE);
    assertFalse(handler.isEligible(payload));
  }

  @Test
  void isEligible_negative_notMarcAuthorityRecord() {
    var actionProfile = (ActionProfile) payload.getCurrentNode().getContent();
    actionProfile.setFolioRecord(ActionProfile.FolioRecord.INSTANCE);
    assertFalse(handler.isEligible(payload));
  }
}
