package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;
import static org.folio.support.base.TestConstants.TEST_DATE;
import static org.folio.support.base.TestConstants.TEST_ID;
import static org.folio.support.base.TestConstants.TEST_PROPERTY_VALUE;

import java.util.List;
import org.folio.entlinks.domain.dto.AuthorityIdentifierTypeDto;
import org.folio.entlinks.domain.dto.AuthorityIdentifierTypeDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityIdentifierType;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

@UnitTest
class AuthorityIdentifierTypeMapperTest {

  private final AuthorityIdentifierTypeMapper mapper = new AuthorityIdentifierTypeMapperImpl();

  @Test
  void toEntity_positive_mapsFields() {
    // Arrange
    var dto = new AuthorityIdentifierTypeDto();
    dto.setId(TEST_ID);
    dto.setName(TEST_PROPERTY_VALUE);
    dto.setCode("test-code");
    dto.setSource(TEST_PROPERTY_VALUE);

    // Act
    var entity = mapper.toEntity(dto);

    // Assert
    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(dto.getId());
    assertThat(entity.getName()).isEqualTo(dto.getName());
    assertThat(entity.getCode()).isEqualTo(dto.getCode());
    assertThat(entity.getSource()).isEqualTo(dto.getSource());
  }

  @Test
  void toEntity_negative_nullDtoReturnsNull() {
    // Act
    var entity = mapper.toEntity(null);

    // Assert
    assertThat(entity).isNull();
  }

  @Test
  void toDto_positive_mapsMetadata() {
    // Arrange
    var entity = createAuthorityIdentifierType();

    // Act
    var dto = mapper.toDto(entity);

    // Assert
    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(entity.getId());
    assertThat(dto.getName()).isEqualTo(entity.getName());
    assertThat(dto.getCode()).isEqualTo(entity.getCode());
    assertThat(dto.getSource()).isEqualTo(entity.getSource());
    assertThat(dto.getMetadata().getCreatedDate()).isEqualTo(fromTimestamp(entity.getCreatedDate()));
    assertThat(dto.getMetadata().getCreatedByUserId()).isEqualTo(entity.getCreatedByUserId());
    assertThat(dto.getMetadata().getUpdatedDate()).isEqualTo(fromTimestamp(entity.getUpdatedDate()));
    assertThat(dto.getMetadata().getUpdatedByUserId()).isEqualTo(entity.getUpdatedByUserId());
  }

  @Test
  void toDto_negative_nullEntityReturnsNull() {
    // Act
    var dto = mapper.toDto(null);

    // Assert
    assertThat(dto).isNull();
  }

  @Test
  void toDtoList_positive_mapsEntities() {
    // Arrange
    var entity = createAuthorityIdentifierType();

    // Act
    var dtoList = mapper.toDtoList(List.of(entity));

    // Assert
    assertThat(dtoList).hasSize(1);
    var dto = dtoList.getFirst();
    assertThat(dto.getId()).isEqualTo(entity.getId());
    assertThat(dto.getName()).isEqualTo(entity.getName());
    assertThat(dto.getCode()).isEqualTo(entity.getCode());
    assertThat(dto.getSource()).isEqualTo(entity.getSource());
    assertThat(dto.getMetadata().getCreatedDate()).isEqualTo(fromTimestamp(entity.getCreatedDate()));
    assertThat(dto.getMetadata().getCreatedByUserId()).isEqualTo(entity.getCreatedByUserId());
    assertThat(dto.getMetadata().getUpdatedDate()).isEqualTo(fromTimestamp(entity.getUpdatedDate()));
    assertThat(dto.getMetadata().getUpdatedByUserId()).isEqualTo(entity.getUpdatedByUserId());
  }

  @Test
  void toAuthorityIdentifierTypeCollection_positive_mapsPage() {
    // Arrange
    var entity = createAuthorityIdentifierType();

    // Act
    AuthorityIdentifierTypeDtoCollection dtoCollection =
      mapper.toAuthorityIdentifierTypeCollection(new PageImpl<>(List.of(entity)));

    // Assert
    assertThat(dtoCollection).isNotNull();
    assertThat(dtoCollection.getTotalRecords()).isEqualTo(1);
    var dto = dtoCollection.getIdentifierTypes().getFirst();
    assertThat(dto.getId()).isEqualTo(entity.getId());
    assertThat(dto.getName()).isEqualTo(entity.getName());
    assertThat(dto.getCode()).isEqualTo(entity.getCode());
    assertThat(dto.getSource()).isEqualTo(entity.getSource());
    assertThat(dto.getMetadata().getCreatedDate()).isEqualTo(fromTimestamp(entity.getCreatedDate()));
    assertThat(dto.getMetadata().getCreatedByUserId()).isEqualTo(entity.getCreatedByUserId());
    assertThat(dto.getMetadata().getUpdatedDate()).isEqualTo(fromTimestamp(entity.getUpdatedDate()));
    assertThat(dto.getMetadata().getUpdatedByUserId()).isEqualTo(entity.getUpdatedByUserId());
  }

  private static AuthorityIdentifierType createAuthorityIdentifierType() {
    var entity = new AuthorityIdentifierType();
    entity.setId(TEST_ID);
    entity.setName(TEST_PROPERTY_VALUE);
    entity.setCode("test-code");
    entity.setSource(TEST_PROPERTY_VALUE);
    entity.setCreatedDate(TEST_DATE);
    entity.setCreatedByUserId(TEST_ID);
    entity.setUpdatedDate(TEST_DATE);
    entity.setUpdatedByUserId(TEST_ID);
    return entity;
  }
}
