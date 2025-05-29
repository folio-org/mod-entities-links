package org.folio.entlinks.service.links.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.folio.spring.testing.type.UnitTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class SubfieldValidationTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("subfieldTestCases")
  void shouldValidateSubfield(SubfieldTestCase testCase) {
    boolean result = SubfieldValidation.isValidSubfield(testCase.subfield());

    assertEquals(testCase.expectedValid(), result,
      String.format("Expected '%c' to %sbe a valid subfield.",
        testCase.subfield(),
        testCase.expectedValid() ? "" : "not ")
    );
  }

  private static Stream<SubfieldTestCase> subfieldTestCases() {
    return Stream.of(
      new SubfieldTestCase('c', true, "lowercase letter"),
      new SubfieldTestCase('A', false, "uppercase letter"),
      new SubfieldTestCase('4', true, "valid digit"),
      new SubfieldTestCase('9', false, "invalid digit"),
      new SubfieldTestCase('@', false, "special character")
    );
  }

  record SubfieldTestCase(
    char subfield,
    boolean expectedValid,
    String description
  ) {

    @Override
    public @NotNull String toString() {
      return String.format("subfield ''%s'' (%s) should be %s",
        subfield, description, expectedValid ? "valid" : "invalid");
    }
  }
}
