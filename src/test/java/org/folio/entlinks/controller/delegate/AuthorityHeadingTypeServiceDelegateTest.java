package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.entlinks.controller.converter.AuthorityHeadingTypeMapper;
import org.folio.entlinks.domain.dto.AuthorityHeadingTypeDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityHeadingType;
import org.folio.entlinks.service.authority.AuthorityHeadingTypeService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityHeadingTypeServiceDelegateTest {

  @Mock
  private AuthorityHeadingTypeService service;
  @Mock
  private AuthorityHeadingTypeMapper mapper;

  @InjectMocks
  private AuthorityHeadingTypeServiceDelegate delegate;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(service, mapper);
  }

  @Test
  void getAuthorityHeadingTypes_positive_returnsMappedCollection() {
    var offset = 0;
    var limit = 10;
    var cqlQuery = "code==personalName";
    var page = new PageImpl<>(List.of(new AuthorityHeadingType()));
    var expectedCollection = new AuthorityHeadingTypeDtoCollection(List.of(), 1);
    when(service.getAll(offset, limit, cqlQuery)).thenReturn(page);
    when(mapper.toAuthorityHeadingTypeCollection(page)).thenReturn(expectedCollection);

    var result = delegate.getAuthorityHeadingTypes(offset, limit, cqlQuery);

    assertThat(result).isEqualTo(expectedCollection);
    verify(service).getAll(offset, limit, cqlQuery);
    verify(mapper).toAuthorityHeadingTypeCollection(page);
  }
}
