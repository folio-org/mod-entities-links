package org.folio.entlinks.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class EventMetadata {
  @JsonProperty("tenantId")
  private String tenantId;
  @JsonProperty("correlationId")
  private String correlationId;
  @JsonProperty("originalEventId")
  private String originalEventId;
  @JsonProperty("createdDate")
  private Date createdDate;
  @JsonProperty("publishedDate")
  private Date publishedDate;
  @JsonProperty("createdBy")
  private String createdBy;
  @JsonProperty("publishedBy")
  private String publishedBy;
}
