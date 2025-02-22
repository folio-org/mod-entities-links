package org.folio.entlinks.config.constants;

import lombok.Getter;

@Getter
public enum ErrorCode {
  UNKNOWN_CONSTRAINT("-1", "Unknown constraint."),
  NO_SUGGESTIONS("101", ""),
  MORE_THAN_ONE_SUGGESTIONS("102", ""),
  DISABLED_AUTO_LINKING("103", ""),
  DUPLICATE_AUTHORITY_SOURCE_FILE_NAME("104", "Authority source file with the given 'name' already exists."),
  DUPLICATE_AUTHORITY_SOURCE_FILE_URL("105", "Authority source file with the given 'baseUrl' already exists."),
  DUPLICATE_AUTHORITY_SOURCE_FILE_CODE("106", "Authority source file with the given 'code' already exists."),
  DUPLICATE_NOTE_TYPE_NAME("107", "Authority note type with the given 'name' already exists."),
  VIOLATION_OF_RELATION_BETWEEN_AUTHORITY_AND_SOURCE_FILE("108",
      "Cannot complete operation on the entity due to it's relation with Authority/Authority Source File."),
  DUPLICATE_AUTHORITY_ID("109",
      "Authority with the given 'id' already exists."),
  DUPLICATE_AUTHORITY_SOURCE_FILE_SEQUENCE("110",
      "Authority source file with the same given 'code' and HRID generator name already exist."),
  DUPLICATE_AUTHORITY_SOURCE_FILE_ID("111",
      "Authority Source File with the given 'id' already exists."),
  NOT_EXISTED_AUTHORITY_SOURCE_FILE("112",
    "Authority Source File with the given 'id' does not exists."),
  VIOLATION_OF_RELATION_BETWEEN_AUTHORITY_ARCHIVE_AND_SOURCE_FILE("113",
    "Cannot complete operation on the entity due to it's relation with Authority Archive/Authority.");

  private final String code;
  private final String message;

  ErrorCode(String code, String message) {
    this.code = code;
    this.message = message;
  }
}
