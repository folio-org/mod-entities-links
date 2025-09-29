package org.folio.entlinks.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ConsortiumUtils {
  public static final String CONSORTIUM_SOURCE_PREFIX = "CONSORTIUM-";

  public boolean isConsortiumShadowCopy(String source) {
    return source != null && source.startsWith(CONSORTIUM_SOURCE_PREFIX);
  }
}
