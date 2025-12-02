package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus.ACTUAL;
import static org.folio.support.base.TestConstants.TEST_ID;
import static org.folio.support.base.TestConstants.TEST_PROPERTY_VALUE;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.spring.testing.type.UnitTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

@UnitTest
class InstanceAuthorityLinkMapperTest {

  private static final Integer TEST_INTEGER_ID = ThreadLocalRandom.current().nextInt();
  private static final Long TEST_LONG_ID = 789L;
  private final InstanceAuthorityLinkMapperImpl mapper = new InstanceAuthorityLinkMapperImpl();

  @Test
  void testConvertToDto_InstanceAuthorityLink() {
    InstanceAuthorityLink source = createInstanceAuthorityLink();

    InstanceLinkDto dto = mapper.convertToDto(source);

    assertThat(dto.getAuthorityId()).isEqualTo(source.getAuthority().getId());
    assertThat(dto.getAuthorityNaturalId()).isEqualTo(source.getAuthority().getNaturalId());
    assertThat(dto.getLinkingRuleId()).isEqualTo(source.getLinkingRule().getId());
    assertThat(dto.getId()).isEqualTo(source.getId().intValue());
    assertThat(dto.getInstanceId()).isEqualTo(source.getInstanceId());
    assertThat(dto.getStatus()).isEqualTo(source.getStatus().name());
    assertThat(dto.getErrorCause()).isEqualTo(source.getErrorCause());
  }

  @Test
  void testConvertDto_InstanceLinkDto() {
    InstanceLinkDto source = createInstanceLinkDto();

    InstanceAuthorityLink link = mapper.convertDto(source);

    assertThat(link.getAuthority().getId()).isEqualTo(source.getAuthorityId());
    assertThat(link.getAuthority().getNaturalId()).isEqualTo(source.getAuthorityNaturalId());
    assertThat(link.getLinkingRule().getId()).isEqualTo(source.getLinkingRuleId());
    assertThat(link.getId().intValue()).isEqualTo(source.getId());
    assertThat(link.getInstanceId()).isEqualTo(source.getInstanceId());
    assertThat(link.getStatus().name()).isEqualTo(source.getStatus());
  }

  @Test
  void testConvertDtoList_InstanceLinkDtoList() {
    var sourceList = List.of(createInstanceLinkDto());

    var linkList = mapper.convertDto(sourceList);

    assertThat(sourceList).hasSize(linkList.size());

    var expected = sourceList.getFirst();
    var actual = linkList.getFirst();
    assertThat(actual.getAuthority().getId()).isEqualTo(expected.getAuthorityId());
    assertThat(actual.getAuthority().getNaturalId()).isEqualTo(expected.getAuthorityNaturalId());
    assertThat(actual.getLinkingRule().getId()).isEqualTo(expected.getLinkingRuleId());
    assertThat(actual.getId().intValue()).isEqualTo(expected.getId());
    assertThat(actual.getInstanceId()).isEqualTo(expected.getInstanceId());
    assertThat(actual.getStatus().name()).isEqualTo(expected.getStatus());
  }

  @NotNull
  private static InstanceLinkDto createInstanceLinkDto() {
    InstanceLinkDto source = new InstanceLinkDto();
    source.setAuthorityId(TEST_ID);
    source.setAuthorityNaturalId(TEST_PROPERTY_VALUE);
    source.setLinkingRuleId(TEST_INTEGER_ID);
    source.setId(TEST_INTEGER_ID);
    source.setInstanceId(TEST_ID);
    source.setStatus(ACTUAL.name());
    return source;
  }

  @NotNull
  private static InstanceAuthorityLink createInstanceAuthorityLink() {
    InstanceAuthorityLink source = new InstanceAuthorityLink();
    source.setAuthority(new Authority());
    source.setLinkingRule(new InstanceAuthorityLinkingRule());
    source.setId(TEST_LONG_ID);
    source.setInstanceId(TEST_ID);
    source.setStatus(ACTUAL);
    source.setErrorCause(TEST_PROPERTY_VALUE);
    return source;
  }
}
