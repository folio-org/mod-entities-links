package org.folio.entlinks.exception;

import lombok.Getter;
import org.folio.entlinks.exception.type.ErrorCode;

/**
 * Base exception class that is used for all exceptional situations.
 */
@Getter
public abstract class BaseException extends RuntimeException {

  private final ErrorCode errorCode;

  /**
   * Initialize exception with provided message and error code.
   *
   * @param message   exception message
   * @param errorCode exception code {@link ErrorCode}
   */
  protected BaseException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * Initialize exception with provided message and error code.
   *
   * @param message   exception message
   * @param errorCode exception code {@link ErrorCode}
   * @param cause     cause Exception
   */
  protected BaseException(String message, ErrorCode errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
