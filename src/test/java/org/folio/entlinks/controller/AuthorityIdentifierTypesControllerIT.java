package org.folio.entlinks.controller;

import static org.folio.support.base.TestConstants.authorityIdentifierTypesEndpoint;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.UUID;
import org.folio.entlinks.config.JpaConfig;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

@IntegrationTest
@Import(JpaConfig.class)
class AuthorityIdentifierTypesControllerIT extends IntegrationTestBase {

  private static final String DEFAULT_USER_ID = "00000000-0000-0000-0000-000000000000";

  private static final UUID[] DEFAULT_IDENTIFIER_TYPE_IDS = new UUID[] {
    UUID.fromString("c858e4f2-2b6b-4385-842b-60532ee34abb"),
    UUID.fromString("5d164f4b-0b15-4e42-ae75-cfcf85318ad9"),
    UUID.fromString("c858e4f2-2b6b-4385-842b-60732ee14abb"),
    UUID.fromString("2e8b3b6c-0e7d-4e48-bca2-b0b23b376af5"),
    UUID.fromString("7e591197-f335-4afb-bc6d-a6d76ca3bace")
  };
  private static final String[] DEFAULT_IDENTIFIER_TYPE_NAMES = new String[] {
    "Canceled LCCN",
    "Control number",
    "LCCN",
    "Other standard identifier",
    "System control number"
  };
  private static final String[] DEFAULT_IDENTIFIER_TYPE_CODES = new String[] {
    "canceled-lccn",
    "control-number",
    "lccn",
    "other-standard-identifier",
    "system-control-number"
  };

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @Test
  @DisplayName("Get Collection: return Liquibase-seeded authority identifier types")
  void getAuthorityIdentifierTypes_positive_defaultEntitiesFound() throws Exception {
    doGet(authorityIdentifierTypesEndpoint() + "?query={cql}", "(cql.allRecords=1)sortby name/sort.ascending")
      .andExpect(jsonPath("totalRecords", is(DEFAULT_IDENTIFIER_TYPE_IDS.length)))
      .andExpect(jsonPath("identifierTypes[0].id", is(DEFAULT_IDENTIFIER_TYPE_IDS[0].toString())))
      .andExpect(jsonPath("identifierTypes[0].name", is(DEFAULT_IDENTIFIER_TYPE_NAMES[0])))
      .andExpect(jsonPath("identifierTypes[0].code", is(DEFAULT_IDENTIFIER_TYPE_CODES[0])))
      .andExpect(jsonPath("identifierTypes[0].source", is("folio")))
      .andExpect(jsonPath("identifierTypes[0].metadata", notNullValue()))
      .andExpect(jsonPath("identifierTypes[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("identifierTypes[0].metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("identifierTypes[0].metadata.createdByUserId", is(DEFAULT_USER_ID)))
      .andExpect(jsonPath("identifierTypes[0].metadata.updatedByUserId", is(DEFAULT_USER_ID)));
  }

  @Test
  @DisplayName("Get Collection: find one authority identifier type by CQL")
  void getAuthorityIdentifierTypes_positive_entityFoundByCqlQuery() throws Exception {
    doGet(authorityIdentifierTypesEndpoint() + "?query=({cql})", "id = " + DEFAULT_IDENTIFIER_TYPE_IDS[0])
      .andExpect(jsonPath("identifierTypes[0].id", is(DEFAULT_IDENTIFIER_TYPE_IDS[0].toString())))
      .andExpect(jsonPath("identifierTypes[0].name", is(DEFAULT_IDENTIFIER_TYPE_NAMES[0])))
      .andExpect(jsonPath("identifierTypes[0].code", is(DEFAULT_IDENTIFIER_TYPE_CODES[0])))
      .andExpect(jsonPath("identifierTypes[0].source", is("folio")))
      .andExpect(jsonPath("identifierTypes[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("identifierTypes[0].metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("identifierTypes[0].metadata.createdByUserId", is(DEFAULT_USER_ID)))
      .andExpect(jsonPath("identifierTypes[0].metadata.updatedByUserId", is(DEFAULT_USER_ID)))
      .andExpect(jsonPath("identifierTypes[1]").doesNotExist())
      .andExpect(jsonPath("totalRecords", is(1)));
  }

  @Test
  @DisplayName("Get Collection: return sorted identifier types with offset and limit")
  void getAuthorityIdentifierTypes_positive_entitiesSortedByNameAndLimitedWithOffset() throws Exception {
    var cqlQuery = "(cql.allRecords=1)sortby name/sort.descending";
    doGet(authorityIdentifierTypesEndpoint() + "?limit={limit}&offset={offset}&query={cql}", "1", "1", cqlQuery)
      .andExpect(jsonPath("identifierTypes[0].id", is(DEFAULT_IDENTIFIER_TYPE_IDS[3].toString())))
      .andExpect(jsonPath("identifierTypes[0].name", is(DEFAULT_IDENTIFIER_TYPE_NAMES[3])))
      .andExpect(jsonPath("identifierTypes[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("identifierTypes[0].metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("identifierTypes[0].metadata.createdByUserId", is(DEFAULT_USER_ID)))
      .andExpect(jsonPath("identifierTypes[0].metadata.updatedByUserId", is(DEFAULT_USER_ID)))
      .andExpect(jsonPath("identifierTypes[1]").doesNotExist())
      .andExpect(jsonPath("totalRecords", is(5)));
  }
}
