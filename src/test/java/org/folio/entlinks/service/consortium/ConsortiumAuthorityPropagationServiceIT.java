package org.folio.entlinks.service.consortium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.folio.entlinks.service.consortium.ConsortiumAuthorityPropagationServiceIT.COLLEGE_TENANT_ID;
import static org.folio.entlinks.service.consortium.ConsortiumAuthorityPropagationServiceIT.UNIVERSITY_TENANT_ID;
import static org.folio.entlinks.service.settings.TenantSetting.ARCHIVES_EXPIRATION_PERIOD;
import static org.folio.support.DatabaseHelper.AUTHORITY_ARCHIVE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_DATA_STAT_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_NOTE_TYPE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_TABLE;
import static org.folio.support.base.TestConstants.CENTRAL_TENANT_ID;
import static org.folio.support.base.TestConstants.CONSORTIUM_SOURCE_PREFIX;
import static org.folio.support.base.TestConstants.authorityConfigEndpoint;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.authorityExpireEndpoint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.awaitility.Durations;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.base.IntegrationTestBase;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@IntegrationTest
@DatabaseCleanup(tables = {AUTHORITY_DATA_STAT_TABLE, AUTHORITY_ARCHIVE_TABLE, AUTHORITY_TABLE,
                           AUTHORITY_SOURCE_FILE_CODE_TABLE, AUTHORITY_SOURCE_FILE_TABLE, AUTHORITY_NOTE_TYPE_TABLE},
                 tenants = {CENTRAL_TENANT_ID, COLLEGE_TENANT_ID, UNIVERSITY_TENANT_ID})
class ConsortiumAuthorityPropagationServiceIT extends IntegrationTestBase {

  public static final String COLLEGE_TENANT_ID = "college";
  public static final String UNIVERSITY_TENANT_ID = "university";

  @BeforeAll
  static void beforeAll() {
    setUpConsortium(CENTRAL_TENANT_ID, List.of(COLLEGE_TENANT_ID, UNIVERSITY_TENANT_ID), true);
  }

