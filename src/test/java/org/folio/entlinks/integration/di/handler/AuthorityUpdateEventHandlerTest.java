package org.folio.entlinks.integration.di.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import org.folio.DataImportEventPayload;
import org.folio.DataImportEventTypes;
import org.folio.MappingProfile;
import org.folio.entlinks.controller.delegate.AuthorityServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.di.AuthoritySourceMapper;
import org.folio.entlinks.integration.di.DataImportEventPublisher;
import org.folio.processing.exceptions.EventProcessingException;
import org.folio.rest.jaxrs.model.EntityType;
import org.folio.rest.jaxrs.model.Event;
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
class AuthorityUpdateEventHandlerTest {

  private static final String MOCKED_AUTHORITY_DTO_AS_STRING = "mocked authority dto";
  private static final String SHADOW_COPY_UPDATE_RESTRICTED_MSG =
      "Shared MARC authority record cannot be updated from this tenant";
  private static final UUID AUTHORITY_ID = UUID.randomUUID();

  @Mock
  private JsonMapper jsonMapper;
  @Mock
  private AuthorityServiceDelegate delegate;
  @Mock
  private AuthoritySourceMapper sourceMapper;
  @Mock
  private DataImportEventPublisher eventPublisher;
  @InjectMocks
  private AuthorityUpdateEventHandler handler;

  private DataImportEventPayload payload;
  private AuthorityDto authorityDto;
  private AuthorityDto existingAuthorityDto;

  @BeforeEach
  void setUp() {
    authorityDto = new AuthorityDto().id(AUTHORITY_ID);
    existingAuthorityDto = new AuthorityDto().id(AUTHORITY_ID).source("MARC").version(1);

    var mappingProfile = new MappingProfile();
    mappingProfile.setExistingRecordType(EntityType.MARC_AUTHORITY);

    var mappingProfileWrapper = new ProfileSnapshotWrapper();
    mappingProfileWrapper.setContentType(ProfileType.MAPPING_PROFILE);
    mappingProfileWrapper.setContent(mappingProfile);
    mappingProfileWrapper.setChildSnapshotWrappers(new ArrayList<>());

    payload = new DataImportEventPayload()
        .withContext(new HashMap<>())
        .withEventsChain(new ArrayList<>())
        .withCurrentNode(mappingProfileWrapper);
  }

  @Test
  @SneakyThrows
  void handle_positive() {
    when(sourceMapper.map(payload)).thenReturn(authorityDto);
    when(delegate.getAuthorityById(AUTHORITY_ID)).thenReturn(existingAuthorityDto);
    when(jsonMapper.writeValueAsString(authorityDto)).thenReturn(MOCKED_AUTHORITY_DTO_AS_STRING);
    when(eventPublisher.publish(any(DataImportEventPayload.class)))
        .thenReturn(CompletableFuture.completedFuture(new Event()));

    var future = handler.handle(payload);
    var result = future.get();

    assertEquals(DataImportEventTypes.DI_INVENTORY_AUTHORITY_UPDATED.value(), result.getEventType());
    assertEquals(MOCKED_AUTHORITY_DTO_AS_STRING, result.getContext().get("AUTHORITY"));
    assertThat(result.getEventsChain()).contains(DataImportEventTypes.DI_INVENTORY_AUTHORITY_UPDATED.value());
  }

  @Test
  void handle_negative_shouldThrowExceptionWhenShadowCopy() {
    existingAuthorityDto.setSource("CONSORTIUM-MARC");
    when(sourceMapper.map(payload)).thenReturn(authorityDto);
    when(delegate.getAuthorityById(AUTHORITY_ID)).thenReturn(existingAuthorityDto);

    var future = handler.handle(payload);

    ExecutionException ex = assertThrows(ExecutionException.class, future::get);
    assertThat(ex.getCause())
        .isInstanceOf(EventProcessingException.class)
        .hasMessage(SHADOW_COPY_UPDATE_RESTRICTED_MSG);
  }

  @Test
  @SneakyThrows
  void handle_negative_shouldThrowExceptionWhenJsonProcessingFails() {
    when(sourceMapper.map(payload)).thenReturn(authorityDto);
    when(delegate.getAuthorityById(AUTHORITY_ID)).thenReturn(existingAuthorityDto);
    doThrow(new StreamReadException("test error")).when(jsonMapper).writeValueAsString(authorityDto);

    // Act + Assert
    EventProcessingException ex =
        assertThrows(EventProcessingException.class,
            () -> handler.handle(payload));

    assertThat(ex.getMessage())
        .contains("Failed to prepare payload for DI event");
  }

  @Test
  void handle_negative_whenPublisherFails() {
    when(sourceMapper.map(payload)).thenReturn(authorityDto);
    when(delegate.getAuthorityById(AUTHORITY_ID)).thenReturn(existingAuthorityDto);
    when(eventPublisher.publish(any(DataImportEventPayload.class)))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("test error")));

    var future = handler.handle(payload);

    assertThrows(ExecutionException.class, future::get);
  }

  @Test
  void isEligible_positive() {
    assertTrue(handler.isEligible(payload));
  }

  @Test
  void isEligible_negative_notMappingProfile() {
    payload.getCurrentNode().setContentType(ProfileType.ACTION_PROFILE);
    assertFalse(handler.isEligible(payload));
  }

  @Test
  void isEligible_negative_notMarcAuthorityRecord() {
    var mappingProfile = (MappingProfile) payload.getCurrentNode().getContent();
    mappingProfile.setExistingRecordType(EntityType.INSTANCE);
    assertFalse(handler.isEligible(payload));
  }
}
