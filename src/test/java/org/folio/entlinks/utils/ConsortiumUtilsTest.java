package org.folio.entlinks.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class ConsortiumUtilsTest {

  @Test
  @DisplayName("true when source starts with consortium prefix")
  void shouldReturnTrueForConsortiumMarcSource() {
    assertTrue(ConsortiumUtils.isConsortiumShadowCopy("CONSORTIUM-MARC"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"NON-CONSORTIUM"})
  @DisplayName("false when source is null, empty or not consortium")
  void shouldReturnFalse(String source) {
    assertFalse(ConsortiumUtils.isConsortiumShadowCopy(source));
  }
}