  @Test
  @SneakyThrows
  void testAuthorityCreatePropagation() {
    var authorityId = UUID.randomUUID();
    var dto = getAuthorityDto(authorityId);
    doPost(authorityEndpoint(), dto, tenantHeaders(CENTRAL_TENANT_ID));
    var centralAuthority = requestAuthority(CENTRAL_TENANT_ID, authorityId);
    assertThat(centralAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(authorityId, dto.getSource(), dto.getNaturalId(), dto.getPersonalName());

    awaitUntilAsserted(() ->
      assertEquals(1, databaseHelper.countRows(AUTHORITY_TABLE, COLLEGE_TENANT_ID)));
    var collegeAuthority = requestAuthority(COLLEGE_TENANT_ID, authorityId);
    assertThat(collegeAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(authorityId, CONSORTIUM_SOURCE_PREFIX + dto.getSource(), dto.getNaturalId(),
        dto.getPersonalName());

    awaitUntilAsserted(() ->
      assertEquals(1, databaseHelper.countRows(AUTHORITY_TABLE, UNIVERSITY_TENANT_ID)));
    var universityAuthority = requestAuthority(UNIVERSITY_TENANT_ID, authorityId);
    assertThat(universityAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(authorityId, CONSORTIUM_SOURCE_PREFIX + dto.getSource(), dto.getNaturalId(),
        dto.getPersonalName());
  }

  @Test
  @SneakyThrows
  void testAuthorityDeletePropagation() {
    var authorityId = UUID.randomUUID();
    var dto = getAuthorityDto(authorityId);
    doPost(authorityEndpoint(), dto, tenantHeaders(CENTRAL_TENANT_ID));

    // wait until authority is created
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authorityId), tenantHeaders(CENTRAL_TENANT_ID))
            .andExpect(status().isOk())
    );
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authorityId), tenantHeaders(COLLEGE_TENANT_ID))
            .andExpect(status().isOk())
    );
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authorityId), tenantHeaders(UNIVERSITY_TENANT_ID))
            .andExpect(status().isOk())
    );

    // delete authority in central tenant
    doDelete(authorityEndpoint(authorityId), tenantHeaders(CENTRAL_TENANT_ID));

    // verify authority is deleted in all tenants
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authorityId), tenantHeaders(CENTRAL_TENANT_ID))
            .andExpect(status().isNotFound())
    );
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authorityId), tenantHeaders(COLLEGE_TENANT_ID))
            .andExpect(status().isNotFound())
    );
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authorityId), tenantHeaders(UNIVERSITY_TENANT_ID))
            .andExpect(status().isNotFound())
    );
  }

  @Test
  @SneakyThrows
  void testAuthorityUpdatePropagation() {
    var authorityId = UUID.randomUUID();
    var dto = getAuthorityDto(authorityId);
    doPost(authorityEndpoint(), dto, tenantHeaders(CENTRAL_TENANT_ID));
    assertThat(requestAuthority(CENTRAL_TENANT_ID, authorityId)).isNotNull();
    awaitUntilAsserted(() -> assertNotNull(requestAuthority(COLLEGE_TENANT_ID, authorityId)));
    awaitUntilAsserted(() -> assertNotNull(requestAuthority(UNIVERSITY_TENANT_ID, authorityId)));
    doPut(authorityEndpoint(authorityId), dto.personalName("updated"), tenantHeaders(CENTRAL_TENANT_ID));

    var centralAuthority = requestAuthority(CENTRAL_TENANT_ID, authorityId);
    assertThat(centralAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(authorityId, dto.getSource(), dto.getNaturalId(), dto.getPersonalName());

    awaitUntilAsserted(() ->
      assertEquals(1, databaseHelper.countRowsWhere(AUTHORITY_TABLE, COLLEGE_TENANT_ID, "heading = 'updated'")));
    var collegeAuthority = requestAuthority(COLLEGE_TENANT_ID, authorityId);
    assertThat(collegeAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(authorityId, CONSORTIUM_SOURCE_PREFIX + dto.getSource(), dto.getNaturalId(),
        dto.getPersonalName());

    awaitUntilAsserted(() ->
      assertEquals(1, databaseHelper.countRowsWhere(AUTHORITY_TABLE, UNIVERSITY_TENANT_ID, "heading = 'updated'")));
    var universityAuthority = requestAuthority(UNIVERSITY_TENANT_ID, authorityId);
    assertThat(universityAuthority)
      .extracting(AuthorityDto::getId, AuthorityDto::getSource, AuthorityDto::getNaturalId,
        AuthorityDto::getPersonalName)
      .containsExactly(authorityId, CONSORTIUM_SOURCE_PREFIX + dto.getSource(), dto.getNaturalId(),
        dto.getPersonalName());
  }

  @Test
  @SneakyThrows
  void testAuthorityArchivePropagation() {
    var body = new SettingUpdateRequest().value(1);
    doPatch(authorityConfigEndpoint(ARCHIVES_EXPIRATION_PERIOD), body, tenantHeaders(CENTRAL_TENANT_ID));
    doPatch(authorityConfigEndpoint(ARCHIVES_EXPIRATION_PERIOD), body, tenantHeaders(COLLEGE_TENANT_ID));
    doPatch(authorityConfigEndpoint(ARCHIVES_EXPIRATION_PERIOD), body, tenantHeaders(UNIVERSITY_TENANT_ID));
    var authorityId = UUID.randomUUID();
    var dto = getAuthorityDto(authorityId);
    doPost(authorityEndpoint(), dto, tenantHeaders(CENTRAL_TENANT_ID));
    assertThat(requestAuthority(CENTRAL_TENANT_ID, authorityId)).isNotNull();

    // wait until authority is created
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authorityId), tenantHeaders(CENTRAL_TENANT_ID))
            .andExpect(status().isOk())
    );
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authorityId), tenantHeaders(COLLEGE_TENANT_ID))
            .andExpect(status().isOk())
    );
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authorityId), tenantHeaders(UNIVERSITY_TENANT_ID))
            .andExpect(status().isOk())
    );

    doDelete(authorityEndpoint(authorityId), tenantHeaders(CENTRAL_TENANT_ID));
    tryGet(authorityEndpoint(authorityId), tenantHeaders(CENTRAL_TENANT_ID)).andExpect(status().isNotFound());

    awaitUntilAsserted(() ->
        assertEquals(authorityId.toString(), databaseHelper.getAuthorityArchive(CENTRAL_TENANT_ID, authorityId)));
    awaitUntilAsserted(() ->
        assertEquals(authorityId.toString(), databaseHelper.getAuthorityArchive(COLLEGE_TENANT_ID, authorityId)));
    awaitUntilAsserted(() ->
        assertEquals(authorityId.toString(), databaseHelper.getAuthorityArchive(UNIVERSITY_TENANT_ID, authorityId)));

    var dateInPast = Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS));
    databaseHelper.updateAuthorityArchiveUpdateDate(CENTRAL_TENANT_ID, authorityId, dateInPast);
    databaseHelper.updateAuthorityArchiveUpdateDate(COLLEGE_TENANT_ID, authorityId, dateInPast);
    databaseHelper.updateAuthorityArchiveUpdateDate(UNIVERSITY_TENANT_ID, authorityId, dateInPast);

    doPost(authorityExpireEndpoint(), null, tenantHeaders(CENTRAL_TENANT_ID));

    awaitUntilAsserted(() -> assertNull(databaseHelper.getAuthorityArchive(CENTRAL_TENANT_ID, authorityId)));
    awaitUntilAsserted(() -> assertNull(databaseHelper.getAuthorityArchive(COLLEGE_TENANT_ID, authorityId)));
    awaitUntilAsserted(() -> assertNull(databaseHelper.getAuthorityArchive(UNIVERSITY_TENANT_ID, authorityId)));
  }

  private AuthorityDto getAuthorityDto(UUID id) {
    return new AuthorityDto()
      .id(id)
      .version(0)
      .source("MARC")
      .naturalId("ns12345")
      .personalName("Nikola Tesla");
  }

  private AuthorityDto requestAuthority(String tenantId, UUID id)
    throws UnsupportedEncodingException, JsonProcessingException {
    var response = doGet(authorityEndpoint(id), tenantHeaders(tenantId)).andReturn()
      .getResponse()
      .getContentAsString();
    return objectMapper.readValue(response, AuthorityDto.class);
  }
}
