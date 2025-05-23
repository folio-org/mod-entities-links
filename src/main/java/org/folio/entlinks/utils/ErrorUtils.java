package org.folio.entlinks.utils;

import static java.util.Collections.singletonList;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.folio.tenant.domain.dto.Parameter;

@UtilityClass
public class ErrorUtils {

  public static Parameter createErrorParameter(String key, String value) {
    return new Parameter().key(key).value(value);
  }

  public static List<Parameter> createErrorParameters(String key, String value) {
    return singletonList(createErrorParameter(key, value));
  }
}
