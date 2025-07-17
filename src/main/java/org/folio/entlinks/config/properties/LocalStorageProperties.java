package org.folio.entlinks.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "folio.local-storage")
public class LocalStorageProperties {
  /**
   * Provides the S3 subpath for local files storage.
   */
  private String s3LocalSubPath = "mod-entities-storage";
}
