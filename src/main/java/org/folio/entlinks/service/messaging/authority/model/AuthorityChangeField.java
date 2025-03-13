package org.folio.entlinks.service.messaging.authority.model;

import static org.folio.entlinks.domain.entity.AuthorityConstants.CHRON_SUBDIVISION_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CHRON_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CORPORATE_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CORPORATE_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.FORM_SUBDIVISION_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GENERAL_SUBDIVISION_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GENRE_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GEOGRAPHIC_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GEOGRAPHIC_SUBDIVISION_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEDIUM_PERF_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEETING_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEETING_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.NAMED_EVENT_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.NATURAL_ID_FIELD;
import static org.folio.entlinks.domain.entity.AuthorityConstants.PERSONAL_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.PERSONAL_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.TOPICAL_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.UNIFORM_TITLE_HEADING;

import lombok.Getter;

@Getter
public enum AuthorityChangeField {

  PERSONAL_NAME(PERSONAL_NAME_HEADING),
  PERSONAL_NAME_TITLE(PERSONAL_NAME_TITLE_HEADING),
  CORPORATE_NAME(CORPORATE_NAME_HEADING),
  CORPORATE_NAME_TITLE(CORPORATE_NAME_TITLE_HEADING),
  MEETING_NAME(MEETING_NAME_HEADING),
  MEETING_NAME_TITLE(MEETING_NAME_TITLE_HEADING),
  UNIFORM_TITLE(UNIFORM_TITLE_HEADING),
  NAMED_EVENT(NAMED_EVENT_HEADING),
  TOPICAL_TERM(TOPICAL_TERM_HEADING),
  GEOGRAPHIC_NAME(GEOGRAPHIC_NAME_HEADING),
  GENRE_TERM(GENRE_TERM_HEADING),
  CHRON_TERM(CHRON_TERM_HEADING),
  MEDIUM_PERF_TERM(MEDIUM_PERF_TERM_HEADING),
  GENERAL_SUBDIVISION(GENERAL_SUBDIVISION_HEADING),
  GEOGRAPHIC_SUBDIVISION(GEOGRAPHIC_SUBDIVISION_HEADING),
  CHRON_SUBDIVISION(CHRON_SUBDIVISION_HEADING),
  FORM_SUBDIVISION(FORM_SUBDIVISION_HEADING),
  NATURAL_ID(NATURAL_ID_FIELD);

  private final String fieldName;

  AuthorityChangeField(String fieldName) {
    this.fieldName = fieldName;
  }

  public static AuthorityChangeField fromValue(String value) {
    for (AuthorityChangeField b : AuthorityChangeField.values()) {
      if (b.fieldName.equalsIgnoreCase(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
