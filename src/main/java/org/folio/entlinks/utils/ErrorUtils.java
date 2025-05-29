package org.folio.entlinks.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.folio.tenant.domain.dto.Parameter;

/**
 * Utility class for creating error-related parameters.
 * Provides methods to construct error parameters as {@code Parameter} objects
 * and lists of such objects.
 */
@UtilityClass
public class ErrorUtils {

  /**
   * Creates a Parameter object with the specified key and value.
   *
   * @param key   the parameter key (must not be null)
   * @param value the parameter value
   * @return a new Parameter instance
   * @throws IllegalArgumentException if key is null
   */
  public static Parameter createErrorParameter(String key, String value) {
    Objects.requireNonNull(key, "Parameter key must not be null");
    return new Parameter().key(key).value(value);
  }

  /**
   * Creates a list of Parameters from provided key-value pairs.
   *
   * @param keyValuePairs key-value pairs in form [key1, value1, key2, value2, ...]
   * @return a list of Parameter objects
   * @throws IllegalArgumentException if an odd number of arguments or a null key is provided
   */
  public static List<Parameter> createErrorParameters(String... keyValuePairs) {
    if (keyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("Even number of arguments required");
    }
    var parameters = new ArrayList<Parameter>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      parameters.add(createErrorParameter(keyValuePairs[i], keyValuePairs[i + 1]));
    }
    return parameters;
  }
}
