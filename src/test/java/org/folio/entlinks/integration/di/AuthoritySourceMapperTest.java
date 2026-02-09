package org.folio.entlinks.integration.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.dto.RecordType.MARC_AUTHORITY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.integration.dto.MappingMetadata;
import org.folio.entlinks.integration.internal.MappingRulesService;
import org.folio.processing.mapping.defaultmapper.RecordMapper;
import org.folio.processing.mapping.defaultmapper.processor.parameters.MappingParameters;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthoritySourceMapperTest {

  private static final String JOB_ID = "jobId";

  @Mock
  private MappingRulesService mappingRulesService;
  @Mock
  private RecordMapper<Object> recordMapper;
  @Mock
  private DataImportEventPayload payload;
  @Mock
  private MappingParameters params;
  @InjectMocks
  private AuthoritySourceMapper authoritySourceMapper;

  @Test
  void map_returnsAuthorityDtoSuccessfully() {
    var authorityJson = "{\"parsedRecord\":{\"content\":\"{\\\"naturalId\\\":\\\"n111\\\"}\"}}";
    HashMap<String, String> context = new HashMap<>();
    context.put(MARC_AUTHORITY.getValue(), authorityJson);

    when(payload.getJobExecutionId()).thenReturn(JOB_ID);
    when(payload.getContext()).thenReturn(context);
    when(mappingRulesService.getMappingMetadata(any())).thenReturn(new MappingMetadata(params, new JsonObject()));

    // Act
    var result = authoritySourceMapper.map(payload);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getSource()).isEqualTo("MARC");
  }

  @Test
  void map_throwsExceptionOnInvalidPayload() {
    when(payload.getJobExecutionId()).thenReturn(JOB_ID);
    when(mappingRulesService.getMappingMetadata(any())).thenThrow(new RuntimeException("Mapping error"));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> authoritySourceMapper.map(payload));
  }
}
