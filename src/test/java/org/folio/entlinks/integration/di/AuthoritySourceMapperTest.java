package org.folio.entlinks.integration.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.dto.RecordType.MARC_AUTHORITY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.SettingsService;
import org.folio.entlinks.integration.dto.MappingMetadata;
import org.folio.entlinks.integration.internal.MappingRulesService;
import org.folio.processing.mapping.defaultmapper.MarcToAuthorityMapper;
import org.folio.processing.mapping.defaultmapper.MarkToAuthorityExtendedMapper;
import org.folio.processing.mapping.defaultmapper.RecordMapper;
import org.folio.processing.mapping.defaultmapper.processor.parameters.MappingParameters;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthoritySourceMapperTest {

  private static final String JOB_ID = "jobId";

  @Mock
  private Cache cache;
  @Mock
  private MappingRulesService mappingRulesService;
  @Mock
  private SettingsService settingsService;
  @Mock
  private DataImportEventPayload payload;
  @Spy
  @SuppressWarnings("rawtypes")
  private RecordMapper extendedMapper = new MarkToAuthorityExtendedMapper();
  @Spy
  @SuppressWarnings("rawtypes")
  private RecordMapper simpleMapper = new MarcToAuthorityMapper();
  private AuthoritySourceMapper authoritySourceMapper;

  @BeforeEach
  @SuppressWarnings({"unchecked", "rawtypes"})
  void setUp() {
    List recordMappers = List.of(extendedMapper, simpleMapper);
    authoritySourceMapper = new AuthoritySourceMapper(cache, settingsService, mappingRulesService, recordMappers);
  }

  @Test
  void map_returnsAuthorityDtoSuccessfully() {
    mockPayload();
    mockCacheEmpty();
    mockMappingMetadata();
    mockExtendedMappingSetting(false);

    // Act
    var result = authoritySourceMapper.map(payload);

    // Assert
    assertSuccessfullyMapped(result);
    verify(simpleMapper).mapRecord(any(), any(), any());
  }

  @Test
  void map_returnsAuthorityDtoSuccessfully_extendedMappingEnabled() {
    mockPayload();
    mockCacheEmpty();
    mockMappingMetadata();
    mockExtendedMappingSetting(true);

    // Act
    var result = authoritySourceMapper.map(payload);

    // Assert
    assertSuccessfullyMapped(result);
    verify(extendedMapper).mapRecord(any(), any(), any());
  }

  @Test
  void map_returnsAuthorityDtoSuccessfully_extendedMappingCached() {
    mockPayload();
    mockCacheReturnTrue();
    mockMappingMetadata();

    // Act
    var result = authoritySourceMapper.map(payload);

    // Assert
    assertSuccessfullyMapped(result);
    verify(extendedMapper).mapRecord(any(), any(), any());
    verifyNoInteractions(settingsService);
  }

  @Test
  void map_throwsExceptionOnInvalidPayload() {
    when(payload.getJobExecutionId()).thenReturn(JOB_ID);
    when(mappingRulesService.getMappingMetadata(any())).thenThrow(new RuntimeException("Mapping error"));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> authoritySourceMapper.map(payload));
  }

  private void assertSuccessfullyMapped(AuthorityDto result) {
    assertThat(result).isNotNull()
      .extracting(AuthorityDto::getSource)
      .isEqualTo("MARC");
  }

  private void mockExtendedMappingSetting(boolean isEnabled) {
    when(settingsService.isAuthorityExtendedMappingEnabled()).thenReturn(isEnabled);
  }

  private void mockPayload() {
    var authorityJson = "{\"parsedRecord\":{\"content\":\"{\\\"naturalId\\\":\\\"n111\\\"}\"}}";
    HashMap<String, String> context = new HashMap<>();
    context.put(MARC_AUTHORITY.getValue(), authorityJson);
    when(payload.getJobExecutionId()).thenReturn(JOB_ID);
    when(payload.getContext()).thenReturn(context);
  }

  private void mockCacheEmpty() {
    when(cache.get(any(), ArgumentMatchers.<Callable<Boolean>>any()))
      .thenAnswer(invocation -> invocation.getArgument(1, Callable.class).call());
  }

  private void mockMappingMetadata() {
    when(mappingRulesService.getMappingMetadata(any()))
      .thenReturn(new MappingMetadata(new MappingParameters(), new JsonObject()));
  }

  private void mockCacheReturnTrue() {
    when(cache.get(any(), ArgumentMatchers.<Callable<Boolean>>any())).thenReturn(Boolean.TRUE);
  }
}
