package org.folio.entlinks.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.Parameter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class ErrorUtilsTest {

  private static final String KEY_1 = "key1";
  private static final String VALUE_1 = "value1";
  private static final String KEY_2 = "key2";
  private static final String VALUE_2 = "value2";

  private void assertParameter(Parameter parameter, String expectedKey, String expectedValue) {
    assertThat(parameter.getKey()).isEqualTo(expectedKey);
    assertThat(parameter.getValue()).isEqualTo(expectedValue);
  }

  @Nested
  class ValidInputTests {

    @Test
    void shouldCreateParametersFromValidKeyValuePairs() {
      // Given: valid key-value pairs
      String[] keyValuePairs = {KEY_1, VALUE_1, KEY_2, VALUE_2};

      // When: creating error parameters
      List<Parameter> result = ErrorUtils.createErrorParameters(keyValuePairs);

      // Then: parameters should be created correctly
      assertThat(result)
        .hasSize(2)
        .satisfies(parameters -> {
          assertParameter(parameters.get(0), KEY_1, VALUE_1);
          assertParameter(parameters.get(1), KEY_2, VALUE_2);
        });
    }

    @Test
    void shouldCreateEmptyListForEmptyInput() {
      List<Parameter> result = ErrorUtils.createErrorParameters();

      assertThat(result).isEmpty();
    }

    @Test
    void shouldAllowNullValues() {
      // Given: key-value pair with null value
      String[] keyValuePairs = {KEY_1, null};

      // When: creating error parameters
      List<Parameter> result = ErrorUtils.createErrorParameters(keyValuePairs);

      // Then: parameter should be created with null value
      assertThat(result)
        .hasSize(1)
        .satisfies(parameters -> assertParameter(parameters.get(0), KEY_1, null));
    }
  }

  @Nested
  class InvalidInputTests {

    @Test
    void shouldThrowExceptionForOddNumberOfArguments() {
      String[] keyValuePairs = {KEY_1, VALUE_1, KEY_2};

      assertThrows(IllegalArgumentException.class,
        () -> ErrorUtils.createErrorParameters(keyValuePairs));
    }

    @Test
    void shouldThrowExceptionForNullKey() {
      String[] keyValuePairs = {null, VALUE_1};

      assertThrows(NullPointerException.class,
        () -> ErrorUtils.createErrorParameters(keyValuePairs));
    }
  }
}
