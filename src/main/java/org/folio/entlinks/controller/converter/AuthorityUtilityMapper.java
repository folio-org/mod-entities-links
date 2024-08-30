package org.folio.entlinks.controller.converter;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CORPORATE_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.CORPORATE_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GENRE_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.GEOGRAPHIC_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEETING_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.MEETING_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.PERSONAL_NAME_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.PERSONAL_NAME_TITLE_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.TOPICAL_TERM_HEADING;
import static org.folio.entlinks.domain.entity.AuthorityConstants.UNIFORM_TITLE_HEADING;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.AuthorityBase;
import org.folio.entlinks.domain.entity.HeadingRef;
import org.folio.entlinks.domain.entity.RelationshipType;

@UtilityClass
@Log4j2
public class AuthorityUtilityMapper {

  public static void extractAuthorityHeading(AuthorityDto source, AuthorityBase target) {
    if (Objects.nonNull(source.getPersonalName())) {
      target.setHeading(source.getPersonalName());
      target.setHeadingType(PERSONAL_NAME_HEADING);
      return;
    }
    if (Objects.nonNull(source.getPersonalNameTitle())) {
      target.setHeading(source.getPersonalNameTitle());
      target.setHeadingType(PERSONAL_NAME_TITLE_HEADING);
      return;
    }
    if (Objects.nonNull(source.getCorporateName())) {
      target.setHeading(source.getCorporateName());
      target.setHeadingType(CORPORATE_NAME_HEADING);
      return;
    }
    if (Objects.nonNull(source.getCorporateNameTitle())) {
      target.setHeading(source.getCorporateNameTitle());
      target.setHeadingType(CORPORATE_NAME_TITLE_HEADING);
      return;
    }
    if (Objects.nonNull(source.getMeetingName())) {
      target.setHeading(source.getMeetingName());
      target.setHeadingType(MEETING_NAME_HEADING);
      return;
    }
    if (Objects.nonNull(source.getMeetingNameTitle())) {
      target.setHeading(source.getMeetingNameTitle());
      target.setHeadingType(MEETING_NAME_TITLE_HEADING);
      return;
    }
    if (Objects.nonNull(source.getUniformTitle())) {
      target.setHeading(source.getUniformTitle());
      target.setHeadingType(UNIFORM_TITLE_HEADING);
      return;
    }
    if (Objects.nonNull(source.getTopicalTerm())) {
      target.setHeading(source.getTopicalTerm());
      target.setHeadingType(TOPICAL_TERM_HEADING);
      return;
    }
    if (Objects.nonNull(source.getGeographicName())) {
      target.setHeading(source.getGeographicName());
      target.setHeadingType(GEOGRAPHIC_NAME_HEADING);
      return;
    }
    if (Objects.nonNull(source.getGenreTerm())) {
      target.setHeading(source.getGenreTerm());
      target.setHeadingType(GENRE_TERM_HEADING);
    }
  }

  public static void extractAuthoritySftHeadings(AuthorityDto source, AuthorityBase target) {
    List<HeadingRef> sftHeadings = new ArrayList<>();
    if (isNotEmpty(source.getSftPersonalName())) {
      sftHeadings.addAll(asSftHeadings(source.getSftPersonalName(), PERSONAL_NAME_HEADING));
    }
    if (isNotEmpty(source.getSftPersonalNameTitle())) {
      sftHeadings.addAll(asSftHeadings(source.getSftPersonalNameTitle(), PERSONAL_NAME_TITLE_HEADING));
    }
    if (isNotEmpty(source.getSftCorporateName())) {
      sftHeadings.addAll(asSftHeadings(source.getSftCorporateName(), CORPORATE_NAME_HEADING));
    }
    if (isNotEmpty(source.getSftCorporateNameTitle())) {
      sftHeadings.addAll(asSftHeadings(source.getSftCorporateNameTitle(), CORPORATE_NAME_TITLE_HEADING));
    }
    if (isNotEmpty(source.getSftMeetingName())) {
      sftHeadings.addAll(asSftHeadings(source.getSftMeetingName(), MEETING_NAME_HEADING));
    }
    if (isNotEmpty(source.getSftMeetingNameTitle())) {
      sftHeadings.addAll(asSftHeadings(source.getSftMeetingNameTitle(), MEETING_NAME_TITLE_HEADING));
    }
    if (isNotEmpty(source.getSftUniformTitle())) {
      sftHeadings.addAll(asSftHeadings(source.getSftUniformTitle(), UNIFORM_TITLE_HEADING));
    }
    if (isNotEmpty(source.getSftTopicalTerm())) {
      sftHeadings.addAll(asSftHeadings(source.getSftTopicalTerm(), TOPICAL_TERM_HEADING));
    }
    if (isNotEmpty(source.getSftGeographicName())) {
      sftHeadings.addAll(asSftHeadings(source.getSftGeographicName(), GEOGRAPHIC_NAME_HEADING));
    }
    if (isNotEmpty(source.getSftGenreTerm())) {
      sftHeadings.addAll(asSftHeadings(source.getSftGenreTerm(), GENRE_TERM_HEADING));
    }
    addRelationshipsToSftHeadings(source, sftHeadings);
    target.setSftHeadings(sftHeadings);
  }

