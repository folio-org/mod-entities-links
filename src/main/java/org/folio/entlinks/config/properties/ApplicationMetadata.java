package org.folio.entlinks.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.application")
public class ApplicationMetadata {

  private String name;
  private String version;

  public String getFullApplicationName() {
    return name + "-" + version;
  }
}
