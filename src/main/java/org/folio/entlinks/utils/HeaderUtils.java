package org.folio.entlinks.utils;

import java.util.Arrays;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.kafka.common.header.Headers;

@UtilityClass
public class HeaderUtils {

  public static Optional<String> extractHeaderValue(String headerKey, Headers headers) {
    if (headerKey == null || headers == null) {
      return Optional.empty();
    }
    return Arrays.stream(headers.toArray())
      .filter(header -> headerKey.equalsIgnoreCase(header.key()) && header.value() != null)
      .findFirst()
      .map(header -> new String(header.value()));
  }
}