  public static void extractAuthoritySaftHeadings(AuthorityDto source, AuthorityBase target) {
    List<HeadingRef> saftHeadings = new ArrayList<>();
    if (isNotEmpty(source.getSaftPersonalName())) {
      saftHeadings.addAll(asSftHeadings(source.getSaftPersonalName(), PERSONAL_NAME_HEADING));
    }
    if (isNotEmpty(source.getSaftPersonalNameTitle())) {
      saftHeadings.addAll(asSftHeadings(source.getSaftPersonalNameTitle(), PERSONAL_NAME_TITLE_HEADING));
    }
    if (isNotEmpty(source.getSaftCorporateName())) {
      saftHeadings.addAll(asSftHeadings(source.getSaftCorporateName(), CORPORATE_NAME_HEADING));
    }
    if (isNotEmpty(source.getSaftCorporateNameTitle())) {
      saftHeadings.addAll(asSftHeadings(source.getSaftCorporateNameTitle(), CORPORATE_NAME_TITLE_HEADING));
    }
    if (isNotEmpty(source.getSaftMeetingName())) {
      saftHeadings.addAll(asSftHeadings(source.getSaftMeetingName(), MEETING_NAME_HEADING));
    }
    if (isNotEmpty(source.getSaftMeetingNameTitle())) {
      saftHeadings.addAll(asSftHeadings(source.getSaftMeetingNameTitle(), MEETING_NAME_TITLE_HEADING));
    }
    if (isNotEmpty(source.getSaftUniformTitle())) {
      saftHeadings.addAll(asSftHeadings(source.getSaftUniformTitle(), UNIFORM_TITLE_HEADING));
    }
    if (isNotEmpty(source.getSaftTopicalTerm())) {
      saftHeadings.addAll(asSftHeadings(source.getSaftTopicalTerm(), TOPICAL_TERM_HEADING));
    }
    if (isNotEmpty(source.getSaftGeographicName())) {
      saftHeadings.addAll(asSftHeadings(source.getSaftGeographicName(), GEOGRAPHIC_NAME_HEADING));
    }
    if (isNotEmpty(source.getSaftGenreTerm())) {
      saftHeadings.addAll(asSftHeadings(source.getSaftGenreTerm(), GENRE_TERM_HEADING));
    }
    addRelationshipsToSaftHeadings(source, saftHeadings);
    target.setSaftHeadings(saftHeadings);
  }

  private static void addRelationshipsToSftHeadings(final AuthorityDto source, final List<HeadingRef> headingRefs) {
    processRelationshipHeadings(source.getSftBroaderTerm(), headingRefs, RelationshipType.BROADER_TERM);
    processRelationshipHeadings(source.getSftNarrowerTerm(), headingRefs, RelationshipType.NARROWER_TERM);
    processRelationshipHeadings(source.getSftEarlierHeading(), headingRefs, RelationshipType.EARLIER_HEADING);
    processRelationshipHeadings(source.getSftLaterHeading(), headingRefs, RelationshipType.LATER_HEADING);
  }

