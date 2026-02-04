package org.folio.support;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@UtilityClass
public class JsonTestUtils {

  @SneakyThrows
  public static String asJson(Object value, ObjectMapper mapper) {
    return mapper.writeValueAsString(value);
  }

  @SneakyThrows
  public static <T> T toObject(String json, TypeReference<T> type, ObjectMapper mapper) {
    return mapper.readValue(json, type);
  }
}
