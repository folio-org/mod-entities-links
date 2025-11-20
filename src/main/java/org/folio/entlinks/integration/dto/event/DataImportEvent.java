package org.folio.entlinks.integration.dto.event;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class DataImportEvent {

  @JsonProperty("id")
  @JsonPropertyDescription("A universally unique identifier (UUID), this is a 128-bit number used to identify a record and is shown in hex with dashes, for example 6312d172-f0cf-40f6-b27d-9fa8feaf332f; the UUID version must be from 1-5; see https://dev.folio.org/guides/uuids/")
  private String id;
  @JsonProperty("eventType")
  @JsonPropertyDescription("Unique Event type, human-readable String, not UUID")
  private String eventType;

  @JsonIgnore
  @Valid
  private final Map<String, Object> additionalProperties = new HashMap<>();

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }
}