  private static void addRelationshipsToSaftHeadings(final AuthorityDto source, final List<HeadingRef> headingRefs) {
    processRelationshipHeadings(source.getSaftBroaderTerm(), headingRefs, RelationshipType.BROADER_TERM);
    processRelationshipHeadings(source.getSaftNarrowerTerm(), headingRefs, RelationshipType.NARROWER_TERM);
    processRelationshipHeadings(source.getSaftEarlierHeading(), headingRefs, RelationshipType.EARLIER_HEADING);
    processRelationshipHeadings(source.getSaftLaterHeading(), headingRefs, RelationshipType.LATER_HEADING);
  }

  private static void processRelationshipHeadings(List<String> relationshipHeadings, final List<HeadingRef> headingRefs,
      final RelationshipType relationshipType) {
    if (isNotEmpty(relationshipHeadings)) {
      headingRefs.forEach(headingRef -> {
        if (relationshipHeadings.contains(headingRef.getHeading())) {
          Set<RelationshipType> relationshipTypeSet = getOrCreateRelationshipTypeSet(headingRef);
          relationshipTypeSet.add(relationshipType);
        }
      });
    }
  }

  public static void extractAuthorityDtoHeadingValue(AuthorityBase source, AuthorityDto target) {
    if (source.getHeadingType() == null || source.getHeading() == null) {
      return;
    }
    switch (source.getHeadingType()) {
      case PERSONAL_NAME_HEADING -> target.setPersonalName(source.getHeading());
      case PERSONAL_NAME_TITLE_HEADING -> target.setPersonalNameTitle(source.getHeading());
      case CORPORATE_NAME_HEADING -> target.setCorporateName(source.getHeading());
      case CORPORATE_NAME_TITLE_HEADING -> target.setCorporateNameTitle(source.getHeading());
      case MEETING_NAME_HEADING -> target.setMeetingName(source.getHeading());
      case MEETING_NAME_TITLE_HEADING -> target.setMeetingNameTitle(source.getHeading());
      case UNIFORM_TITLE_HEADING -> target.setUniformTitle(source.getHeading());
      case TOPICAL_TERM_HEADING -> target.setTopicalTerm(source.getHeading());
      case GEOGRAPHIC_NAME_HEADING -> target.setGeographicName(source.getHeading());
      case GENRE_TERM_HEADING -> target.setGenreTerm(source.getHeading());
      default -> log.warn("Invalid heading type - {} cannot be mapped", source.getHeadingType());
    }
  }

  public static void extractAuthorityDtoSftHeadings(AuthorityBase source, AuthorityDto target) {
    if (isEmpty(source.getSftHeadings())) {
      return;
    }
    source.getSftHeadings().forEach(headingRef -> extractAuthorityDtoSftHeading(headingRef, target));
  }

  public static void extractAuthorityDtoSaftHeadings(AuthorityBase source, AuthorityDto target) {
    if (isEmpty(source.getSaftHeadings())) {
      return;
    }
    source.getSaftHeadings().forEach(headingRef -> extractAuthorityDtoSaftHeading(headingRef, target));
  }

  private void extractAuthorityDtoSftHeading(HeadingRef headingRef, AuthorityDto target) {
    if (headingRef == null || headingRef.getHeadingType() == null) {
      return;
    }
    switch (headingRef.getHeadingType()) {
      case PERSONAL_NAME_HEADING -> target.addSftPersonalNameItem(headingRef.getHeading());
      case PERSONAL_NAME_TITLE_HEADING -> target.addSftPersonalNameTitleItem(headingRef.getHeading());
      case CORPORATE_NAME_HEADING -> target.addSftCorporateNameItem(headingRef.getHeading());
      case CORPORATE_NAME_TITLE_HEADING -> target.addSftCorporateNameTitleItem(headingRef.getHeading());
      case MEETING_NAME_HEADING -> target.addSftMeetingNameItem(headingRef.getHeading());
      case MEETING_NAME_TITLE_HEADING -> target.addSftMeetingNameTitleItem(headingRef.getHeading());
      case UNIFORM_TITLE_HEADING -> target.addSftUniformTitleItem(headingRef.getHeading());
      case TOPICAL_TERM_HEADING -> target.addSftTopicalTermItem(headingRef.getHeading());
      case GEOGRAPHIC_NAME_HEADING -> target.addSftGeographicNameItem(headingRef.getHeading());
      case GENRE_TERM_HEADING -> target.addSftGenreTermItem(headingRef.getHeading());
      default -> log.warn("Invalid sft heading type - {} cannot be mapped", headingRef.getHeadingType());
    }
    extractSftHeadingsRelationships(headingRef, target);
  }

