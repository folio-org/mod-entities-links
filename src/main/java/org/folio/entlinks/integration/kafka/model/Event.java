package org.folio.entlinks.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class Event {
  @JsonProperty("id")
  private String id;
  @JsonProperty("eventType")
  private String eventType;
  @JsonProperty("eventMetadata")
  private EventMetadata eventMetadata;
  @JsonProperty("eventPayload")
  private String eventPayload;
}
