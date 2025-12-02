package org.folio.entlinks.controller.converter;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.RelatedHeading;
import org.folio.entlinks.domain.entity.AuthorityBase;
import org.folio.entlinks.domain.entity.HeadingRef;
import org.folio.entlinks.domain.entity.RelationshipType;

@UtilityClass
@Log4j2
public class AuthorityUtilityMapper {

  private static final Map<String, Function<AuthorityDto, String>> HEADING_EXTRACTORS =
    createHeadingExtractors();
  private static final Map<String, BiConsumer<AuthorityDto, String>> HEADING_SETTERS =
    createHeadingSetters();
  private static final Map<String, Function<AuthorityDto, List<String>>> SFT_HEADING_EXTRACTORS =
    createSftHeadingExtractors();
  private static final Map<String, BiConsumer<AuthorityDto, String>> SFT_HEADING_ADDERS =
    createSftHeadingAdders();
  private static final Map<String, Function<AuthorityDto, List<String>>> SAFT_HEADING_EXTRACTORS =
    createSaftHeadingExtractors();
  private static final Map<String, BiConsumer<AuthorityDto, String>> SAFT_HEADING_ADDERS =
    createSaftHeadingAdders();
  private static final Map<String, Function<AuthorityDto, List<String>>> SAFT_TRUNC_HEADING_EXTRACTORS =
    createSaftTruncHeadingExtractors();
  private static final Map<String, BiConsumer<AuthorityDto, String>> SAFT_TRUNC_HEADING_ADDERS =
    createSaftTruncHeadingAdders();

  public static void extractAuthorityHeading(AuthorityDto source, AuthorityBase target) {
    for (Map.Entry<String, Function<AuthorityDto, String>> entry : HEADING_EXTRACTORS.entrySet()) {
      var heading = entry.getValue().apply(source);
      if (Objects.nonNull(heading)) {
        target.setHeading(heading);
        target.setHeadingType(entry.getKey());
        return;
      }
    }
  }

  public static void extractAuthoritySftHeadings(AuthorityDto source, AuthorityBase target) {
    var sftHeadings = extractHeadingsFromMap(source, SFT_HEADING_EXTRACTORS);
    target.setSftHeadings(sftHeadings);
  }

  public static void extractAuthoritySaftHeadings(AuthorityDto source, AuthorityBase target) {
    var saftHeadings = extractHeadingsFromMap(source, SAFT_HEADING_EXTRACTORS);
    saftHeadings.addAll(extractHeadingsFromMap(source, SAFT_TRUNC_HEADING_EXTRACTORS));
    addRelationshipsToSaftHeadings(source, saftHeadings);
    target.setSaftHeadings(saftHeadings);
  }

