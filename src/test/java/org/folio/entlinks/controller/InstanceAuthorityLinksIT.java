package org.folio.entlinks.controller;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.folio.support.JsonTestUtils.asJson;
import static org.folio.support.MatchUtils.errorCodeMatch;
import static org.folio.support.MatchUtils.errorMessageMatch;
import static org.folio.support.MatchUtils.errorParameterMatch;
import static org.folio.support.MatchUtils.errorTotalMatch;
import static org.folio.support.MatchUtils.errorTypeMatch;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.TestDataUtils.AuthorityTestData.authoritySourceFile;
import static org.folio.support.TestDataUtils.Link.TAGS;
import static org.folio.support.TestDataUtils.NATURAL_IDS;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.authoritiesLinksCountEndpoint;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.LinksCountDto;
import org.folio.entlinks.domain.dto.LinksCountDtoCollection;
import org.folio.entlinks.domain.dto.UuidCollection;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.exception.type.ErrorType;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.TestDataUtils.Link;
import org.folio.support.base.IntegrationTestBase;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.ResultMatcher;

@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.INSTANCE_AUTHORITY_LINK_TABLE,
  DatabaseHelper.AUTHORITY_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE})
class InstanceAuthorityLinksIT extends IntegrationTestBase {

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @BeforeEach
  void setup() {
    setUpTenant();
    var sourceFile = authoritySourceFile(0);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, sourceFile);
  }

  @Test
  void getInstanceLinks_positive_noLinksFound() throws Exception {
    doGet(linksInstanceEndpoint(), randomUUID())
      .andExpect(linksMatch(empty()))
      .andExpect(totalRecordsMatch(0));
  }

  @Test
  void getInstanceLinks_negative_invalidId() throws Exception {
    tryGet(linksInstanceEndpoint(), "not a uuid")
      .andExpect(status().isBadRequest())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("MethodArgumentTypeMismatchException")))
      .andExpect(errorCodeMatch(is(ErrorType.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("Invalid UUID string")));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_saveIncomingLinks_whenAnyExist() {
    var instanceId = randomUUID();
    var incomingLinks = createLinkDtoCollection(2, instanceId);
    createAuthoritiesForLinks(incomingLinks.getLinks());
    doPut(linksInstanceEndpoint(), incomingLinks, instanceId);

    doGet(linksInstanceEndpoint(), instanceId)
      .andExpect(linksMatch(hasSize(2)))
      .andExpect(linksMatch(incomingLinks))
      .andExpect(totalRecordsMatch(2));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_deleteAllLinks_whenIncomingIsEmpty() {
    var instanceId = randomUUID();
    var existedLinks = createLinkDtoCollection(2, instanceId);
    createAuthoritiesForLinks(existedLinks.getLinks());
    doPut(linksInstanceEndpoint(), existedLinks, instanceId);

    var incomingLinks = linksDtoCollection(emptyList());
    doPut(linksInstanceEndpoint(), incomingLinks, instanceId);

    doGet(linksInstanceEndpoint(), instanceId)
      .andExpect(linksMatch(hasSize(0)))
      .andExpect(totalRecordsMatch(0));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_deleteAllExistedAndSaveAllIncomingLinks() {
    var instanceId = randomUUID();
    var existedLinks = createLinkDtoCollection(4, instanceId);
    createAuthoritiesForLinks(existedLinks.getLinks());
    doPut(linksInstanceEndpoint(), existedLinks, instanceId);

    var incomingLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 2, TestDataUtils.NATURAL_IDS[0]),
      Link.of(1, 3, TestDataUtils.NATURAL_IDS[1]),
      Link.of(2, 1, TestDataUtils.NATURAL_IDS[2]),
      Link.of(3, 0, TestDataUtils.NATURAL_IDS[3])
    ));
    doPut(linksInstanceEndpoint(), incomingLinks, instanceId);

    doGet(linksInstanceEndpoint(), instanceId)
      .andExpect(linksMatch(hasSize(4)))
      .andExpect(linksMatch(incomingLinks))
      .andExpect(totalRecordsMatch(4));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_saveOnlyNewLinks() {
    var instanceId = randomUUID();
    var existedLinks = createLinkDtoCollection(2, instanceId);
    createAuthoritiesForLinks(existedLinks.getLinks());
    doPut(linksInstanceEndpoint(), existedLinks, instanceId);

    var incomingLinks = createLinkDtoCollection(4, instanceId);
    createAuthoritiesForLinks(incomingLinks.getLinks().subList(2, 4));
    doPut(linksInstanceEndpoint(), incomingLinks, instanceId);

    doGet(linksInstanceEndpoint(), instanceId)
      .andExpect(linksMatch(hasSize(4)))
      .andExpect(linksMatch(incomingLinks))
      .andExpect(totalRecordsMatch(4));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_updateExistedLinks() {
    var instanceId = randomUUID();
    var existedLinks = createLinkDtoCollection(2, instanceId);
    createAuthoritiesForLinks(existedLinks.getLinks());
    doPut(linksInstanceEndpoint(), existedLinks, instanceId);

    databaseHelper.updateAuthorityNaturalId(TENANT_ID, TestDataUtils.AUTHORITY_IDS[0], NATURAL_IDS[2]);
    databaseHelper.updateAuthorityNaturalId(TENANT_ID, TestDataUtils.AUTHORITY_IDS[1], NATURAL_IDS[3]);
    var incomingLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 0, TestDataUtils.NATURAL_IDS[2]),
      Link.of(1, 1, TestDataUtils.NATURAL_IDS[3])
    ));
    doPut(linksInstanceEndpoint(), incomingLinks, instanceId);

    doGet(linksInstanceEndpoint(), instanceId)
      .andExpect(linksMatch(hasSize(2)))
      .andExpect(linksMatch(incomingLinks))
      .andExpect(totalRecordsMatch(2));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_deleteAndSaveLinks_whenHaveDifference() {
    var instanceId = randomUUID();
    var existedLinks = createLinkDtoCollection(4, instanceId);
    createAuthoritiesForLinks(existedLinks.getLinks());
    doPut(linksInstanceEndpoint(), existedLinks, instanceId);

    var incomingLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(0, 0, TestDataUtils.NATURAL_IDS[0]),
      Link.of(1, 1, TestDataUtils.NATURAL_IDS[1]),
      Link.of(2, 3, TestDataUtils.NATURAL_IDS[2]),
      Link.of(3, 2, TestDataUtils.NATURAL_IDS[3])
    ));
    doPut(linksInstanceEndpoint(), incomingLinks, instanceId);

    doGet(linksInstanceEndpoint(), instanceId)
      .andExpect(linksMatch(hasSize(4)))
      .andExpect(linksMatch(incomingLinks))
      .andExpect(totalRecordsMatch(4));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_ignoreReadOnlyFields() {
    var instanceId = randomUUID();
    var link = Link.of(InstanceAuthorityLinkStatus.ERROR, "test");
    var incomingLinks = linksDtoCollection(linksDto(instanceId, link));
    var authority = authority(0, 0);
    authority.setNaturalId(link.naturalId());
    databaseHelper.saveAuthority(TENANT_ID, authority);
    doPut(linksInstanceEndpoint(), incomingLinks, instanceId);

    var expectedLinks = linksDtoCollection(linksDto(instanceId,
      Link.of(InstanceAuthorityLinkStatus.ACTUAL, null)));

    doGet(linksInstanceEndpoint(), instanceId)
      .andExpect(linksMatch(hasSize(1)))
      .andExpect(linksMatch(expectedLinks))
      .andExpect(totalRecordsMatch(1));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_negative_whenInstanceIdIsNotSameForIncomingLinks() {
    var instanceId = randomUUID();
    var incomingLinks = linksDtoCollection(linksDto(randomUUID(),
      Link.of(0, 0),
      Link.of(1, 1),
      Link.of(2, 3),
      Link.of(3, 2)
    ));

    tryPut(linksInstanceEndpoint(), incomingLinks, instanceId)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("RequestBodyValidationException")))
      .andExpect(errorCodeMatch(is(ErrorType.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("Link should have instanceId = " + instanceId)));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_negative_whenInstanceIdIsNotUuid() {
    var invalidInstanceId = "1111";
    var incomingLinks = linksDtoCollection(emptyList());

    tryPut(linksInstanceEndpoint(), incomingLinks, invalidInstanceId)
      .andExpect(status().isBadRequest())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("MethodArgumentTypeMismatchException")))
      .andExpect(errorCodeMatch(is(ErrorType.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("Invalid UUID string")));
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_negative_whenBodyIsEmpty() {
    var instanceId = randomUUID();

    tryPut(linksInstanceEndpoint(), null, instanceId)
      .andExpect(status().isBadRequest())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("HttpMessageNotReadableException")))
      .andExpect(errorCodeMatch(is(ErrorType.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("Required request body is missing")));
  }

  @SneakyThrows
  @MethodSource("requiredFieldMissingProvider")
  @ParameterizedTest(name = "[{index}] missing {0}")
  void updateInstanceLinks_negative_whenRequiredFieldIsMissing(String missingField, InstanceLinkDto invalidLink) {
    var instanceId = randomUUID();
    var incomingLinks = linksDtoCollection(List.of(invalidLink));

    tryPut(linksInstanceEndpoint(), incomingLinks, instanceId)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("MethodArgumentNotValidException")))
      .andExpect(errorCodeMatch(is(ErrorType.VALIDATION_ERROR.getValue())))
      .andExpect(errorMessageMatch(containsString("must not be null")))
      .andExpect(errorParameterMatch(is("links[0]." + missingField)));
  }

  @Test
  @SneakyThrows
  void countNumberOfTitles_positive_whenInstanceLinksExist() {
    var instanceId = randomUUID();
    var authorityId = TestDataUtils.AUTHORITY_IDS[0];
    var authority1 = authority(0, 0);
    databaseHelper.saveAuthority(TENANT_ID, authority1);
    var links = linksDtoCollection(linksDto(instanceId,
      new Link(authorityId, TAGS[1]),
      new Link(authorityId, TAGS[2]),
      new Link(authorityId, TAGS[3])
    ));
    doPut(linksInstanceEndpoint(), links, instanceId);

    var secondInstanceId = randomUUID();
    var secondAuthorityId = TestDataUtils.AUTHORITY_IDS[1];
    var authority2 = authority(1, 0);
    databaseHelper.saveAuthority(TENANT_ID, authority2);
    var secondLinks = linksDtoCollection(linksDto(secondInstanceId,
      new Link(authorityId, TAGS[1]),
      new Link(authorityId, TAGS[2]),
      new Link(secondAuthorityId, TAGS[0]),
      new Link(secondAuthorityId, TAGS[2])
    ));
    doPut(linksInstanceEndpoint(), secondLinks, secondInstanceId);

    var requestBody = new UuidCollection(List.of(authorityId, secondAuthorityId));
    doPost(authoritiesLinksCountEndpoint(), requestBody)
      .andExpect(status().isOk())
      .andExpect(linksMatch(hasSize(2)))
      .andExpect(content().json(asJson(new LinksCountDtoCollection(
        List.of(
          new LinksCountDto().id(secondAuthorityId).totalLinks(1),
          new LinksCountDto().id(authorityId).totalLinks(2)
        )), objectMapper)));
  }

  @Test
  @SneakyThrows
  void countNumberOfTitles_positive_whenInstanceLinksNotExistThenReturnZeroCount() {
    var requestBody = new UuidCollection(List.of(randomUUID(), randomUUID()));
    doPost(authoritiesLinksCountEndpoint(), requestBody)
      .andExpect(status().isOk())
      .andExpect(linksMatch(hasSize(2)))
      .andExpect(jsonPath("$.links.[0].totalLinks", is(0)))
      .andExpect(jsonPath("$.links.[1].totalLinks", is(0)));
  }

  @Test
  @SneakyThrows
  void countNumberOfTitles_positive_whenRequestBodyIsEmptyThenReturnEmptyList() {
    var requestBody = new UuidCollection(List.of());
    doPost(authoritiesLinksCountEndpoint(), requestBody)
      .andExpect(status().isOk())
      .andExpect(linksMatch(hasSize(0)));
  }

  @Test
  @SneakyThrows
  void countNumberOfTitles_negative_whenRequestBodyInvalidThenThrowsValidationException() {
    var requestBody = List.of("not uuid collection object");
    tryPost(authoritiesLinksCountEndpoint(), requestBody)
      .andExpect(status().isBadRequest())
      .andExpect(errorTotalMatch(1))
      .andExpect(errorTypeMatch(is("HttpMessageNotReadableException")))
      .andExpect(errorCodeMatch(is(ErrorType.VALIDATION_ERROR.getValue())));
  }

  static Stream<Arguments> requiredFieldMissingProvider() {
    return Stream.of(
      arguments("instanceId",
        new InstanceLinkDto()
          .authorityId(randomUUID()).authorityNaturalId("id")
          .linkingRuleId(1)
      ),
      arguments("authorityId",
        new InstanceLinkDto().instanceId(randomUUID())
          .authorityNaturalId("id")
          .linkingRuleId(1)
      ),
      arguments("authorityNaturalId",
        new InstanceLinkDto().instanceId(randomUUID())
          .authorityId(randomUUID())
          .linkingRuleId(1)
      ),
      arguments("linkingRuleId",
        new InstanceLinkDto().instanceId(randomUUID())
          .authorityId(randomUUID()).authorityNaturalId("id")
      )
    );
  }

  private InstanceLinkDtoCollection createLinkDtoCollection(int num, UUID instanceId) {
    var links = IntStream.range(0, num)
      .mapToObj(i -> Link.of(i, i, TestDataUtils.NATURAL_IDS[i]))
      .toArray(Link[]::new);
    return linksDtoCollection(linksDto(instanceId, links));
  }

  private void createAuthoritiesForLinks(List<InstanceLinkDto> links) {
    var authority = authority(0, 0);
    links.forEach(link -> {
      authority.setId(link.getAuthorityId());
      authority.setNaturalId(link.getAuthorityNaturalId());
      databaseHelper.saveAuthority(TENANT_ID, authority);
    });
  }

  private ResultMatcher totalRecordsMatch(int recordsTotal) {
    return jsonPath("$.totalRecords", is(recordsTotal));
  }

  private ResultMatcher linksMatch(Matcher<Collection<? extends InstanceLinkDto>> matcher) {
    return jsonPath("$.links", matcher);
  }
}
