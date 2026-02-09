package org.folio.entlinks.integration.di.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.ActionProfile.FolioRecord.AUTHORITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.ActionProfile;
import org.folio.DataImportEventPayload;
import org.folio.DataImportEventTypes;
import org.folio.entlinks.controller.delegate.AuthorityServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.di.AuthoritySourceMapper;
import org.folio.processing.exceptions.EventProcessingException;
import org.folio.rest.jaxrs.model.ProfileSnapshotWrapper;
import org.folio.rest.jaxrs.model.ProfileType;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityCreateEventHandlerTest {

  private static final String MOCKED_AUTHORITY_DTO_AS_STRING = "mocked authority dto";

  @Mock
  private JsonMapper jsonMapper;
  @Mock
  private AuthorityServiceDelegate delegate;
  @Mock
  private AuthoritySourceMapper sourceMapper;
  @InjectMocks
  private AuthorityCreateEventHandler handler;

  private DataImportEventPayload payload;
  private AuthorityDto authorityDto;

  @BeforeEach
  void setUp() {
    authorityDto = new AuthorityDto().id(UUID.randomUUID());
    var childWrapper = new ProfileSnapshotWrapper();
    childWrapper.setContentType(ProfileType.MAPPING_PROFILE);

    var actionProfile = new ActionProfile();
    actionProfile.setAction(ActionProfile.Action.CREATE);
    actionProfile.setFolioRecord(ActionProfile.FolioRecord.AUTHORITY);

    var parentWrapper = new ProfileSnapshotWrapper();
    parentWrapper.setContentType(ProfileType.ACTION_PROFILE);
    parentWrapper.setContent(actionProfile);
    parentWrapper.setChildSnapshotWrappers(List.of(childWrapper));

    payload = new DataImportEventPayload()
        .withContext(new HashMap<>())
        .withEventsChain(new ArrayList<>())
        .withCurrentNode(parentWrapper);
  }

  @Test
  @SneakyThrows
  void handle_positive() {
    when(sourceMapper.map(payload)).thenReturn(authorityDto);
    when(delegate.createAuthority(authorityDto)).thenReturn(authorityDto);
    when(jsonMapper.writeValueAsString(authorityDto)).thenReturn(MOCKED_AUTHORITY_DTO_AS_STRING);

    var future = handler.handle(payload);
    var result = future.get();

    assertEquals(DataImportEventTypes.DI_INVENTORY_AUTHORITY_CREATED.value(), result.getEventType());
    assertEquals(MOCKED_AUTHORITY_DTO_AS_STRING, result.getContext().get(AUTHORITY.value()));
    assertThat(result.getEventsChain()).contains(DataImportEventTypes.DI_INVENTORY_AUTHORITY_CREATED.value());
  }

  @Test
  @SneakyThrows
  void handle_negative() {
    // Arrange
    when(sourceMapper.map(payload)).thenReturn(authorityDto);
    when(delegate.createAuthority(authorityDto)).thenReturn(authorityDto);
    when(jsonMapper.writeValueAsString(authorityDto))
        .thenThrow(new StreamReadException(null, "test error"));
    // Act + Assert
    EventProcessingException ex =
        assertThrows(EventProcessingException.class,
            () -> handler.handle(payload));

    assertThat(ex.getMessage())
        .contains("Failed to prepare payload for DI event");
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
  void isEligible_negative_notCreateAction() {
    var actionProfile = (ActionProfile) payload.getCurrentNode().getContent();
    actionProfile.setAction(ActionProfile.Action.UPDATE);
    assertFalse(handler.isEligible(payload));
  }

  @Test
  void isEligible_negative_notAuthorityRecord() {
    var actionProfile = (ActionProfile) payload.getCurrentNode().getContent();
    actionProfile.setFolioRecord(ActionProfile.FolioRecord.INSTANCE);
    assertFalse(handler.isEligible(payload));
  }

  @Test
  void isPostProcessingNeeded() {
    assertTrue(handler.isPostProcessingNeeded());
  }

  @Test
  void getPostProcessingInitializationEventType() {
    assertEquals(DataImportEventTypes.DI_INVENTORY_AUTHORITY_CREATED_READY_FOR_POST_PROCESSING.value(),
        handler.getPostProcessingInitializationEventType());
  }
}
