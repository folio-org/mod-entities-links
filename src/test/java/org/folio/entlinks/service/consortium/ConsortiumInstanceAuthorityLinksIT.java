package org.folio.entlinks.service.consortium;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.folio.entlinks.service.consortium.ConsortiumAuthorityPropagationServiceIT.COLLEGE_TENANT_ID;
import static org.folio.entlinks.service.consortium.ConsortiumAuthorityPropagationServiceIT.UNIVERSITY_TENANT_ID;
import static org.folio.support.DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_TABLE;
import static org.folio.support.DatabaseHelper.INSTANCE_AUTHORITY_LINK_TABLE;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.TestDataUtils.AuthorityTestData.authoritySourceFile;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.CENTRAL_TENANT_ID;
import static org.folio.support.base.TestConstants.linksInstanceEndpoint;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.TestDataUtils;
import org.folio.support.TestDataUtils.Link;
import org.folio.support.base.IntegrationTestBase;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;

@IntegrationTest
@DatabaseCleanup(
    tables = {INSTANCE_AUTHORITY_LINK_TABLE, AUTHORITY_TABLE, AUTHORITY_SOURCE_FILE_CODE_TABLE,
      AUTHORITY_SOURCE_FILE_TABLE},
    tenants = {CENTRAL_TENANT_ID, COLLEGE_TENANT_ID, UNIVERSITY_TENANT_ID}
)
class ConsortiumInstanceAuthorityLinksIT extends IntegrationTestBase {

  @BeforeAll
  static void prepare() {
    setUpConsortium(CENTRAL_TENANT_ID, List.of(COLLEGE_TENANT_ID, UNIVERSITY_TENANT_ID), true);
  }

  @Test
  @SneakyThrows
  void updateInstanceLinks_positive_saveIncomingLinks_whenAnyExist() {
    var instanceId = randomUUID();
    var sourceFile = authoritySourceFile(0);
    final var incomingLinks = createLinkDtoCollection(2, instanceId);
    databaseHelper.saveAuthoritySourceFile(CENTRAL_TENANT_ID, sourceFile);
    databaseHelper.saveAuthoritySourceFile(COLLEGE_TENANT_ID, sourceFile);
    databaseHelper.saveAuthoritySourceFile(UNIVERSITY_TENANT_ID, sourceFile);
    createAuthoritiesForLinks(incomingLinks.getLinks());

    var httpHeaders = defaultHeaders();
    httpHeaders.put(XOkapiHeaders.TENANT, singletonList(CENTRAL_TENANT_ID));

    doPut(linksInstanceEndpoint(), incomingLinks, httpHeaders, instanceId);

    doGet(linksInstanceEndpoint(), httpHeaders, instanceId)
      .andExpect(linksMatch(hasSize(2)))
      .andExpect(linksMatch(incomingLinks))
      .andExpect(totalRecordsMatch(2));
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
      databaseHelper.saveAuthority(CENTRAL_TENANT_ID, authority);
      databaseHelper.saveAuthority(COLLEGE_TENANT_ID, authority);
      databaseHelper.saveAuthority(UNIVERSITY_TENANT_ID, authority);
    });
  }

  private ResultMatcher totalRecordsMatch(int recordsTotal) {
    return jsonPath("$.totalRecords", is(recordsTotal));
  }

  private ResultMatcher linksMatch(Matcher<Collection<? extends InstanceLinkDto>> matcher) {
    return jsonPath("$.links", matcher);
  }
}
