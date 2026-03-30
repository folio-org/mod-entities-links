package org.folio.entlinks.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.entlinks.controller.delegate.AuthorityHeadingTypeServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityHeadingTypeDtoCollection;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityHeadingTypesControllerTest {

  @Mock
  private AuthorityHeadingTypeServiceDelegate delegate;

  @InjectMocks
  private AuthorityHeadingTypesController controller;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(delegate);
  }

  @Test
  void retrieveAuthorityHeadingTypes_positive_returnsOkResponse() {
    var offset = 0;
    var limit = 10;
    var query = "queryable==true";
    var expectedCollection = new AuthorityHeadingTypeDtoCollection(List.of(), 0);
    when(delegate.getAuthorityHeadingTypes(offset, limit, query)).thenReturn(expectedCollection);

    var response = controller.retrieveAuthorityHeadingTypes(offset, limit, query);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
    assertThat(response.getBody()).isSameAs(expectedCollection);
    verify(delegate).getAuthorityHeadingTypes(offset, limit, query);
  }
}
