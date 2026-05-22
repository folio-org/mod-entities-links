package org.folio.entlinks.utils;

import static org.folio.entlinks.exception.type.ErrorType.VALIDATION_ERROR;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.folio.tenant.domain.dto.Error;
import org.folio.tenant.domain.dto.Parameter;

@UtilityClass
public class ConstraintViolationResolver {

  public static Error toError(ConstraintViolation<?> violation) {
    var lastNode = getLastNode(violation.getPropertyPath());
    var paramName = lastNode != null ? lastNode.getName() : null;
    var message = paramName != null
                  ? paramName + ": " + violation.getMessage()
                  : violation.getMessage();
    var error = new Error(message)
      .code(VALIDATION_ERROR.getValue())
      .type(jakarta.validation.ConstraintViolationException.class.getSimpleName());
    if (paramName != null) {
      error.parameters(List.of(new Parameter()
        .key(paramName)
        .value(String.valueOf(violation.getInvalidValue()))));
    }
    return error;
  }

  private static Path.Node getLastNode(Path propertyPath) {
    Path.Node last = null;
    for (Path.Node node : propertyPath) {
      last = node;
    }
    return last;
  }
}
