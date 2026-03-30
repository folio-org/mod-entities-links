package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.TEST_ID;
import static org.folio.support.base.TestConstants.TEST_PROPERTY_VALUE;

import java.util.List;
import org.folio.entlinks.domain.dto.AuthorityHeadingTypeDto;
import org.folio.entlinks.domain.dto.AuthorityHeadingTypeDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityHeadingType;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

@UnitTest
class AuthorityHeadingTypeMapperTest {

  private static final String TEST_CODE = "test-code";

  private final AuthorityHeadingTypeMapper mapper = new AuthorityHeadingTypeMapperImpl();

  @Test
  void toEntity_positive_mapsFields() {
    var dto = new AuthorityHeadingTypeDto();
    dto.setId(TEST_ID);
    dto.setName(TEST_PROPERTY_VALUE);
    dto.setCode(TEST_CODE);
    dto.setQueryable(Boolean.TRUE);

    var entity = mapper.toEntity(dto);

    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(dto.getId());
    assertThat(entity.getName()).isEqualTo(dto.getName());
    assertThat(entity.getCode()).isEqualTo(dto.getCode());
    assertThat(entity.getQueryable()).isEqualTo(dto.getQueryable());
    assertThat(entity.isNew()).isTrue();
  }

  @Test
  void toEntity_negative_nullDtoReturnsNull() {
    var entity = mapper.toEntity(null);

    assertThat(entity).isNull();
  }

  @Test
  void toDto_positive_mapsFields() {
    var entity = createAuthorityHeadingType();

    var dto = mapper.toDto(entity);

    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(entity.getId());
    assertThat(dto.getName()).isEqualTo(entity.getName());
    assertThat(dto.getCode()).isEqualTo(entity.getCode());
    assertThat(dto.getQueryable()).isEqualTo(entity.getQueryable());
  }

  @Test
  void toDto_negative_nullEntityReturnsNull() {
    var dto = mapper.toDto(null);

    assertThat(dto).isNull();
  }

  @Test
  void toDtoList_positive_mapsEntities() {
    var entity = createAuthorityHeadingType();

    var dtoList = mapper.toDtoList(List.of(entity));

    assertThat(dtoList).hasSize(1);
    var dto = dtoList.getFirst();
    assertThat(dto.getId()).isEqualTo(entity.getId());
    assertThat(dto.getName()).isEqualTo(entity.getName());
    assertThat(dto.getCode()).isEqualTo(entity.getCode());
    assertThat(dto.getQueryable()).isEqualTo(entity.getQueryable());
  }

  @Test
  void toAuthorityHeadingTypeCollection_positive_mapsPage() {
    var entity = createAuthorityHeadingType();

    AuthorityHeadingTypeDtoCollection dtoCollection =
      mapper.toAuthorityHeadingTypeCollection(new PageImpl<>(List.of(entity)));

    assertThat(dtoCollection).isNotNull();
    assertThat(dtoCollection.getTotalRecords()).isEqualTo(1);
    var dto = dtoCollection.getHeadingTypes().getFirst();
    assertThat(dto.getId()).isEqualTo(entity.getId());
    assertThat(dto.getName()).isEqualTo(entity.getName());
    assertThat(dto.getCode()).isEqualTo(entity.getCode());
    assertThat(dto.getQueryable()).isEqualTo(entity.getQueryable());
  }

  private static AuthorityHeadingType createAuthorityHeadingType() {
    var entity = new AuthorityHeadingType();
    entity.setId(TEST_ID);
    entity.setName(TEST_PROPERTY_VALUE);
    entity.setCode(TEST_CODE);
    entity.setQueryable(Boolean.TRUE);
    return entity;
  }
}
