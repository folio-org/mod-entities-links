package org.folio.entlinks.service.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
import org.folio.entlinks.domain.entity.AuthorityHeadingType;
import org.folio.entlinks.domain.repository.AuthorityHeadingTypeRepository;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityHeadingTypeServiceTest {

  @Mock
  private AuthorityHeadingTypeRepository repository;

  @InjectMocks
  private AuthorityHeadingTypeService service;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(repository);
  }

  @ParameterizedTest
  @MethodSource("blankCqlProvider")
  void getAll_positive_blankCqlReturnsPagedResult(String cql) {
    var expectedPage = new PageImpl<>(List.of(new AuthorityHeadingType()));
    var pageable = new OffsetRequest(0, 10);
    when(repository.findAll(pageable)).thenReturn(expectedPage);

    var result = service.getAll(0, 10, cql);

    assertThat(result).isEqualTo(expectedPage);
    verify(repository).findAll(pageable);
  }

  @Test
  void getAll_positive_withCqlReturnsPagedResult() {
    var expectedPage = new PageImpl<>(List.of(new AuthorityHeadingType()));
    var pageable = new OffsetRequest(5, 15);
    var cqlQuery = "queryable==true";
    when(repository.findByCql(cqlQuery, pageable)).thenReturn(expectedPage);

    var result = service.getAll(5, 15, cqlQuery);

    assertThat(result).isEqualTo(expectedPage);
    verify(repository).findByCql(cqlQuery, pageable);
  }

  private static Stream<String> blankCqlProvider() {
    return Stream.of(null, "", " ");
  }
}
