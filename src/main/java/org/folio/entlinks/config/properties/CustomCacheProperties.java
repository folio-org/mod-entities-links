package org.folio.entlinks.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = "folio.cache")
public class CustomCacheProperties {

  private final Map<String, CustomCache> spec = new HashMap<>();

  public record CustomCache(@Min(1) long maximumSize, @NotNull Duration ttl) { }
}
