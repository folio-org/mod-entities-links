package org.folio.entlinks.utils;

import java.util.Arrays;
import lombok.experimental.UtilityClass;
import org.apache.kafka.common.header.Headers;

@UtilityClass
public class HeaderUtils {

  public static String extractHeaderValue(String headerKey, Headers headers) {
    if (headerKey == null || headers == null) {
      return "";
    }
    return Arrays.stream(headers.toArray())
      .filter(header -> headerKey.equalsIgnoreCase(header.key()) && header.value() != null)
      .findFirst()
      .map(header -> new String(header.value()))
      .orElse("");
  }
}
