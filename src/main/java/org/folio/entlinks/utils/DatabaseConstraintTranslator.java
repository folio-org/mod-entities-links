package org.folio.entlinks.utils;

import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_ID;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_SOURCE_FILE_CODE;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_SOURCE_FILE_ID;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_SOURCE_FILE_NAME;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_SOURCE_FILE_SEQUENCE;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_SOURCE_FILE_URL;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_NOTE_TYPE_NAME;
import static org.folio.entlinks.config.constants.ErrorCode.UNKNOWN_CONSTRAINT;
import static org.folio.entlinks.config.constants.ErrorCode.VIOLATION_OF_RELATION_BETWEEN_AUTHORITY_AND_SOURCE_FILE;

import java.util.Map;
import org.folio.entlinks.config.constants.ErrorCode;
import org.hibernate.exception.ConstraintViolationException;

public final class DatabaseConstraintTranslator {

  private static final Map<String, ErrorCode> DB_CONSTRAINTS_I18N_MAP = Map.of(
    "authority_note_type_name_unq", DUPLICATE_NOTE_TYPE_NAME,
    "authority_source_file_name_unq", DUPLICATE_AUTHORITY_SOURCE_FILE_NAME,
    "authority_source_file_base_url_unq", DUPLICATE_AUTHORITY_SOURCE_FILE_URL,
    "authority_source_file_code_unq", DUPLICATE_AUTHORITY_SOURCE_FILE_CODE,
    "pk_authority_storage", DUPLICATE_AUTHORITY_ID,
    "authority_storage_source_file_id_foreign_key", VIOLATION_OF_RELATION_BETWEEN_AUTHORITY_AND_SOURCE_FILE,
    "authority_source_file_sequence_name_unq", DUPLICATE_AUTHORITY_SOURCE_FILE_SEQUENCE,
    "pk_authority_source_file", DUPLICATE_AUTHORITY_SOURCE_FILE_ID
  );

  private DatabaseConstraintTranslator() {}

  public static ErrorCode translate(ConstraintViolationException cve) {
    return DB_CONSTRAINTS_I18N_MAP.getOrDefault(cve.getConstraintName(), UNKNOWN_CONSTRAINT);
  }
}