  private void extractAuthorityDtoSaftHeading(HeadingRef headingRef, AuthorityDto target) {
    if (headingRef == null || headingRef.getHeadingType() == null) {
      return;
    }
    switch (headingRef.getHeadingType()) {
      case PERSONAL_NAME_HEADING -> target.addSaftPersonalNameItem(headingRef.getHeading());
      case PERSONAL_NAME_TITLE_HEADING -> target.addSaftPersonalNameTitleItem(headingRef.getHeading());
      case CORPORATE_NAME_HEADING -> target.addSaftCorporateNameItem(headingRef.getHeading());
      case CORPORATE_NAME_TITLE_HEADING -> target.addSaftCorporateNameTitleItem(headingRef.getHeading());
      case MEETING_NAME_HEADING -> target.addSaftMeetingNameItem(headingRef.getHeading());
      case MEETING_NAME_TITLE_HEADING -> target.addSaftMeetingNameTitleItem(headingRef.getHeading());
      case UNIFORM_TITLE_HEADING -> target.addSaftUniformTitleItem(headingRef.getHeading());
      case TOPICAL_TERM_HEADING -> target.addSaftTopicalTermItem(headingRef.getHeading());
      case GEOGRAPHIC_NAME_HEADING -> target.addSaftGeographicNameItem(headingRef.getHeading());
      case GENRE_TERM_HEADING -> target.addSaftGenreTermItem(headingRef.getHeading());
      default -> log.warn("Invalid saft heading type - {} cannot be mapped", headingRef.getHeadingType());
    }
    extractSaftHeadingsRelationships(headingRef, target);
  }

  private static void extractSftHeadingsRelationships(HeadingRef headingRef, AuthorityDto target) {
    if (isNotEmpty(headingRef.getRelationshipType())) {
      headingRef.getRelationshipType().forEach(
          relationshipType -> {
            switch (relationshipType) {
              case BROADER_TERM -> addIfNotExists(target.getSftBroaderTerm(), headingRef.getHeading());
              case NARROWER_TERM ->   addIfNotExists(target.getSftNarrowerTerm(), headingRef.getHeading());
              case EARLIER_HEADING ->  addIfNotExists(target.getSftEarlierHeading(), headingRef.getHeading());
              case LATER_HEADING ->  addIfNotExists(target.getSftLaterHeading(), headingRef.getHeading());
              default -> log.warn("Invalid sft relationship type - {} cannot be mapped", relationshipType);
            }
          }
      );
    }
  }

  private static void extractSaftHeadingsRelationships(HeadingRef headingRef, AuthorityDto target) {
    if (isNotEmpty(headingRef.getRelationshipType())) {
      headingRef.getRelationshipType().forEach(
          relationshipType -> {
            switch (relationshipType) {
              case BROADER_TERM -> addIfNotExists(target.getSaftBroaderTerm(), headingRef.getHeading());
              case NARROWER_TERM ->   addIfNotExists(target.getSaftNarrowerTerm(), headingRef.getHeading());
              case EARLIER_HEADING ->  addIfNotExists(target.getSaftEarlierHeading(), headingRef.getHeading());
              case LATER_HEADING ->  addIfNotExists(target.getSaftLaterHeading(), headingRef.getHeading());
              default -> log.warn("Invalid saft relationship type - {} cannot be mapped", relationshipType);
            }
          }
      );
    }
  }

  private static List<HeadingRef> asSftHeadings(List<String> headingValues, String headingType) {
    return headingValues.stream()
        .map(headingValue -> new HeadingRef(headingType, headingValue))
        .toList();
  }

  private static Set<RelationshipType> getOrCreateRelationshipTypeSet(HeadingRef heading) {
    Set<RelationshipType> relationshipTypeSet = heading.getRelationshipType();
    if (relationshipTypeSet == null) {
      relationshipTypeSet = new HashSet<>();
      heading.setRelationshipType(relationshipTypeSet);
    }
    return relationshipTypeSet;
  }

  private static void addIfNotExists(List<String> headings, String heading) {
    if (!headings.contains(heading)) {
      headings.add(heading);
    }
  }
}
