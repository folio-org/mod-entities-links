package org.folio.entlinks.service.messaging.authority;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.folio.entlinks.integration.internal.MappingRulesService;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeField;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityMappingRulesProcessingServiceTest {

  private @Mock MappingRulesService mappingRulesService;
  private @InjectMocks AuthorityMappingRulesProcessingService service;

  @Test
  @SneakyThrows
  void getTagByAuthorityChange_positive() {
    when(mappingRulesService.getFieldTargetsMappingRelations()).thenReturn(Optional.of(Map.of(
      "100", List.of("corporateName", "personalName"),
      "200", emptyList()
    )));

    var actual = service.getTagByAuthorityChangeField(AuthorityChangeField.PERSONAL_NAME);
    assertThat(actual).hasValue("100");
  }
}
