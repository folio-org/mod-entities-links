package org.folio.entlinks.service.links.validator;

import lombok.experimental.UtilityClass;

/**
 * Utility class that provides validation methods for verifying subfield characteristics
 * such as whether a subfield is valid or required.
 */
@UtilityClass
public class SubfieldValidation {

  static final char REQUIRED_SUBFIELD_A = 'a';
  static final char MIN_LETTER_SUBFIELD = 'a';
  static final char MAX_LETTER_SUBFIELD = 'z';
  static final char MIN_DIGIT_SUBFIELD = '1';
  static final char MAX_DIGIT_SUBFIELD = '8';

  public static boolean isValidSubfield(char subfield) {
    return isValidLetter(subfield) || isValidDigit(subfield);
  }

  static boolean isValidLetter(char subfield) {
    return subfield >= MIN_LETTER_SUBFIELD && subfield <= MAX_LETTER_SUBFIELD;
  }

  static boolean isValidDigit(char subfield) {
    return subfield >= MIN_DIGIT_SUBFIELD && subfield <= MAX_DIGIT_SUBFIELD;
  }

  public static boolean isRequiredSubfield(char subfield) {
    return subfield == REQUIRED_SUBFIELD_A;
  }
}
