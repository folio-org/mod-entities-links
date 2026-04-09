package org.folio.entlinks.controller;

import static org.folio.support.base.TestConstants.authorityHeadingTypesEndpoint;
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
class AuthorityHeadingTypesControllerIT extends IntegrationTestBase {

  private static final int DEFAULT_HEADING_TYPES_COUNT = 34;
  private static final UUID FIRST_SORTED_HEADING_TYPE_ID =
    UUID.fromString("7654d533-14c4-509d-8336-a4dd76187990");
  private static final UUID SECOND_SORTED_HEADING_TYPE_ID =
    UUID.fromString("9bee2c50-6598-511d-a6b1-4970d44c3751");
  private static final UUID FILTERED_HEADING_TYPE_ID =
    UUID.fromString("0802e085-468c-5f1c-8d6b-c5763b227168");

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @Test
  @DisplayName("Get Collection: return Liquibase-seeded authority heading types")
  void getAuthorityHeadingTypes_positive_defaultEntitiesFound() throws Exception {
    doGet(authorityHeadingTypesEndpoint() + "?query={cql}", "(cql.allRecords=1)sortby name/sort.ascending")
      .andExpect(jsonPath("totalRecords", is(DEFAULT_HEADING_TYPES_COUNT)))
      .andExpect(jsonPath("headingTypes[0].id", is(FIRST_SORTED_HEADING_TYPE_ID.toString())))
      .andExpect(jsonPath("headingTypes[0].name", is("Chronological subdivision")))
      .andExpect(jsonPath("headingTypes[0].code", is("chronSubdivision")))
      .andExpect(jsonPath("headingTypes[0].queryable", is(true)));
  }

  @Test
  @DisplayName("Get Collection: find one authority heading type by CQL")
  void getAuthorityHeadingTypes_positive_entityFoundByCqlQuery() throws Exception {
    doGet(authorityHeadingTypesEndpoint() + "?query=({cql})", "id = " + FILTERED_HEADING_TYPE_ID)
      .andExpect(jsonPath("headingTypes[0].id", is(FILTERED_HEADING_TYPE_ID.toString())))
      .andExpect(jsonPath("headingTypes[0].name", is("Personal name")))
      .andExpect(jsonPath("headingTypes[0].code", is("personalName")))
      .andExpect(jsonPath("headingTypes[0].queryable", is(true)))
      .andExpect(jsonPath("headingTypes[1]").doesNotExist())
      .andExpect(jsonPath("totalRecords", is(1)));
  }

  @Test
  @DisplayName("Get Collection: return sorted heading types with offset and limit")
  void getAuthorityHeadingTypes_positive_entitiesSortedAndLimitedWithOffset() throws Exception {
    var cqlQuery = "(cql.allRecords=1)sortby name/sort.ascending";
    doGet(authorityHeadingTypesEndpoint() + "?limit={limit}&offset={offset}&query={cql}", "1", "1", cqlQuery)
      .andExpect(jsonPath("headingTypes[0].id", is(SECOND_SORTED_HEADING_TYPE_ID.toString())))
      .andExpect(jsonPath("headingTypes[0].name", is("Chronological subdivision - Truncated")))
      .andExpect(jsonPath("headingTypes[0].code", is("chronSubdivisionTrunc")))
      .andExpect(jsonPath("headingTypes[0].queryable", is(false)))
      .andExpect(jsonPath("headingTypes[1]").doesNotExist())
      .andExpect(jsonPath("totalRecords", is(DEFAULT_HEADING_TYPES_COUNT)))
      .andExpect(jsonPath("headingTypes[0]", notNullValue()));
  }
}
