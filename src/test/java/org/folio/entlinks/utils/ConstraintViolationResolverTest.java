package org.folio.entlinks.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ConstraintViolationResolverTest {

  private static final String VALIDATION_CODE = "validation";
  private static final String CVE_TYPE = ConstraintViolationException.class.getSimpleName();

  @Test
  void toError_positive_setsMessageWithParamName() {
    // Arrange
    var violation = violation("limit", 0, "must be greater than or equal to 1");

    // Act
    var error = ConstraintViolationResolver.toError(violation);

    // Assert
    assertThat(error.getMessage()).isEqualTo("limit: must be greater than or equal to 1");
  }

  @Test
  void toError_positive_setsValidationCodeAndType() {
    // Arrange
    var violation = violation("offset", -1, "must be greater than or equal to 0");

    // Act
    var error = ConstraintViolationResolver.toError(violation);

    // Assert
    assertThat(error.getCode()).isEqualTo(VALIDATION_CODE);
    assertThat(error.getType()).isEqualTo(CVE_TYPE);
  }

  @Test
  void toError_positive_addsParameterWithKeyAndInvalidValue() {
    // Arrange
    var violation = violation("limit", 0, "must be greater than or equal to 1");

    // Act
    var error = ConstraintViolationResolver.toError(violation);

    // Assert
    assertThat(error.getParameters()).hasSize(1);
    var param = error.getParameters().getFirst();
    assertThat(param.getKey()).isEqualTo("limit");
    assertThat(param.getValue()).isEqualTo("0");
  }

  @Test
  void toError_positive_invalidValueIsStringified() {
    // Arrange
    var violation = violation("size", 9999, "must be less than or equal to 2000");

    // Act
    var error = ConstraintViolationResolver.toError(violation);

    // Assert
    assertThat(error.getParameters()).hasSize(1);
    assertThat(error.getParameters().getFirst().getValue()).isEqualTo("9999");
  }

  @Test
  void toError_positive_multiNodePathUsesLastNode() {
    // Arrange — path: methodName -> paramName; only last node (paramName) must be used
    var violation = violation(List.of("retrieveAuthorityIdentifierTypes", "limit"), 1,
      "must be less than or equal to 2000");

    // Act
    var error = ConstraintViolationResolver.toError(violation);

    // Assert
    assertThat(error.getMessage()).isEqualTo("limit: must be less than or equal to 2000");
    assertThat(error.getParameters()).hasSize(1);
    assertThat(error.getParameters().getFirst().getKey()).isEqualTo("limit");
  }

  @Test
  void toError_negative_emptyPathProducesRawMessage() {
    // Arrange — empty property path: no nodes at all
    var violation = violationWithPath(emptyPath(), "must not be null");

    // Act
    var error = ConstraintViolationResolver.toError(violation);

    // Assert — raw message, no "null: " prefix, no parameters
    assertThat(error.getMessage()).isEqualTo("must not be null");
    assertThat(error.getParameters()).isNullOrEmpty();
  }

  @Test
  void toError_negative_nullNodeNameProducesRawMessage() {
    // Arrange — single node whose getName() returns null
    var violation = violationWithPath(pathWithNullNamedNode(), "must not be null");

    // Act
    var error = ConstraintViolationResolver.toError(violation);

    // Assert
    assertThat(error.getMessage()).isEqualTo("must not be null");
    assertThat(error.getParameters()).isNullOrEmpty();
  }

  private static ConstraintViolation<?> violation(String paramName, Object invalidValue, String message) {
    return violation(List.of(paramName), invalidValue, message);
  }

  @SuppressWarnings("unchecked")
  private static ConstraintViolation<?> violation(List<String> nodeNames, Object invalidValue, String message) {
    // Build path completely first
    var path = pathOf(nodeNames);
    // Now stub the violation — no nested when() in progress
    var v = (ConstraintViolation<Object>) mock(ConstraintViolation.class);
    when(v.getMessage()).thenReturn(message);
    when(v.getInvalidValue()).thenReturn(invalidValue);
    when(v.getPropertyPath()).thenReturn(path);
    return v;
  }

  @SuppressWarnings("unchecked")
  private static ConstraintViolation<?> violationWithPath(Path path, String message) {
    var v = (ConstraintViolation<Object>) mock(ConstraintViolation.class);
    when(v.getMessage()).thenReturn(message);
    when(v.getPropertyPath()).thenReturn(path);
    return v;
  }

  private static Path pathOf(List<String> nodeNames) {
    // Build and fully stub all nodes before touching the Path mock
    var nodes = new ArrayList<Path.Node>(nodeNames.size());
    for (var name : nodeNames) {
      var node = mock(Path.Node.class);
      when(node.getName()).thenReturn(name);
      nodes.add(node);
    }
    var path = mock(Path.class);
    when(path.iterator()).thenAnswer(inv -> nodes.iterator());
    return path;
  }

  private static Path emptyPath() {
    var path = mock(Path.class);
    when(path.iterator()).thenAnswer(inv -> Collections.<Path.Node>emptyIterator());
    return path;
  }

  private static Path pathWithNullNamedNode() {
    var node = mock(Path.Node.class);
    when(node.getName()).thenReturn(null);
    var path = mock(Path.class);
    when(path.iterator()).thenAnswer(inv -> List.of(node).iterator());
    return path;
  }
}
