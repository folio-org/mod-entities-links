package org.folio.entlinks.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class AuthorityHeadingTypeTest {

  @Test
  void markNotNew_positive_marksEntityAsPersisted() {
    var entity = new AuthorityHeadingType();

    assertThat(entity.isNew()).isTrue();

    entity.markNotNew();

    assertThat(entity.isNew()).isFalse();
  }
}
