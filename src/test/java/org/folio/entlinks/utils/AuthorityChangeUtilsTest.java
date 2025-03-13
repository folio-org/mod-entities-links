package org.folio.entlinks.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField;
import org.folio.spring.testing.extension.Random;
import org.folio.spring.testing.extension.impl.RandomParametersExtension;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@UnitTest
@ExtendWith(RandomParametersExtension.class)
class AuthorityChangeUtilsTest {

  @Test
  void getAuthorityChanges_allFields(@Random AuthorityDto s1, @Random AuthorityDto s2) {
    var changes = AuthorityChangeUtils.getAuthorityChanges(s1, s2);
    assertThat(changes).hasSize(18)
      .containsOnlyKeys(AuthorityChangeField.values());
  }
}
