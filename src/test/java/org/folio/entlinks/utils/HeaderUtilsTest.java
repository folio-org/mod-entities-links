package org.folio.entlinks.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HeaderUtilsTest {

  @Test
  void shouldExtractHeaderValueWhenKeyExists() {
    var headers = new RecordHeaders();
    headers.add("test-key", "test-value".getBytes());

    var actual = HeaderUtils.extractHeaderValue("test-key", headers);

    assertFalse(actual.isEmpty());
    assertEquals("test-value", actual.get());
  }

  @ParameterizedTest
  @MethodSource("provideInvalidHeaders")
  void shouldReturnEmptyStringForInvalidHeaders(String headerKey, Headers headers) {
    var actual = HeaderUtils.extractHeaderValue(headerKey, headers);

    assertTrue(actual.isEmpty());
  }

  private static Stream<Arguments> provideInvalidHeaders() {
    return Stream.of(
      Arguments.of("test-key", createHeaders("another-key", "value")),
      Arguments.of("test-key", new RecordHeaders()),
      Arguments.of("test-key", createHeaders("test-key", null)),
      Arguments.of(null, createHeaders("test-key", "test-value")),
      Arguments.of("test-key", null)
    );
  }

  private static Headers createHeaders(String key, String value) {
    var headers = new RecordHeaders();
    if (key != null) {
      headers.add(key, value != null ? value.getBytes() : null);
    }
    return headers;
  }
}