  public static void extractAuthorityDtoHeadingValue(AuthorityBase source, AuthorityDto target) {
    if (source.getHeadingType() == null || source.getHeading() == null) {
      return;
    }
    var setter = HEADING_SETTERS.get(source.getHeadingType());
    if (setter != null) {
      setter.accept(target, source.getHeading());
    } else {
      log.warn("Invalid heading type - {} cannot be mapped", source.getHeadingType());
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

  private static Map<String, Function<AuthorityDto, String>> createHeadingExtractors() {
    Map<String, Function<AuthorityDto, String>> map = new LinkedHashMap<>();
    map.put(PERSONAL_NAME_HEADING, AuthorityDto::getPersonalName);
    map.put(PERSONAL_NAME_TITLE_HEADING, AuthorityDto::getPersonalNameTitle);
    map.put(CORPORATE_NAME_HEADING, AuthorityDto::getCorporateName);
    map.put(CORPORATE_NAME_TITLE_HEADING, AuthorityDto::getCorporateNameTitle);
    map.put(MEETING_NAME_HEADING, AuthorityDto::getMeetingName);
    map.put(MEETING_NAME_TITLE_HEADING, AuthorityDto::getMeetingNameTitle);
    map.put(UNIFORM_TITLE_HEADING, AuthorityDto::getUniformTitle);
    map.put(NAMED_EVENT_HEADING, AuthorityDto::getNamedEvent);
    map.put(TOPICAL_TERM_HEADING, AuthorityDto::getTopicalTerm);
    map.put(GEOGRAPHIC_NAME_HEADING, AuthorityDto::getGeographicName);
    map.put(GENRE_TERM_HEADING, AuthorityDto::getGenreTerm);
    map.put(CHRON_TERM_HEADING, AuthorityDto::getChronTerm);
    map.put(MEDIUM_PERF_TERM_HEADING, AuthorityDto::getMediumPerfTerm);
    map.put(GENERAL_SUBDIVISION_HEADING, AuthorityDto::getGeneralSubdivision);
    map.put(GEOGRAPHIC_SUBDIVISION_HEADING, AuthorityDto::getGeographicSubdivision);
    map.put(CHRON_SUBDIVISION_HEADING, AuthorityDto::getChronSubdivision);
    map.put(FORM_SUBDIVISION_HEADING, AuthorityDto::getFormSubdivision);
    return map;
  }

  private static Map<String, Function<AuthorityDto, List<String>>> createSftHeadingExtractors() {
    Map<String, Function<AuthorityDto, List<String>>> map = new LinkedHashMap<>();
    map.put(PERSONAL_NAME_HEADING, AuthorityDto::getSftPersonalName);
    map.put(PERSONAL_NAME_TITLE_HEADING, AuthorityDto::getSftPersonalNameTitle);
    map.put(CORPORATE_NAME_HEADING, AuthorityDto::getSftCorporateName);
    map.put(CORPORATE_NAME_TITLE_HEADING, AuthorityDto::getSftCorporateNameTitle);
    map.put(MEETING_NAME_HEADING, AuthorityDto::getSftMeetingName);
    map.put(MEETING_NAME_TITLE_HEADING, AuthorityDto::getSftMeetingNameTitle);
    map.put(UNIFORM_TITLE_HEADING, AuthorityDto::getSftUniformTitle);
    map.put(NAMED_EVENT_HEADING, AuthorityDto::getSftNamedEvent);
    map.put(TOPICAL_TERM_HEADING, AuthorityDto::getSftTopicalTerm);
    map.put(GEOGRAPHIC_NAME_HEADING, AuthorityDto::getSftGeographicName);
    map.put(GENRE_TERM_HEADING, AuthorityDto::getSftGenreTerm);
    map.put(CHRON_TERM_HEADING, AuthorityDto::getSftChronTerm);
    map.put(MEDIUM_PERF_TERM_HEADING, AuthorityDto::getSftMediumPerfTerm);
    map.put(GENERAL_SUBDIVISION_HEADING, AuthorityDto::getSftGeneralSubdivision);
    map.put(GEOGRAPHIC_SUBDIVISION_HEADING, AuthorityDto::getSftGeographicSubdivision);
    map.put(CHRON_SUBDIVISION_HEADING, AuthorityDto::getSftChronSubdivision);
    map.put(FORM_SUBDIVISION_HEADING, AuthorityDto::getSftFormSubdivision);
    return map;
  }

  private static Map<String, Function<AuthorityDto, List<String>>> createSaftHeadingExtractors() {
    Map<String, Function<AuthorityDto, List<String>>> map = new LinkedHashMap<>();
    map.put(PERSONAL_NAME_HEADING, AuthorityDto::getSaftPersonalName);
    map.put(PERSONAL_NAME_TITLE_HEADING, AuthorityDto::getSaftPersonalNameTitle);
    map.put(CORPORATE_NAME_HEADING, AuthorityDto::getSaftCorporateName);
    map.put(CORPORATE_NAME_TITLE_HEADING, AuthorityDto::getSaftCorporateNameTitle);
    map.put(MEETING_NAME_HEADING, AuthorityDto::getSaftMeetingName);
    map.put(MEETING_NAME_TITLE_HEADING, AuthorityDto::getSaftMeetingNameTitle);
    map.put(UNIFORM_TITLE_HEADING, AuthorityDto::getSaftUniformTitle);
    map.put(NAMED_EVENT_HEADING, AuthorityDto::getSaftNamedEvent);
    map.put(TOPICAL_TERM_HEADING, AuthorityDto::getSaftTopicalTerm);
    map.put(GEOGRAPHIC_NAME_HEADING, AuthorityDto::getSaftGeographicName);
    map.put(GENRE_TERM_HEADING, AuthorityDto::getSaftGenreTerm);
    map.put(CHRON_TERM_HEADING, AuthorityDto::getSaftChronTerm);
    map.put(MEDIUM_PERF_TERM_HEADING, AuthorityDto::getSaftMediumPerfTerm);
    map.put(GENERAL_SUBDIVISION_HEADING, AuthorityDto::getSaftGeneralSubdivision);
    map.put(GEOGRAPHIC_SUBDIVISION_HEADING, AuthorityDto::getSaftGeographicSubdivision);
    map.put(CHRON_SUBDIVISION_HEADING, AuthorityDto::getSaftChronSubdivision);
    map.put(FORM_SUBDIVISION_HEADING, AuthorityDto::getSaftFormSubdivision);
    return map;
  }

  private static Map<String, Function<AuthorityDto, List<String>>> createSaftTruncHeadingExtractors() {
    Map<String, Function<AuthorityDto, List<String>>> map = new LinkedHashMap<>();
    map.put(PERSONAL_NAME_HEADING_TRUNC, AuthorityDto::getSaftPersonalNameTrunc);
    map.put(PERSONAL_NAME_TITLE_HEADING_TRUNC, AuthorityDto::getSaftPersonalNameTitleTrunc);
    map.put(CORPORATE_NAME_HEADING_TRUNC, AuthorityDto::getSaftCorporateNameTrunc);
    map.put(CORPORATE_NAME_TITLE_HEADING_TRUNC, AuthorityDto::getSaftCorporateNameTitleTrunc);
    map.put(MEETING_NAME_HEADING_TRUNC, AuthorityDto::getSaftMeetingNameTrunc);
    map.put(MEETING_NAME_TITLE_HEADING_TRUNC, AuthorityDto::getSaftMeetingNameTitleTrunc);
    map.put(UNIFORM_TITLE_HEADING_TRUNC, AuthorityDto::getSaftUniformTitleTrunc);
    map.put(TOPICAL_TERM_HEADING_TRUNC, AuthorityDto::getSaftTopicalTermTrunc);
    map.put(GEOGRAPHIC_NAME_HEADING_TRUNC, AuthorityDto::getSaftGeographicNameTrunc);
    map.put(GENRE_TERM_HEADING_TRUNC, AuthorityDto::getSaftGenreTermTrunc);
    map.put(NAMED_EVENT_HEADING_TRUNC, AuthorityDto::getSaftNamedEventTrunc);
    map.put(CHRON_TERM_HEADING_TRUNC, AuthorityDto::getSaftChronTermTrunc);
    map.put(MEDIUM_PERF_TERM_HEADING_TRUNC, AuthorityDto::getSaftMediumPerfTermTrunc);
    map.put(GENERAL_SUBDIVISION_HEADING_TRUNC, AuthorityDto::getSaftGeneralSubdivisionTrunc);
    map.put(GEOGRAPHIC_SUBDIVISION_HEADING_TRUNC, AuthorityDto::getSaftGeographicSubdivisionTrunc);
    map.put(CHRON_SUBDIVISION_HEADING_TRUNC, AuthorityDto::getSaftChronSubdivisionTrunc);
    map.put(FORM_SUBDIVISION_HEADING_TRUNC, AuthorityDto::getSaftFormSubdivisionTrunc);
    return map;
  }

  private static Map<String, BiConsumer<AuthorityDto, String>> createHeadingSetters() {
    Map<String, BiConsumer<AuthorityDto, String>> map = new LinkedHashMap<>();
    map.put(PERSONAL_NAME_HEADING, AuthorityDto::setPersonalName);
    map.put(PERSONAL_NAME_TITLE_HEADING, AuthorityDto::setPersonalNameTitle);
    map.put(CORPORATE_NAME_HEADING, AuthorityDto::setCorporateName);
    map.put(CORPORATE_NAME_TITLE_HEADING, AuthorityDto::setCorporateNameTitle);
    map.put(MEETING_NAME_HEADING, AuthorityDto::setMeetingName);
    map.put(MEETING_NAME_TITLE_HEADING, AuthorityDto::setMeetingNameTitle);
    map.put(UNIFORM_TITLE_HEADING, AuthorityDto::setUniformTitle);
    map.put(NAMED_EVENT_HEADING, AuthorityDto::setNamedEvent);
    map.put(TOPICAL_TERM_HEADING, AuthorityDto::setTopicalTerm);
    map.put(GEOGRAPHIC_NAME_HEADING, AuthorityDto::setGeographicName);
    map.put(GENRE_TERM_HEADING, AuthorityDto::setGenreTerm);
    map.put(CHRON_TERM_HEADING, AuthorityDto::setChronTerm);
    map.put(MEDIUM_PERF_TERM_HEADING, AuthorityDto::setMediumPerfTerm);
    map.put(GENERAL_SUBDIVISION_HEADING, AuthorityDto::setGeneralSubdivision);
    map.put(GEOGRAPHIC_SUBDIVISION_HEADING, AuthorityDto::setGeographicSubdivision);
    map.put(CHRON_SUBDIVISION_HEADING, AuthorityDto::setChronSubdivision);
    map.put(FORM_SUBDIVISION_HEADING, AuthorityDto::setFormSubdivision);
    return map;
  }

  private static Map<String, BiConsumer<AuthorityDto, String>> createSftHeadingAdders() {
    Map<String, BiConsumer<AuthorityDto, String>> map = new LinkedHashMap<>();
    map.put(PERSONAL_NAME_HEADING, AuthorityDto::addSftPersonalNameItem);
    map.put(PERSONAL_NAME_TITLE_HEADING, AuthorityDto::addSftPersonalNameTitleItem);
    map.put(CORPORATE_NAME_HEADING, AuthorityDto::addSftCorporateNameItem);
    map.put(CORPORATE_NAME_TITLE_HEADING, AuthorityDto::addSftCorporateNameTitleItem);
    map.put(MEETING_NAME_HEADING, AuthorityDto::addSftMeetingNameItem);
    map.put(MEETING_NAME_TITLE_HEADING, AuthorityDto::addSftMeetingNameTitleItem);
    map.put(UNIFORM_TITLE_HEADING, AuthorityDto::addSftUniformTitleItem);
    map.put(NAMED_EVENT_HEADING, AuthorityDto::addSftNamedEventItem);
    map.put(TOPICAL_TERM_HEADING, AuthorityDto::addSftTopicalTermItem);
    map.put(GEOGRAPHIC_NAME_HEADING, AuthorityDto::addSftGeographicNameItem);
    map.put(GENRE_TERM_HEADING, AuthorityDto::addSftGenreTermItem);
    map.put(CHRON_TERM_HEADING, AuthorityDto::addSftChronTermItem);
    map.put(MEDIUM_PERF_TERM_HEADING, AuthorityDto::addSftMediumPerfTermItem);
    map.put(GENERAL_SUBDIVISION_HEADING, AuthorityDto::addSftGeneralSubdivisionItem);
    map.put(GEOGRAPHIC_SUBDIVISION_HEADING, AuthorityDto::addSftGeographicSubdivisionItem);
    map.put(CHRON_SUBDIVISION_HEADING, AuthorityDto::addSftChronSubdivisionItem);
    map.put(FORM_SUBDIVISION_HEADING, AuthorityDto::addSftFormSubdivisionItem);
    return map;
  }

  private static Map<String, BiConsumer<AuthorityDto, String>> createSaftHeadingAdders() {
    Map<String, BiConsumer<AuthorityDto, String>> map = new LinkedHashMap<>();
    map.put(PERSONAL_NAME_HEADING, AuthorityDto::addSaftPersonalNameItem);
    map.put(PERSONAL_NAME_TITLE_HEADING, AuthorityDto::addSaftPersonalNameTitleItem);
    map.put(CORPORATE_NAME_HEADING, AuthorityDto::addSaftCorporateNameItem);
    map.put(CORPORATE_NAME_TITLE_HEADING, AuthorityDto::addSaftCorporateNameTitleItem);
    map.put(MEETING_NAME_HEADING, AuthorityDto::addSaftMeetingNameItem);
    map.put(MEETING_NAME_TITLE_HEADING, AuthorityDto::addSaftMeetingNameTitleItem);
    map.put(UNIFORM_TITLE_HEADING, AuthorityDto::addSaftUniformTitleItem);
    map.put(NAMED_EVENT_HEADING, AuthorityDto::addSaftNamedEventItem);
    map.put(TOPICAL_TERM_HEADING, AuthorityDto::addSaftTopicalTermItem);
    map.put(GEOGRAPHIC_NAME_HEADING, AuthorityDto::addSaftGeographicNameItem);
    map.put(GENRE_TERM_HEADING, AuthorityDto::addSaftGenreTermItem);
    map.put(CHRON_TERM_HEADING, AuthorityDto::addSaftChronTermItem);
    map.put(MEDIUM_PERF_TERM_HEADING, AuthorityDto::addSaftMediumPerfTermItem);
    map.put(GENERAL_SUBDIVISION_HEADING, AuthorityDto::addSaftGeneralSubdivisionItem);
    map.put(GEOGRAPHIC_SUBDIVISION_HEADING, AuthorityDto::addSaftGeographicSubdivisionItem);
    map.put(CHRON_SUBDIVISION_HEADING, AuthorityDto::addSaftChronSubdivisionItem);
    map.put(FORM_SUBDIVISION_HEADING, AuthorityDto::addSaftFormSubdivisionItem);
    return map;
  }

  private static Map<String, BiConsumer<AuthorityDto, String>> createSaftTruncHeadingAdders() {
    Map<String, BiConsumer<AuthorityDto, String>> map = new LinkedHashMap<>();
    map.put(PERSONAL_NAME_HEADING_TRUNC, AuthorityDto::addSaftPersonalNameTruncItem);
    map.put(PERSONAL_NAME_TITLE_HEADING_TRUNC, AuthorityDto::addSaftPersonalNameTitleTruncItem);
    map.put(CORPORATE_NAME_HEADING_TRUNC, AuthorityDto::addSaftCorporateNameTruncItem);
    map.put(CORPORATE_NAME_TITLE_HEADING_TRUNC, AuthorityDto::addSaftCorporateNameTitleTruncItem);
    map.put(MEETING_NAME_HEADING_TRUNC, AuthorityDto::addSaftMeetingNameTruncItem);
    map.put(MEETING_NAME_TITLE_HEADING_TRUNC, AuthorityDto::addSaftMeetingNameTitleTruncItem);
    map.put(UNIFORM_TITLE_HEADING_TRUNC, AuthorityDto::addSaftUniformTitleTruncItem);
    map.put(TOPICAL_TERM_HEADING_TRUNC, AuthorityDto::addSaftTopicalTermTruncItem);
    map.put(GEOGRAPHIC_NAME_HEADING_TRUNC, AuthorityDto::addSaftGeographicNameTruncItem);
    map.put(GENRE_TERM_HEADING_TRUNC, AuthorityDto::addSaftGenreTermTruncItem);
    map.put(NAMED_EVENT_HEADING_TRUNC, AuthorityDto::addSaftNamedEventTruncItem);
    map.put(CHRON_TERM_HEADING_TRUNC, AuthorityDto::addSaftChronTermTruncItem);
    map.put(MEDIUM_PERF_TERM_HEADING_TRUNC, AuthorityDto::addSaftMediumPerfTermTruncItem);
    map.put(GENERAL_SUBDIVISION_HEADING_TRUNC, AuthorityDto::addSaftGeneralSubdivisionTruncItem);
    map.put(GEOGRAPHIC_SUBDIVISION_HEADING_TRUNC, AuthorityDto::addSaftGeographicSubdivisionTruncItem);
    map.put(CHRON_SUBDIVISION_HEADING_TRUNC, AuthorityDto::addSaftChronSubdivisionTruncItem);
    map.put(FORM_SUBDIVISION_HEADING_TRUNC, AuthorityDto::addSaftFormSubdivisionTruncItem);
    return map;
  }

  private static List<HeadingRef> extractHeadingsFromMap(AuthorityDto source,
                                                         Map<String, Function<AuthorityDto, List<String>>> extractors) {
    var headings = new ArrayList<HeadingRef>();
    for (var entry : extractors.entrySet()) {
      var values = entry.getValue().apply(source);
      if (isNotEmpty(values)) {
        headings.addAll(asSftHeadings(values, entry.getKey()));
      }
    }
    return headings;
  }

  private static void addRelationshipsToSaftHeadings(final AuthorityDto source, final List<HeadingRef> headingRefs) {
    processRelationshipHeadings(source.getSaftBroaderTerm(), headingRefs, RelationshipType.BROADER_TERM);
    processRelationshipHeadings(source.getSaftNarrowerTerm(), headingRefs, RelationshipType.NARROWER_TERM);
    processRelationshipHeadings(source.getSaftEarlierHeading(), headingRefs, RelationshipType.EARLIER_HEADING);
    processRelationshipHeadings(source.getSaftLaterHeading(), headingRefs, RelationshipType.LATER_HEADING);
  }

  private static void processRelationshipHeadings(List<RelatedHeading> relationshipHeadings,
                                                  final List<HeadingRef> headingRefs,
                                                  final RelationshipType relationshipType) {
    if (isNotEmpty(relationshipHeadings)) {
      headingRefs.forEach(headingRef ->
        relationshipHeadings.forEach(relationshipHeading -> {
          if (isProperRelationship(headingRef, relationshipHeading)) {
            var relationshipTypeSet = getOrCreateRelationshipTypeSet(headingRef);
            relationshipTypeSet.add(relationshipType);
          }
        })
      );
    }
  }

  private static boolean isProperRelationship(HeadingRef headingRef, RelatedHeading relationshipHeading) {
    return Objects.equals(relationshipHeading.getHeadingType(), headingRef.getHeadingType())
           && Objects.equals(relationshipHeading.getHeadingRef(), headingRef.getHeading());
  }

  private static void extractAuthorityDtoSftHeading(HeadingRef headingRef, AuthorityDto target) {
    if (headingRef == null || headingRef.getHeadingType() == null) {
      return;
    }
    var adder = SFT_HEADING_ADDERS.get(headingRef.getHeadingType());
    if (adder != null) {
      adder.accept(target, headingRef.getHeading());
    } else {
      log.warn("Invalid sft heading type - {} cannot be mapped", headingRef.getHeadingType());
    }
  }

  private static void extractAuthorityDtoSaftHeading(HeadingRef headingRef, AuthorityDto target) {
    if (headingRef == null || headingRef.getHeadingType() == null) {
      return;
    }
    var adder = getSaftHeadingAdder(headingRef);
    if (adder != null) {
      adder.accept(target, headingRef.getHeading());
    } else {
      log.warn("Invalid saft heading type - {} cannot be mapped", headingRef.getHeadingType());
    }
    extractSaftHeadingsRelationships(headingRef, target);
  }

  private static BiConsumer<AuthorityDto, String> getSaftHeadingAdder(HeadingRef headingRef) {
    var adder = SAFT_HEADING_ADDERS.get(headingRef.getHeadingType());
    if (adder == null) {
      adder = SAFT_TRUNC_HEADING_ADDERS.get(headingRef.getHeadingType());
    }
    return adder;
  }

  private static void extractSaftHeadingsRelationships(HeadingRef headingRef, AuthorityDto target) {
    if (isNotEmpty(headingRef.getRelationshipType())) {
      headingRef.getRelationshipType().forEach(
        relationshipType -> {
          switch (relationshipType) {
            case BROADER_TERM -> target.getSaftBroaderTerm()
              .add(new RelatedHeading(headingRef.getHeading(), headingRef.getHeadingType()));
            case NARROWER_TERM -> target.getSaftNarrowerTerm()
              .add(new RelatedHeading(headingRef.getHeading(), headingRef.getHeadingType()));
            case EARLIER_HEADING -> target.getSaftEarlierHeading()
              .add(new RelatedHeading(headingRef.getHeading(), headingRef.getHeadingType()));
            case LATER_HEADING -> target.getSaftLaterHeading()
              .add(new RelatedHeading(headingRef.getHeading(), headingRef.getHeadingType()));
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
}
