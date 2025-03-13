package org.folio.entlinks.utils;

import static org.folio.entlinks.utils.ObjectUtils.getDifference;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChange;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField;

@Log4j2
@UtilityClass
public class AuthorityChangeUtils {

  @SneakyThrows
  public static Map<AuthorityChangeField, AuthorityChange> getAuthorityChanges(AuthorityDto s1, AuthorityDto s2) {
    return getDifference(s1, s2).stream()
      .map(difference -> {
        try {
          var authorityChangeField = AuthorityChangeField.fromValue(difference.fieldName());
          return new AuthorityChange(authorityChangeField, difference.val1(), difference.val2());
        } catch (IllegalArgumentException e) {
          log.debug("Not supported authority change [fieldName: {}]", difference);
          return null;
        }
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toMap(AuthorityChange::changeField, ac -> ac));
  }
}
