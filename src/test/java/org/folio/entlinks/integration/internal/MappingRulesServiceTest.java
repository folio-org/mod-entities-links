package org.folio.entlinks.integration.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.folio.IdentifierType;
import org.folio.entlinks.client.MappingMetadataClient;
import org.folio.entlinks.client.MappingRulesClient;
import org.folio.entlinks.client.MappingRulesClient.MappingRule;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.processing.mapping.defaultmapper.processor.parameters.MappingParameters;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class MappingRulesServiceTest {

  private static final String DATA_IMPORT_JOB_ID = "jobId";
  private static final JsonMapper MAPPER = new JsonMapper();

  private @Mock MappingRulesClient client;
  private @Mock MappingMetadataClient metadataClient;
  private @InjectMocks MappingRulesService service;

  @Test
  void getFieldTargetsMappingRelations_positive() {
    when(client.fetchAuthorityMappingRules()).thenReturn(Map.of(
      "100", List.of(new MappingRule("a1"), new MappingRule("a2")),
      "101", List.of(new MappingRule("a3"))
    ));

    var actual = service.getFieldTargetsMappingRelations();

    assertNotNull(actual);
    assertThat(actual)
        .hasSize(2)
        .contains(entry("100", List.of("a1", "a2")), entry("101", List.of("a3")));
  }

  @Test
  void getFieldTargetsMappingRelations_negative_clientException() {
    var cause = new IllegalArgumentException("test");
    when(client.fetchAuthorityMappingRules()).thenThrow(cause);

    var exception = assertThrows(FolioIntegrationException.class, () -> service.getFieldTargetsMappingRelations());
    assertEquals("Failed to fetch authority mapping rules", exception.getMessage());
  }

  @Test
  @SneakyThrows
  void getMappingMetadata_positive() {
    var mappingRules = new JsonObject("{\"test\": \"test\"}");
    var mappingParams = new MappingParameters().withIdentifierTypes(List.of(new IdentifierType().withId("typeId")));
    when(metadataClient.getMappingMetadata(DATA_IMPORT_JOB_ID)).thenReturn(
        new MappingMetadataClient.MappingMetadata(DATA_IMPORT_JOB_ID, mappingRules.encode(),
            MAPPER.writeValueAsString(mappingParams)));

    var metadata = service.getMappingMetadata(DATA_IMPORT_JOB_ID);

    assertNotNull(metadata);
    assertNotNull(metadata.mappingParameters());
    assertNotNull(metadata.mappingRules());
    assertEquals(mappingRules, metadata.mappingRules());
    assertEquals(mappingParams.getIdentifierTypes(), metadata.mappingParameters().getIdentifierTypes());
  }

  @Test
  void getMappingMetadata_negative_clientException() {
    when(metadataClient.getMappingMetadata(DATA_IMPORT_JOB_ID)).thenThrow(new RuntimeException(anyString()));

    assertThrows(FolioIntegrationException.class, () -> service.getMappingMetadata(DATA_IMPORT_JOB_ID));
  }
}
