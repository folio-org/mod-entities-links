package org.folio.support;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtils {

  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> mapOf(K k1, V v1, Object... pairs) {
    if (pairs.length % 2 != 0) {
      throw new IllegalArgumentException("pairs must contain an even number of elements (key/value pairs)");
    }
    Map<K, V> map = new LinkedHashMap<>();
    map.put(k1, v1);
    for (int i = 0; i < pairs.length; i += 2) {
      Object key = pairs[i];
      Object value = pairs[i + 1];
      map.put((K) key, (V) value);
    }
    return map;
  }
}
