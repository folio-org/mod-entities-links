package org.folio.entlinks.service.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthorityIdentifierType;
import org.folio.entlinks.domain.repository.AuthorityIdentifierTypeRepository;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityIdentifierTypeServiceTest {

  @Mock
  private AuthorityIdentifierTypeRepository repository;

  @InjectMocks
  private AuthorityIdentifierTypeService service;

  @Test
  void getAll_positive_withoutCqlReturnsPagedResult() {
    // Arrange
    var expectedPage = new PageImpl<>(List.of(new AuthorityIdentifierType()));
    when(repository.findAll(any(Pageable.class))).thenReturn(expectedPage);

    // Act
    var result = service.getAll(0, 10, null);

    // Assert
    assertThat(result).isEqualTo(expectedPage);
  }

  @Test
  void getAll_positive_withCqlReturnsPagedResult() {
    // Arrange
    var expectedPage = new PageImpl<>(List.of(new AuthorityIdentifierType()));
    var cqlQuery = "name==isbn";
    when(repository.findByCql(eq(cqlQuery), any(Pageable.class))).thenReturn(expectedPage);

    // Act
    var result = service.getAll(0, 10, cqlQuery);

    // Assert
    assertThat(result).isEqualTo(expectedPage);
  }

  @Test
  void findById_positive_existingIdReturnsEntity() {
    // Arrange
    var id = UUID.randomUUID();
    var expectedEntity = new AuthorityIdentifierType();
    when(repository.findById(id)).thenReturn(Optional.of(expectedEntity));

    // Act
    var result = service.findById(id);

    // Assert
    assertThat(result).isEqualTo(expectedEntity);
  }

  @Test
  void findById_negative_nullIdReturnsNull() {
    // Act
    var result = service.findById(null);

    // Assert
    assertNull(result);
    verifyNoInteractions(repository);
  }

  @Test
  void findByName_positive_existingNameReturnsEntity() {
    // Arrange
    var name = "isbn";
    var expectedEntity = new AuthorityIdentifierType();
    when(repository.findByName(name)).thenReturn(Optional.of(expectedEntity));

    // Act
    var result = service.findByName(name);

    // Assert
    assertThat(result).isEqualTo(expectedEntity);
  }

  @Test
  void findByName_negative_blankNameReturnsNull() {
    // Act
    var result = service.findByName(" ");

    // Assert
    assertNull(result);
    verifyNoInteractions(repository);
  }

  @Test
  void create_positive_initializesIdAndReturnsSavedEntity() {
    // Arrange
    when(repository.save(any(AuthorityIdentifierType.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    var result = service.create(new AuthorityIdentifierType());

    // Assert
    assertThat(result.getId()).isNotNull();
  }
}
