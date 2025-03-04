package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CHRON_SUBDIVISION_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CHRON_SUBDIVISION_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CHRON_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CHRON_TERM_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CORPORATE_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CORPORATE_NAME_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CORPORATE_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CORPORATE_NAME_TITLE_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.FORM_SUBDIVISION_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.FORM_SUBDIVISION_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GENERAL_SUBDIVISION_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GENERAL_SUBDIVISION_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GENRE_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GENRE_TERM_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GEOGRAPHIC_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GEOGRAPHIC_NAME_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GEOGRAPHIC_SUBDIVISION_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GEOGRAPHIC_SUBDIVISION_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEDIUM_PERF_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEDIUM_PERF_TERM_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEETING_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEETING_NAME_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEETING_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEETING_NAME_TITLE_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.NAMED_EVENT_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.NAMED_EVENT_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.PERSONAL_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.PERSONAL_NAME_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.PERSONAL_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.PERSONAL_NAME_TITLE_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.TOPICAL_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.TOPICAL_TERM_HEADING_TRUNC;
import static org.folio.entlinks.domain.entity.AuthorityConstants.UNIFORM_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.UNIFORM_TITLE_HEADING_TRUNC;
import static org.folio.support.base.TestConstants.TEST_STRING;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.RelatedHeading;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.HeadingRef;
import org.folio.entlinks.domain.entity.RelationshipType;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class AuthorityUtilityMapperTest {

  private final AuthorityDto source = new AuthorityDto();
  private final Authority target = new Authority();

  @ParameterizedTest
  @MethodSource("headingTypeAndValueProvider")
  void testExtractAuthorityHeadingWithNonNullValues(String propertyType, String propertyValue) {
    switch (propertyType) {
      case PERSONAL_NAME_HEADING -> source.setPersonalName(propertyValue);
      case PERSONAL_NAME_TITLE_HEADING -> source.setPersonalNameTitle(propertyValue);
      case CORPORATE_NAME_HEADING -> source.setCorporateName(propertyValue);
      case CORPORATE_NAME_TITLE_HEADING -> source.setCorporateNameTitle(propertyValue);
      case MEETING_NAME_HEADING -> source.setMeetingName(propertyValue);
      case MEETING_NAME_TITLE_HEADING -> source.setMeetingNameTitle(propertyValue);
      case UNIFORM_TITLE_HEADING -> source.setUniformTitle(propertyValue);
      case NAMED_EVENT_HEADING -> source.setNamedEvent(propertyValue);
      case TOPICAL_TERM_HEADING -> source.setTopicalTerm(propertyValue);
      case GEOGRAPHIC_NAME_HEADING -> source.setGeographicName(propertyValue);
      case GENRE_TERM_HEADING -> source.setGenreTerm(propertyValue);
      case CHRON_TERM_HEADING -> source.setChronTerm(propertyValue);
      case MEDIUM_PERF_TERM_HEADING -> source.setMediumPerfTerm(propertyValue);
      case GENERAL_SUBDIVISION_HEADING -> source.setGeneralSubdivision(propertyValue);
      case GEOGRAPHIC_SUBDIVISION_HEADING -> source.setGeographicSubdivision(propertyValue);
      case CHRON_SUBDIVISION_HEADING -> source.setChronSubdivision(propertyValue);
      case FORM_SUBDIVISION_HEADING -> source.setFormSubdivision(propertyValue);
      default -> fail("Invalid heading type - {} cannot be mapped", propertyType);
    }

    AuthorityUtilityMapper.extractAuthorityHeading(source, target);

    assertThat(propertyValue).isEqualTo(target.getHeading());
    assertThat(propertyType).isEqualTo(target.getHeadingType());
  }

  @ParameterizedTest
  @MethodSource("headingTypeAndValueProvider")
  void testExtractAuthoritySftHeadingsWithNonNullValues(String propertyType, String propertyValue) {
    switch (propertyType) {
      case PERSONAL_NAME_HEADING -> source.setSftPersonalName(Collections.singletonList(propertyValue));
      case PERSONAL_NAME_TITLE_HEADING -> source.setSftPersonalNameTitle(Collections.singletonList(propertyValue));
      case CORPORATE_NAME_HEADING -> source.setSftCorporateName(Collections.singletonList(propertyValue));
      case CORPORATE_NAME_TITLE_HEADING -> source.setSftCorporateNameTitle(Collections.singletonList(propertyValue));
      case MEETING_NAME_HEADING -> source.setSftMeetingName(Collections.singletonList(propertyValue));
      case MEETING_NAME_TITLE_HEADING -> source.setSftMeetingNameTitle(Collections.singletonList(propertyValue));
      case UNIFORM_TITLE_HEADING -> source.setSftUniformTitle(Collections.singletonList(propertyValue));
      case NAMED_EVENT_HEADING -> source.setSftNamedEvent(Collections.singletonList(propertyValue));
      case TOPICAL_TERM_HEADING -> source.setSftTopicalTerm(Collections.singletonList(propertyValue));
      case GEOGRAPHIC_NAME_HEADING -> source.setSftGeographicName(Collections.singletonList(propertyValue));
      case GENRE_TERM_HEADING -> source.setSftGenreTerm(Collections.singletonList(propertyValue));
      case CHRON_TERM_HEADING -> source.setSftChronTerm(Collections.singletonList(propertyValue));
      case MEDIUM_PERF_TERM_HEADING -> source.setSftMediumPerfTerm(Collections.singletonList(propertyValue));
      case GENERAL_SUBDIVISION_HEADING -> source.setSftGeneralSubdivision(Collections.singletonList(propertyValue));
      case GEOGRAPHIC_SUBDIVISION_HEADING ->
        source.setSftGeographicSubdivision(Collections.singletonList(propertyValue));
      case CHRON_SUBDIVISION_HEADING -> source.setSftChronSubdivision(Collections.singletonList(propertyValue));
      case FORM_SUBDIVISION_HEADING -> source.setSftFormSubdivision(Collections.singletonList(propertyValue));
      default -> fail("Invalid sft heading type - {} cannot be mapped", propertyType);
    }

    AuthorityUtilityMapper.extractAuthoritySftHeadings(source, target);

    var sftHeadings = target.getSftHeadings();
    assertThat(sftHeadings).hasSize(1);
    assertThat(propertyValue).isEqualTo(sftHeadings.get(0).getHeading());
    assertThat(propertyType).isEqualTo(sftHeadings.get(0).getHeadingType());
  }

  @ParameterizedTest
  @MethodSource("saftHeadingTypeAndValueProvider")
  void testExtractAuthoritySaftHeadingsWithNonNullValues(String propertyType, String propertyValue) {
    switch (propertyType) {
      case PERSONAL_NAME_HEADING -> source.setSaftPersonalName(Collections.singletonList(propertyValue));
      case PERSONAL_NAME_TITLE_HEADING -> source.setSaftPersonalNameTitle(Collections.singletonList(propertyValue));
      case CORPORATE_NAME_HEADING -> source.setSaftCorporateName(Collections.singletonList(propertyValue));
      case CORPORATE_NAME_TITLE_HEADING -> source.setSaftCorporateNameTitle(Collections.singletonList(propertyValue));
      case MEETING_NAME_HEADING -> source.setSaftMeetingName(Collections.singletonList(propertyValue));
      case MEETING_NAME_TITLE_HEADING -> source.setSaftMeetingNameTitle(Collections.singletonList(propertyValue));
      case UNIFORM_TITLE_HEADING -> source.setSaftUniformTitle(Collections.singletonList(propertyValue));
      case NAMED_EVENT_HEADING -> source.setSaftNamedEvent(Collections.singletonList(propertyValue));
      case TOPICAL_TERM_HEADING -> source.setSaftTopicalTerm(Collections.singletonList(propertyValue));
      case GEOGRAPHIC_NAME_HEADING -> source.setSaftGeographicName(Collections.singletonList(propertyValue));
      case GENRE_TERM_HEADING -> source.setSaftGenreTerm(Collections.singletonList(propertyValue));
      case CHRON_TERM_HEADING -> source.setSaftChronTerm(Collections.singletonList(propertyValue));
      case MEDIUM_PERF_TERM_HEADING -> source.setSaftMediumPerfTerm(Collections.singletonList(propertyValue));
      case GENERAL_SUBDIVISION_HEADING -> source.setSaftGeneralSubdivision(Collections.singletonList(propertyValue));
      case GEOGRAPHIC_SUBDIVISION_HEADING ->
        source.setSaftGeographicSubdivision(Collections.singletonList(propertyValue));
      case CHRON_SUBDIVISION_HEADING -> source.setSaftChronSubdivision(Collections.singletonList(propertyValue));
      case FORM_SUBDIVISION_HEADING -> source.setSaftFormSubdivision(Collections.singletonList(propertyValue));
      case PERSONAL_NAME_HEADING_TRUNC -> source.setSaftPersonalNameTrunc(Collections.singletonList(propertyValue));
      case PERSONAL_NAME_TITLE_HEADING_TRUNC ->
        source.setSaftPersonalNameTitleTrunc(Collections.singletonList(propertyValue));
      case CORPORATE_NAME_HEADING_TRUNC -> source.setSaftCorporateNameTrunc(Collections.singletonList(propertyValue));
      case CORPORATE_NAME_TITLE_HEADING_TRUNC ->
        source.setSaftCorporateNameTitleTrunc(Collections.singletonList(propertyValue));
      case MEETING_NAME_HEADING_TRUNC -> source.setSaftMeetingNameTrunc(Collections.singletonList(propertyValue));
      case MEETING_NAME_TITLE_HEADING_TRUNC ->
        source.setSaftMeetingNameTitleTrunc(Collections.singletonList(propertyValue));
      case UNIFORM_TITLE_HEADING_TRUNC -> source.setSaftUniformTitleTrunc(Collections.singletonList(propertyValue));
      case TOPICAL_TERM_HEADING_TRUNC -> source.setSaftTopicalTermTrunc(Collections.singletonList(propertyValue));
      case GEOGRAPHIC_NAME_HEADING_TRUNC -> source.setSaftGeographicNameTrunc(Collections.singletonList(propertyValue));
      case GENRE_TERM_HEADING_TRUNC -> source.setSaftGenreTermTrunc(Collections.singletonList(propertyValue));
      case NAMED_EVENT_HEADING_TRUNC -> source.setSaftNamedEventTrunc(Collections.singletonList(propertyValue));
      case CHRON_TERM_HEADING_TRUNC -> source.setSaftChronTermTrunc(Collections.singletonList(propertyValue));
      case MEDIUM_PERF_TERM_HEADING_TRUNC ->
        source.setSaftMediumPerfTermTrunc(Collections.singletonList(propertyValue));
      case GENERAL_SUBDIVISION_HEADING_TRUNC ->
        source.setSaftGeneralSubdivisionTrunc(Collections.singletonList(propertyValue));
      case GEOGRAPHIC_SUBDIVISION_HEADING_TRUNC ->
        source.setSaftGeographicSubdivisionTrunc(Collections.singletonList(propertyValue));
      case CHRON_SUBDIVISION_HEADING_TRUNC ->
        source.setSaftChronSubdivisionTrunc(Collections.singletonList(propertyValue));
      case FORM_SUBDIVISION_HEADING_TRUNC ->
        source.setSaftFormSubdivisionTrunc(Collections.singletonList(propertyValue));
      default -> fail("Invalid saft heading type - {} cannot be mapped", propertyType);
    }

    AuthorityUtilityMapper.extractAuthoritySaftHeadings(source, target);

    var saftHeadings = target.getSaftHeadings();
    assertThat(saftHeadings).hasSize(1);
    assertThat(propertyValue).isEqualTo(saftHeadings.getFirst().getHeading());
    assertThat(propertyType).isEqualTo(saftHeadings.getFirst().getHeadingType());
  }

  @ParameterizedTest
  @MethodSource("headingTypeAndValueProvider")
  void testExtractAuthorityDtoSftHeadings(String headingType, String headingValue) {

    var sftHeadings = new ArrayList<HeadingRef>();
    sftHeadings.add(new HeadingRef(headingType, headingValue));
    target.setSftHeadings(sftHeadings);

    AuthorityUtilityMapper.extractAuthorityDtoSftHeadings(target, source);

    switch (headingType) {
      case PERSONAL_NAME_HEADING -> assertTrue(source.getSftPersonalName().contains(headingValue));
      case PERSONAL_NAME_TITLE_HEADING -> assertTrue(source.getSftPersonalNameTitle().contains(headingValue));
      case CORPORATE_NAME_HEADING -> assertTrue(source.getSftCorporateName().contains(headingValue));
      case CORPORATE_NAME_TITLE_HEADING -> assertTrue(source.getSftCorporateNameTitle().contains(headingValue));
      case MEETING_NAME_HEADING -> assertTrue(source.getSftMeetingName().contains(headingValue));
      case MEETING_NAME_TITLE_HEADING -> assertTrue(source.getSftMeetingNameTitle().contains(headingValue));
      case UNIFORM_TITLE_HEADING -> assertTrue(source.getSftUniformTitle().contains(headingValue));
      case NAMED_EVENT_HEADING -> assertTrue(source.getSftNamedEvent().contains(headingValue));
      case TOPICAL_TERM_HEADING -> assertTrue(source.getSftTopicalTerm().contains(headingValue));
      case GEOGRAPHIC_NAME_HEADING -> assertTrue(source.getSftGeographicName().contains(headingValue));
      case GENRE_TERM_HEADING -> assertTrue(source.getSftGenreTerm().contains(headingValue));
      case CHRON_TERM_HEADING -> assertTrue(source.getSftChronTerm().contains(headingValue));
      case MEDIUM_PERF_TERM_HEADING -> assertTrue(source.getSftMediumPerfTerm().contains(headingValue));
      case GENERAL_SUBDIVISION_HEADING -> assertTrue(source.getSftGeneralSubdivision().contains(headingValue));
      case GEOGRAPHIC_SUBDIVISION_HEADING -> assertTrue(source.getSftGeographicSubdivision().contains(headingValue));
      case CHRON_SUBDIVISION_HEADING -> assertTrue(source.getSftChronSubdivision().contains(headingValue));
      case FORM_SUBDIVISION_HEADING -> assertTrue(source.getSftFormSubdivision().contains(headingValue));
      default -> fail("Invalid sft heading type - {} cannot be mapped", headingType);
    }
  }

  @ParameterizedTest
  @MethodSource("saftHeadingTypeAndValueProvider")
  void testExtractAuthorityDtoSaftHeadings(String headingType, String headingValue) {

    var saftHeadings = new ArrayList<HeadingRef>();
    saftHeadings.add(new HeadingRef(headingType, headingValue));
    target.setSaftHeadings(saftHeadings);

    AuthorityUtilityMapper.extractAuthorityDtoSaftHeadings(target, source);

    switch (headingType) {
      case PERSONAL_NAME_HEADING -> assertTrue(source.getSaftPersonalName().contains(headingValue));
      case PERSONAL_NAME_TITLE_HEADING -> assertTrue(source.getSaftPersonalNameTitle().contains(headingValue));
      case CORPORATE_NAME_HEADING -> assertTrue(source.getSaftCorporateName().contains(headingValue));
      case CORPORATE_NAME_TITLE_HEADING -> assertTrue(source.getSaftCorporateNameTitle().contains(headingValue));
      case MEETING_NAME_HEADING -> assertTrue(source.getSaftMeetingName().contains(headingValue));
      case MEETING_NAME_TITLE_HEADING -> assertTrue(source.getSaftMeetingNameTitle().contains(headingValue));
      case UNIFORM_TITLE_HEADING -> assertTrue(source.getSaftUniformTitle().contains(headingValue));
      case NAMED_EVENT_HEADING -> assertTrue(source.getSaftNamedEvent().contains(headingValue));
      case TOPICAL_TERM_HEADING -> assertTrue(source.getSaftTopicalTerm().contains(headingValue));
      case GEOGRAPHIC_NAME_HEADING -> assertTrue(source.getSaftGeographicName().contains(headingValue));
      case GENRE_TERM_HEADING -> assertTrue(source.getSaftGenreTerm().contains(headingValue));
      case CHRON_TERM_HEADING -> assertTrue(source.getSaftChronTerm().contains(headingValue));
      case MEDIUM_PERF_TERM_HEADING -> assertTrue(source.getSaftMediumPerfTerm().contains(headingValue));
      case GENERAL_SUBDIVISION_HEADING -> assertTrue(source.getSaftGeneralSubdivision().contains(headingValue));
      case GEOGRAPHIC_SUBDIVISION_HEADING -> assertTrue(source.getSaftGeographicSubdivision().contains(headingValue));
      case CHRON_SUBDIVISION_HEADING -> assertTrue(source.getSaftChronSubdivision().contains(headingValue));
      case FORM_SUBDIVISION_HEADING -> assertTrue(source.getSaftFormSubdivision().contains(headingValue));
      case PERSONAL_NAME_HEADING_TRUNC -> assertTrue(source.getSaftPersonalNameTrunc().contains(headingValue));
      case PERSONAL_NAME_TITLE_HEADING_TRUNC ->
        assertTrue(source.getSaftPersonalNameTitleTrunc().contains(headingValue));
      case CORPORATE_NAME_HEADING_TRUNC -> assertTrue(source.getSaftCorporateNameTrunc().contains(headingValue));
      case CORPORATE_NAME_TITLE_HEADING_TRUNC ->
        assertTrue(source.getSaftCorporateNameTitleTrunc().contains(headingValue));
      case MEETING_NAME_HEADING_TRUNC -> assertTrue(source.getSaftMeetingNameTrunc().contains(headingValue));
      case MEETING_NAME_TITLE_HEADING_TRUNC -> assertTrue(source.getSaftMeetingNameTitleTrunc().contains(headingValue));
      case UNIFORM_TITLE_HEADING_TRUNC -> assertTrue(source.getSaftUniformTitleTrunc().contains(headingValue));
      case TOPICAL_TERM_HEADING_TRUNC -> assertTrue(source.getSaftTopicalTermTrunc().contains(headingValue));
      case GEOGRAPHIC_NAME_HEADING_TRUNC -> assertTrue(source.getSaftGeographicNameTrunc().contains(headingValue));
      case GENRE_TERM_HEADING_TRUNC -> assertTrue(source.getSaftGenreTermTrunc().contains(headingValue));
      case NAMED_EVENT_HEADING_TRUNC -> assertTrue(source.getSaftNamedEventTrunc().contains(headingValue));
      case CHRON_TERM_HEADING_TRUNC -> assertTrue(source.getSaftChronTermTrunc().contains(headingValue));
      case MEDIUM_PERF_TERM_HEADING_TRUNC -> assertTrue(source.getSaftMediumPerfTermTrunc().contains(headingValue));
      case GENERAL_SUBDIVISION_HEADING_TRUNC ->
        assertTrue(source.getSaftGeneralSubdivisionTrunc().contains(headingValue));
      case GEOGRAPHIC_SUBDIVISION_HEADING_TRUNC ->
        assertTrue(source.getSaftGeographicSubdivisionTrunc().contains(headingValue));
      case CHRON_SUBDIVISION_HEADING_TRUNC -> assertTrue(source.getSaftChronSubdivisionTrunc().contains(headingValue));
      case FORM_SUBDIVISION_HEADING_TRUNC -> assertTrue(source.getSaftFormSubdivisionTrunc().contains(headingValue));
      default -> fail("Invalid saft heading type - {} cannot be mapped", headingType);
    }
  }

  @ParameterizedTest
  @MethodSource("headingTypeAndValueProvider")
  void testExtractAuthorityDtoHeadingValue(String headingType, String headingValue) {
    target.setHeading(headingValue);
    target.setHeadingType(headingType);

    AuthorityUtilityMapper.extractAuthorityDtoHeadingValue(target, source);

    switch (headingType) {
      case PERSONAL_NAME_HEADING -> assertThat(source.getPersonalName()).isEqualTo(headingValue);
      case PERSONAL_NAME_TITLE_HEADING -> assertThat(source.getPersonalNameTitle()).isEqualTo(headingValue);
      case CORPORATE_NAME_HEADING -> assertThat(source.getCorporateName()).isEqualTo(headingValue);
      case CORPORATE_NAME_TITLE_HEADING -> assertThat(source.getCorporateNameTitle()).isEqualTo(headingValue);
      case MEETING_NAME_HEADING -> assertThat(source.getMeetingName()).isEqualTo(headingValue);
      case MEETING_NAME_TITLE_HEADING -> assertThat(source.getMeetingNameTitle()).isEqualTo(headingValue);
      case UNIFORM_TITLE_HEADING -> assertThat(source.getUniformTitle()).isEqualTo(headingValue);
      case NAMED_EVENT_HEADING -> assertThat(source.getNamedEvent()).isEqualTo(headingValue);
      case TOPICAL_TERM_HEADING -> assertThat(source.getTopicalTerm()).isEqualTo(headingValue);
      case GEOGRAPHIC_NAME_HEADING -> assertThat(source.getGeographicName()).isEqualTo(headingValue);
      case GENRE_TERM_HEADING -> assertThat(source.getGenreTerm()).isEqualTo(headingValue);
      case CHRON_TERM_HEADING -> assertThat(source.getChronTerm()).isEqualTo(headingValue);
      case MEDIUM_PERF_TERM_HEADING -> assertThat(source.getMediumPerfTerm()).isEqualTo(headingValue);
      case GENERAL_SUBDIVISION_HEADING -> assertThat(source.getGeneralSubdivision()).isEqualTo(headingValue);
      case GEOGRAPHIC_SUBDIVISION_HEADING -> assertThat(source.getGeographicSubdivision()).isEqualTo(headingValue);
      case CHRON_SUBDIVISION_HEADING -> assertThat(source.getChronSubdivision()).isEqualTo(headingValue);
      case FORM_SUBDIVISION_HEADING -> assertThat(source.getFormSubdivision()).isEqualTo(headingValue);
      default -> fail("Invalid heading type - {} cannot be mapped", headingType);
    }
  }

  @Test
  void testExtractAuthoritySaftHeadingsWithRelationships() {
    final var authorityDto = getAuthorityDtoWithSaftTerms();
    final var expectedHeadingRefs = getHeadingRefs();

    AuthorityUtilityMapper.extractAuthoritySaftHeadings(authorityDto, target);

    var saftHeadings = target.getSaftHeadings();
    assertThat(saftHeadings).hasSize(11);
    assertArrayEquals(expectedHeadingRefs.toArray(), saftHeadings.toArray());
  }

  @Test
  void testExtractAuthorityDtoSaftHeadingsWithRelationships() {
    final var saftHeadings = getHeadingRefs();
    target.setSaftHeadings(saftHeadings);
    final var expectedAuthorityDto = getAuthorityDtoWithSaftTerms();

    AuthorityUtilityMapper.extractAuthorityDtoSaftHeadings(target, source);

    assertEquals(expectedAuthorityDto, source);
  }

  private static Stream<Arguments> headingTypeAndValueProvider() {
    return Stream.of(
      arguments(PERSONAL_NAME_HEADING, TEST_STRING),
      arguments(PERSONAL_NAME_TITLE_HEADING, TEST_STRING),
      arguments(CORPORATE_NAME_HEADING, TEST_STRING),
      arguments(CORPORATE_NAME_TITLE_HEADING, TEST_STRING),
      arguments(MEETING_NAME_HEADING, TEST_STRING),
      arguments(MEETING_NAME_TITLE_HEADING, TEST_STRING),
      arguments(UNIFORM_TITLE_HEADING, TEST_STRING),
      arguments(NAMED_EVENT_HEADING, TEST_STRING),
      arguments(TOPICAL_TERM_HEADING, TEST_STRING),
      arguments(GEOGRAPHIC_NAME_HEADING, TEST_STRING),
      arguments(GENRE_TERM_HEADING, TEST_STRING),
      arguments(CHRON_TERM_HEADING, TEST_STRING),
      arguments(MEDIUM_PERF_TERM_HEADING, TEST_STRING),
      arguments(GENERAL_SUBDIVISION_HEADING, TEST_STRING),
      arguments(GEOGRAPHIC_SUBDIVISION_HEADING, TEST_STRING),
      arguments(CHRON_SUBDIVISION_HEADING, TEST_STRING),
      arguments(FORM_SUBDIVISION_HEADING, TEST_STRING)
    );
  }

  private static Stream<Arguments> saftHeadingTypeAndValueProvider() {
    return Stream.of(
      arguments(PERSONAL_NAME_HEADING, TEST_STRING),
      arguments(PERSONAL_NAME_TITLE_HEADING, TEST_STRING),
      arguments(CORPORATE_NAME_HEADING, TEST_STRING),
      arguments(CORPORATE_NAME_TITLE_HEADING, TEST_STRING),
      arguments(MEETING_NAME_HEADING, TEST_STRING),
      arguments(MEETING_NAME_TITLE_HEADING, TEST_STRING),
      arguments(UNIFORM_TITLE_HEADING, TEST_STRING),
      arguments(NAMED_EVENT_HEADING, TEST_STRING),
      arguments(TOPICAL_TERM_HEADING, TEST_STRING),
      arguments(GEOGRAPHIC_NAME_HEADING, TEST_STRING),
      arguments(GENRE_TERM_HEADING, TEST_STRING),
      arguments(PERSONAL_NAME_HEADING_TRUNC, TEST_STRING),
      arguments(PERSONAL_NAME_TITLE_HEADING_TRUNC, TEST_STRING),
      arguments(CORPORATE_NAME_HEADING_TRUNC, TEST_STRING),
      arguments(CORPORATE_NAME_TITLE_HEADING_TRUNC, TEST_STRING),
      arguments(MEETING_NAME_HEADING_TRUNC, TEST_STRING),
      arguments(MEETING_NAME_TITLE_HEADING_TRUNC, TEST_STRING),
      arguments(UNIFORM_TITLE_HEADING_TRUNC, TEST_STRING),
      arguments(TOPICAL_TERM_HEADING_TRUNC, TEST_STRING),
      arguments(GEOGRAPHIC_NAME_HEADING_TRUNC, TEST_STRING),
      arguments(GENRE_TERM_HEADING_TRUNC, TEST_STRING),
      arguments(CHRON_TERM_HEADING_TRUNC, TEST_STRING),
      arguments(MEDIUM_PERF_TERM_HEADING_TRUNC, TEST_STRING),
      arguments(GENERAL_SUBDIVISION_HEADING_TRUNC, TEST_STRING),
      arguments(GEOGRAPHIC_SUBDIVISION_HEADING_TRUNC, TEST_STRING),
      arguments(CHRON_SUBDIVISION_HEADING_TRUNC, TEST_STRING),
      arguments(FORM_SUBDIVISION_HEADING_TRUNC, TEST_STRING)
    );
  }

  private static List<HeadingRef> getHeadingRefs() {
    return List.of(
      new HeadingRef(PERSONAL_NAME_HEADING_TRUNC, PERSONAL_NAME_HEADING_TRUNC),
      new HeadingRef(PERSONAL_NAME_HEADING_TRUNC, "broaderTerm1", Set.of(RelationshipType.BROADER_TERM)),
      new HeadingRef(CORPORATE_NAME_HEADING_TRUNC, CORPORATE_NAME_HEADING_TRUNC),
      new HeadingRef(CORPORATE_NAME_HEADING_TRUNC, "broaderTerm2", Set.of(RelationshipType.BROADER_TERM)),
      new HeadingRef(CORPORATE_NAME_HEADING_TRUNC, "laterHeading", Set.of(RelationshipType.LATER_HEADING)),
      new HeadingRef(MEETING_NAME_HEADING_TRUNC, MEETING_NAME_HEADING_TRUNC),
      new HeadingRef(MEETING_NAME_HEADING_TRUNC, "narrowerTerm", Set.of(RelationshipType.NARROWER_TERM)),
      new HeadingRef(MEETING_NAME_HEADING_TRUNC, "narrower-later", Set.of(RelationshipType.NARROWER_TERM,
        RelationshipType.LATER_HEADING)),
      new HeadingRef(TOPICAL_TERM_HEADING_TRUNC, TOPICAL_TERM_HEADING_TRUNC),
      new HeadingRef(TOPICAL_TERM_HEADING_TRUNC, "broaderTerm1"),
      new HeadingRef(TOPICAL_TERM_HEADING_TRUNC, "earlierHeading", Set.of(RelationshipType.EARLIER_HEADING)));
  }

  private static AuthorityDto getAuthorityDtoWithSaftTerms() {
    AuthorityDto authorityDto = new AuthorityDto();
    authorityDto.setSaftBroaderTerm(List.of(
      new RelatedHeading("broaderTerm1", PERSONAL_NAME_HEADING_TRUNC),
      new RelatedHeading("broaderTerm2", CORPORATE_NAME_HEADING_TRUNC)));
    authorityDto.setSaftNarrowerTerm(List.of(
      new RelatedHeading("narrowerTerm", MEETING_NAME_HEADING_TRUNC),
      new RelatedHeading("narrower-later", MEETING_NAME_HEADING_TRUNC)));
    authorityDto.setSaftEarlierHeading(List.of(
      new RelatedHeading("earlierHeading", TOPICAL_TERM_HEADING_TRUNC)));
    authorityDto.setSaftLaterHeading(List.of(
      new RelatedHeading("laterHeading", CORPORATE_NAME_HEADING_TRUNC),
      new RelatedHeading("narrower-later", MEETING_NAME_HEADING_TRUNC)));
    authorityDto.setSaftPersonalNameTrunc(List.of(PERSONAL_NAME_HEADING_TRUNC, "broaderTerm1"));
    authorityDto.setSaftCorporateNameTrunc(List.of(CORPORATE_NAME_HEADING_TRUNC, "broaderTerm2", "laterHeading"));
    authorityDto.setSaftMeetingNameTrunc(List.of(MEETING_NAME_HEADING_TRUNC, "narrowerTerm", "narrower-later"));
    authorityDto.setSaftTopicalTermTrunc(List.of(TOPICAL_TERM_HEADING_TRUNC, "broaderTerm1", "earlierHeading"));
    return authorityDto;
  }
}
