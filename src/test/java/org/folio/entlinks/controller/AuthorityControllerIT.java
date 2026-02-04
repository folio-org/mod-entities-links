package org.folio.entlinks.controller;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_ID;
import static org.folio.entlinks.integration.dto.event.DomainEventType.CREATE;
import static org.folio.entlinks.integration.dto.event.DomainEventType.DELETE;
import static org.folio.entlinks.integration.dto.event.DomainEventType.UPDATE;
import static org.folio.entlinks.service.settings.TenantSetting.ARCHIVES_EXPIRATION_PERIOD;
import static org.folio.support.DatabaseHelper.AUTHORITY_ARCHIVE_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_DATA_STAT_TABLE;
import static org.folio.support.DatabaseHelper.AUTHORITY_TABLE;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.AuthorityTestData.CREATED_DATE;
import static org.folio.support.TestDataUtils.AuthorityTestData.UPDATED_DATE;
import static org.folio.support.TestDataUtils.AuthorityTestData.authority;
import static org.folio.support.TestDataUtils.AuthorityTestData.authorityArchive;
import static org.folio.support.TestDataUtils.AuthorityTestData.authorityDto;
import static org.folio.support.TestDataUtils.AuthorityTestData.authoritySourceFile;
import static org.folio.support.TestDataUtils.modifiedAuthorityDto;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.UPDATER_USER_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.folio.support.base.TestConstants.authorityConfigEndpoint;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.authorityExpireEndpoint;
import static org.folio.support.base.TestConstants.authoritySourceFilesEndpoint;
import static org.folio.support.base.TestConstants.authorityTopic;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Durations;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityDtoCollection;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.exception.AuthoritiesRequestNotSupportedMediaTypeException;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.integration.dto.event.AuthorityDeleteEventSubType;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.base.IntegrationTestBase;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.MediaType;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE,
  AUTHORITY_DATA_STAT_TABLE,
  AUTHORITY_TABLE,
  AUTHORITY_ARCHIVE_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE})
class AuthorityControllerIT extends IntegrationTestBase {

  private KafkaMessageListenerContainer<String, AuthorityDomainEvent> container;
  private BlockingQueue<ConsumerRecord<String, AuthorityDomainEvent>> consumerRecords;

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @BeforeEach
  void setUp(@Autowired KafkaProperties kafkaProperties) {
    consumerRecords = new LinkedBlockingQueue<>();
    container =
      createAndStartTestConsumer(authorityTopic(), consumerRecords, kafkaProperties, AuthorityDomainEvent.class);
  }

  @AfterEach
  void tearDown() {
    consumerRecords.clear();
    container.stop();
  }

  // Tests for Get Collection

  @Test
  @DisplayName("Get Collection: find no Authority")
  void getCollection_positive_noEntitiesFound() throws Exception {
    doGet(authorityEndpoint())
      .andExpect(jsonPath("totalRecords", is(0)));
  }

  @Test
  @DisplayName("Get Collection: find all Authority entities")
  void getCollection_positive_entitiesFound() throws Exception {
    var createdEntities = createAuthorities();

    tryGet(authorityEndpoint())
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(createdEntities.size())))
      .andExpect(jsonPath("authorities[0].metadata", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("authorities[0].metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.updatedByUserId", is(USER_ID)));
  }

  @Test
  @DisplayName("Get Collection: find all Authority entities IDs")
  void getCollectionOfIdsOnly_positive_authoritiesFound() throws Exception {
    var createdEntities = createAuthorities();

    var content = tryGet(authorityEndpoint() + "?idOnly={io}", true)
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(createdEntities.size())))
      .andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(content, AuthorityDtoCollection.class);
    var expectedCollection = new AuthorityDtoCollection(
      createdEntities.stream().map(authority -> new AuthorityDto().id(authority.getId())).toList(),
      createdEntities.size()
    );

    assertEquals(expectedCollection.getTotalRecords(), collection.getTotalRecords());
    assertEquals(new HashSet<>(expectedCollection.getAuthorities()), new HashSet<>(collection.getAuthorities()));
  }

  @Test
  @DisplayName("Get Collection: find all Authority Archives")
  void getCollection_positive_authorityArchivesFound() throws Exception {
    var createdEntities = createAuthorityArchives();

    tryGet(authorityEndpoint() + "?deleted={d}", true)
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(createdEntities.size())))
      .andExpect(jsonPath("authorities[0].metadata", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("authorities[0].metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.updatedByUserId", is(USER_ID)));
  }

  @Test
  @DisplayName("Get Collection: find all Authority Archives IDs with content-type text/plain and application/json")
  void getCollectionOfIdsOnly_positive_authorityArchivesFound() throws Exception {
    var createdEntities = createAuthorityArchives();
    var expectedCollection = new AuthorityDtoCollection(
      createdEntities.stream().map(archive -> new AuthorityDto().id(archive.getId())).toList(),
      createdEntities.size()
    );

    var content = tryGet(authorityEndpoint() + "?deleted={d}&idOnly={io}", true, true)
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(createdEntities.size())))
      .andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(content, AuthorityDtoCollection.class);

    assertEquals(new HashSet<>(expectedCollection.getAuthorities()), new HashSet<>(collection.getAuthorities()));
    assertEquals(expectedCollection.getTotalRecords(), collection.getTotalRecords());

    var headers = defaultHeaders();
    headers.setAccept(List.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON));
    var expectedContent = createdEntities.stream()
      .map(AuthorityArchive::getId)
      .map(UUID::toString)
      .collect(Collectors.joining("\n"));

    tryGet(authorityEndpoint() + "?deleted={d}&idOnly={io}", headers, true, true)
      .andExpect(status().isOk())
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_PLAIN_VALUE))
      .andExpect(MockMvcResultMatchers.content().string(expectedContent));
  }

  @Test
  @DisplayName("Get Collection: retrieve all Authorities and Archives in plain text")
  void getCollection_negative_authoritiesAndArchivesNotRetrievableInPlainText() throws Exception {
    createAuthorityArchives();
    var headers = defaultHeaders();
    headers.setAccept(List.of(MediaType.TEXT_PLAIN));

    tryGet(authorityEndpoint() + "?deleted={d}", headers, true)
      .andExpect(status().isBadRequest())
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_PLAIN_VALUE))
      .andExpect(MockMvcResultMatchers.content()
        .string(Matchers.containsString(AuthorityController.RETRIEVE_COLLECTION_INVALID_ACCEPT_MESSAGE)))
      .andExpect(exceptionMatch(AuthoritiesRequestNotSupportedMediaTypeException.class));

    tryGet(authorityEndpoint() + "?deleted={d}", headers, false)
      .andExpect(status().isBadRequest())
      .andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_PLAIN_VALUE))
      .andExpect(MockMvcResultMatchers.content()
        .string(Matchers.containsString(AuthorityController.RETRIEVE_COLLECTION_INVALID_ACCEPT_MESSAGE)))
      .andExpect(exceptionMatch(AuthoritiesRequestNotSupportedMediaTypeException.class));
  }

  @ParameterizedTest
  @CsvSource({
    "0, 3, descending, source3",
    "1, 3, ascending, source2",
    "2, 2, descending, source1"
  })
  @DisplayName("Get Collection: return list of authorities for the given limit and offset")
  void getCollection_positive_entitiesSortedBySourceAndLimitedWithOffset(String offset, String limit, String sortOrder,
                                                                         String firstSourceName) throws Exception {
    createAuthorities();
    // the following two authorities should be filtered out and not included in the result because of deleted = true
    createSourceFile(1);
    var authority1 = authority(0, 1);
    authority1.setId(UUID.randomUUID());
    authority1.setDeleted(true);
    var authority2 = authority(0, 1);
    authority2.setId(UUID.randomUUID());
    authority2.setDeleted(true);
    databaseHelper.saveAuthority(TENANT_ID, authority1);
    databaseHelper.saveAuthority(TENANT_ID, authority2);

    var cqlQuery = "(cql.allRecords=1)sortby source/sort." + sortOrder;
    doGet(authorityEndpoint() + "?limit={l}&offset={o}&query={cql}", limit, offset, cqlQuery)
      .andExpect(jsonPath("authorities[0].source", is(firstSourceName)))
      .andExpect(jsonPath("authorities[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("authorities[0].metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("totalRecords").value(3));
  }

  @ParameterizedTest
  @MethodSource("authorityArchivesQueryProvider")
  @DisplayName("Get Collection: return list of authorities for the given query")
  void getCollection_positive_filterAuthoritiesByQuery(String query, String heading, int numberOfRecords)
    throws Exception {
    createSourceFile(0);
    createSourceFile(1);
    var authority1 = authority(0, 0);
    authority1.setCreatedDate(Timestamp.from(Instant.parse(CREATED_DATE).minus(5, ChronoUnit.DAYS)));
    authority1.setUpdatedDate(Timestamp.from(Instant.parse(UPDATED_DATE).minus(2, ChronoUnit.DAYS)));
    var authority2 = authority(1, 0);
    authority2.setCreatedDate(Timestamp.from(Instant.parse(CREATED_DATE).plus(2, ChronoUnit.DAYS)));
    var authority3 = authority(2, 1);
    authority3.setUpdatedDate(Timestamp.from(Instant.parse(UPDATED_DATE).plus(4, ChronoUnit.DAYS)));
    databaseHelper.saveAuthority(TENANT_ID, authority1);
    databaseHelper.saveAuthority(TENANT_ID, authority2);
    databaseHelper.saveAuthority(TENANT_ID, authority3);
    var authority4 = authority(3, 0);
    authority4.setAuthoritySourceFile(null);
    authority4.setUpdatedDate(Timestamp.from(Instant.parse(UPDATED_DATE).minus(2, ChronoUnit.DAYS)));
    databaseHelper.saveAuthority(TENANT_ID, authority4);

    // wait until authorities are created
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authority1.getId()), tenantHeaders(TENANT_ID))
            .andExpect(status().isOk())
    );
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authority2.getId()), tenantHeaders(TENANT_ID))
            .andExpect(status().isOk())
    );
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authority3.getId()), tenantHeaders(TENANT_ID))
            .andExpect(status().isOk())
    );
    await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE).untilAsserted(() ->
        tryGet(authorityEndpoint(authority4.getId()), tenantHeaders(TENANT_ID))
            .andExpect(status().isOk())
    );

    // query and filter authorities
    var cqlQuery = "(" + query + ")sortby createdDate";
    doGet(authorityEndpoint() + "?query={cql}", cqlQuery)
      .andExpect(jsonPath("authorities[0]." + heading, notNullValue()))
      .andExpect(jsonPath("totalRecords").value(numberOfRecords));
  }

  @ParameterizedTest
  @MethodSource("authorityArchivesQueryProvider")
  @DisplayName("Get Collection: return list of authority archives for the given query")
  void getCollection_positive_filterAuthorityArchivesByQuery(String query, String heading, int numberOfRecords)
    throws Exception {
    createSourceFile(0);
    createSourceFile(1);

    // query and filter authority archives
    var archive1 = authorityArchive(0, 0);
    archive1.setCreatedDate(Timestamp.from(Instant.parse(CREATED_DATE).minus(5, ChronoUnit.DAYS)));
    archive1.setUpdatedDate(Timestamp.from(Instant.parse(UPDATED_DATE).minus(2, ChronoUnit.DAYS)));
    var archive2 = authorityArchive(1, 0);
    archive2.setCreatedDate(Timestamp.from(Instant.parse(CREATED_DATE).plus(2, ChronoUnit.DAYS)));
    var archive3 = authorityArchive(2, 1);
    archive3.setUpdatedDate(Timestamp.from(Instant.parse(UPDATED_DATE).plus(4, ChronoUnit.DAYS)));
    databaseHelper.saveAuthorityArchive(TENANT_ID, archive1);
    databaseHelper.saveAuthorityArchive(TENANT_ID, archive2);
    databaseHelper.saveAuthorityArchive(TENANT_ID, archive3);
    var archive4 = authorityArchive(3, 0);
    archive4.setAuthoritySourceFile(null);
    archive4.setUpdatedDate(Timestamp.from(Instant.parse(UPDATED_DATE).minus(2, ChronoUnit.DAYS)));
    databaseHelper.saveAuthorityArchive(TENANT_ID, archive4);

    var cqlQuery = "(" + query + ")sortby createdDate";
    doGet(authorityEndpoint() + "?query={cql}&deleted=true", cqlQuery)
      .andExpect(jsonPath("authorities[0]." + heading, notNullValue()))
      .andExpect(jsonPath("totalRecords").value(numberOfRecords));
  }

  @Test
  @DisplayName("Get Collection: retrieve authorities by providing invalid query field name")
  void getCollection_negative_shouldNotFilterByQueryForIncorrectFilterField() throws Exception {
    createSourceFile(0);
    createAuthority(0, 0);

    var cqlQuery = "(cql.allRecords=1 and headingTypeTest=personalName)";
    tryGet(authorityEndpoint() + "?query={cql}", cqlQuery)
      .andExpect(status().isBadRequest())
      .andExpect(errorMessageMatch(is(
        "Could not resolve attribute 'headingTypeTest' of 'org.folio.entlinks.domain.entity.Authority'")))
      .andExpect(exceptionMatch(InvalidDataAccessApiUsageException.class));
  }

  @Test
  @DisplayName("Get By ID: return authority by given ID")
  void getById_positive_foundByIdForExistingEntity() throws Exception {
    createSourceFile(0);
    var authority = createAuthority(0, 0);

    doGet(authorityEndpoint(authority.getId()))
      .andExpect(jsonPath("source", is(authority.getSource())))
      .andExpect(jsonPath("naturalId", is(authority.getNaturalId())))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)));
  }

  // Tests for Get By ID

  @Test
  @DisplayName("POST: create new Authority with defined ID")
  void createAuthority_positive_entityCreatedWithProvidedId() throws Exception {
    assumeTrue(databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID) == 0);

    var dto = authorityDto(0, 0);
    var id = randomUUID();
    dto.setId(id);
    createSourceFile(0);

    var content = tryPost(authorityEndpoint(), dto)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("id", is(id.toString())))
      .andExpect(jsonPath("source", is(dto.getSource())))
      .andExpect(jsonPath("naturalId", is(dto.getNaturalId())))
      .andExpect(jsonPath("personalName", is(dto.getPersonalName())))
      .andExpect(jsonPath("sourceFileId", is(dto.getSourceFileId().toString())))
      .andExpect(jsonPath("_version", is(0)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
      .andReturn().getResponse().getContentAsString();

    var created = objectMapper.readValue(content, AuthorityDto.class);
    var event = getConsumedEvent();

    verifyConsumedAuthorityEvent(event, CREATE, created);
    assertEquals(1, databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID));
    assertEquals(dto.getNotes(), created.getNotes());
    assertEquals(dto.getIdentifiers(), created.getIdentifiers());
    assertEquals(dto.getSftPersonalName(), created.getSftPersonalName());
    assertEquals(dto.getSaftPersonalName(), created.getSaftPersonalName());
  }

  // Tests for POST

  @Test
  @DisplayName("POST: create new Authority without defined ID")
  void createAuthority_positive_entityCreatedWithNewId() throws Exception {
    assumeTrue(databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID) == 0);

    var dto = authorityDto(0, 0);
    createSourceFile(0);

    var content = tryPost(authorityEndpoint(), dto)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("id", notNullValue()))
      .andExpect(jsonPath("source", is(dto.getSource())))
      .andExpect(jsonPath("naturalId", is(dto.getNaturalId())))
      .andExpect(jsonPath("personalName", is(dto.getPersonalName())))
      .andExpect(jsonPath("sourceFileId", is(Objects.requireNonNull(dto.getSourceFileId()).toString())))
      .andExpect(jsonPath("_version", is(0)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
      .andReturn().getResponse().getContentAsString();

    var created = objectMapper.readValue(content, AuthorityDto.class);
    getConsumedEvent();

    assertEquals(1, databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID));
    assertEquals(dto.getNotes(), created.getNotes());
    assertEquals(dto.getIdentifiers(), created.getIdentifiers());
    assertEquals(dto.getSftPersonalName(), created.getSftPersonalName());
    assertEquals(dto.getSaftPersonalName(), created.getSaftPersonalName());
    assertEquals(dto.getSaftNarrowerTerm(), created.getSaftNarrowerTerm());
    assertEquals(dto.getSaftBroaderTerm(), created.getSaftBroaderTerm());
  }

  @Test
  @DisplayName("POST: create new Authority without Source File ID")
  void createAuthority_positive_entityWithoutSourceFileRelationShouldBeCreated() throws Exception {
    assumeTrue(databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID) == 0);

    var dto = authorityDto(0, 0);
    dto.setSourceFileId(null);

    tryPost(authorityEndpoint(), dto)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("source", is(dto.getSource())))
      .andExpect(jsonPath("naturalId", is(dto.getNaturalId())))
      .andExpect(jsonPath("personalName", is(dto.getPersonalName())))
      .andExpect(jsonPath("sourceFileId").doesNotExist())
      .andExpect(jsonPath("_version", is(0)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)));
    getConsumedEvent();

    assertEquals(1, databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: create new Authority with non-existing source file")
  void createAuthority_negative_notCreatedWithNonExistingSourceFile() throws Exception {
    assumeTrue(databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID) == 0);

    var dto = authorityDto(0, 0);
    var sourceFileId = randomUUID();
    dto.setSourceFileId(sourceFileId);

    tryPost(authorityEndpoint(), dto)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is("Authority Source File with the given 'id' does not exists.")))
      .andExpect(exceptionMatch(InvalidDataAccessApiUsageException.class));
  }

  @Test
  @DisplayName("POST: create new Authority with duplicate id")
  void createAuthority_negative_notCreatedWithDuplicatedId() throws Exception {
    assumeTrue(databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID) == 0);

    var dto = authorityDto(0, 0);
    dto.setId(randomUUID());
    dto.setSourceFileId(null);

    tryPost(authorityEndpoint(), dto).andExpect(status().isCreated());
    getConsumedEvent();

    tryPost(authorityEndpoint(), dto)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is(DUPLICATE_AUTHORITY_ID.getMessage())))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));
  }

  @Test
  @DisplayName("PUT: update existing Authority entity")
  void updateAuthority_positive_entityUpdated() throws Exception {
    getConsumedEvent();
    var dto = authorityDto(0, 0);
    createSourceFile(0);

    doPost(authorityEndpoint(), dto);
    getConsumedEvent();
    var expected = modifiedAuthorityDto(0, 0);

    var headers = defaultHeaders();
    headers.put(XOkapiHeaders.USER_ID, List.of(UPDATER_USER_ID));
    tryPut(authorityEndpoint(expected.getId()), expected, headers)
      .andExpect(status().isNoContent());

    var content = doGet(authorityEndpoint(expected.getId()))
      .andExpect(jsonPath("_version", is(1)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(UPDATER_USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
      .andReturn().getResponse().getContentAsString();

    var resultDto = objectMapper.readValue(content, AuthorityDto.class);
    assertAuthorityCollectionFields(expected, resultDto);

    var event = getConsumedEvent();
    awaitUntilAsserted(() -> assertEquals(1, databaseHelper.countRows(AUTHORITY_DATA_STAT_TABLE, TENANT_ID)));

    verifyConsumedAuthorityEvent(event, UPDATE, resultDto);
    assertThat(event).isNotNull();
    assertThat(event.value().getOldEntity())
      .usingRecursiveComparison()
      .ignoringFields(IGNORED_FIELDS_FOR_VERIFICATION)
      .isEqualTo(dto);
  }

  @Test
  @DisplayName("PUT: update Authority without any changes")
  void updateAuthorityWithoutModification_positive_entityUpdatedAndVersionIncreased() throws Exception {
    getConsumedEvent();
    var dto = authorityDto(0, 0);
    createSourceFile(0);

    doPost(authorityEndpoint(), dto);
    getConsumedEvent();
    var existingAsString = doGet(authorityEndpoint())
      .andExpect(jsonPath("authorities[0]._version", is(0)))
      .andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(existingAsString, AuthorityDtoCollection.class);
    var putDto = collection.getAuthorities().getFirst();

    tryPut(authorityEndpoint(putDto.getId()), putDto).andExpect(status().isNoContent());

    var content = doGet(authorityEndpoint(putDto.getId()))
      .andExpect(jsonPath("_version", is(1)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andReturn().getResponse().getContentAsString();
    var resultDto = objectMapper.readValue(content, AuthorityDto.class);

    assertNotNull(resultDto.getMetadata());
    assertNotNull(resultDto.getMetadata().getUpdatedDate());
    assertTrue(resultDto.getMetadata().getUpdatedDate()
      .isAfter(Objects.requireNonNull(putDto.getMetadata()).getUpdatedDate()));
  }

  // Tests for PUT

  @Test
  @DisplayName("PUT: update Authority with non-existing source file id")
  void updateAuthority_negative_notUpdatedWithNonExistingSourceFile() throws Exception {
    var dto = authorityDto(0, 0);
    createSourceFile(0);

    doPost(authorityEndpoint(), dto);
    getConsumedEvent();
    var existingAsString = doGet(authorityEndpoint()).andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(existingAsString, AuthorityDtoCollection.class);
    var expected = collection.getAuthorities().getFirst();
    var sourceFileId = randomUUID();
    expected.setSourceFileId(sourceFileId);

    tryPut(authorityEndpoint(expected.getId()), expected)
      .andExpect(status().isNotFound())
      .andExpect(errorMessageMatch(is("Authority Source File with ID [" + sourceFileId + "] was not found")))
      .andExpect(exceptionMatch(AuthoritySourceFileNotFoundException.class));
  }

  @Test
  @DisplayName("PUT: repeated update of Authority with old version")
  void repeatedUpdateWithOldVersion_negative_shouldReturnOptimisticLockingError() throws Exception {
    getConsumedEvent();
    var dto = authorityDto(0, 0);
    createSourceFile(0);

    doPost(authorityEndpoint(), dto);
    getConsumedEvent();
    var existingAsString = doGet(authorityEndpoint())
      .andExpect(jsonPath("authorities[0]._version", is(0)))
      .andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(existingAsString, AuthorityDtoCollection.class);
    var putDto = collection.getAuthorities().getFirst();
    var expectedError = String.format("Cannot update record %s because it has been changed (optimistic locking): "
                                      + "Stored _version is %d, _version of request is %d", putDto.getId().toString(),
      1, 0);

    tryPut(authorityEndpoint(putDto.getId()), putDto).andExpect(status().isNoContent());
    tryPut(authorityEndpoint(putDto.getId()), putDto)
      .andExpect(status().isConflict())
      .andExpect(errorMessageMatch(is(expectedError)))
      .andExpect(exceptionMatch(OptimisticLockingException.class));
  }

  @Test
  @DisplayName("PUT: return 404 for non-existing authority")
  void updateAuthority_negative_entityNotFound() throws Exception {
    var id = UUID.randomUUID();
    var dto = new AuthorityDto().id(id).naturalId(id.toString()).source("source");

    tryPut(authorityEndpoint(id), dto).andExpect(status().isNotFound())
      .andExpect(exceptionMatch(AuthorityNotFoundException.class))
      .andExpect(errorMessageMatch(containsString("was not found")));
  }

  @Test
  @DisplayName("DELETE: Should delete existing authority and put it into archive table")
  void deleteAuthority_positive_deleteExistingEntity() throws Exception {
    createSourceFile(0);
    var authority = createAuthority(0, 0);

    var contentAsString = doGet(authorityEndpoint(authority.getId())).andReturn().getResponse().getContentAsString();
    final var expectedDto = objectMapper.readValue(contentAsString, AuthorityDto.class);
    doDelete(authorityEndpoint(authority.getId()));
    var event = getConsumedEvent();
    assertThat(event).isNotNull();
    assertEquals(AuthorityDeleteEventSubType.SOFT_DELETE, event.value().getDeleteEventSubType());
    verifyConsumedAuthorityEvent(event, DELETE, expectedDto.version(1));

    awaitUntilAsserted(() ->
      assertEquals(1, databaseHelper.countRows(AUTHORITY_DATA_STAT_TABLE, TENANT_ID)));
    awaitUntilAsserted(() ->
      assertEquals(1, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, TENANT_ID,
        String.format("id = '%s' AND deleted = true", authority.getId()))));
    awaitUntilAsserted(() ->
      assertEquals(0, databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID)));
    tryGet(authorityEndpoint(authority.getId()))
      .andExpect(status().isNotFound())
      .andExpect(exceptionMatch(AuthorityNotFoundException.class));
  }

  @Test
  @DisplayName("DELETE: Should delete existing authority archives by retention in settings")
  void expireAuthorityArchives_positive_shouldExpireExistingArchives()
    throws JsonProcessingException, UnsupportedEncodingException {
    var body = new SettingUpdateRequest().value(1);
    doPatch(authorityConfigEndpoint(ARCHIVES_EXPIRATION_PERIOD), body, tenantHeaders(TENANT_ID));

    createSourceFile(0);
    var authority1 = createAuthority(0, 0);
    var authority2 = createAuthority(1, 0);
    final var content1 = doGet(authorityEndpoint(authority1.getId())).andReturn().getResponse().getContentAsString();
    final var content2 = doGet(authorityEndpoint(authority2.getId())).andReturn().getResponse().getContentAsString();

    doDelete(authorityEndpoint(authority1.getId()));
    getConsumedEvent();
    doDelete(authorityEndpoint(authority2.getId()));
    getConsumedEvent();
    awaitUntilAsserted(() ->
      assertEquals(2, databaseHelper.countRowsWhere(AUTHORITY_ARCHIVE_TABLE, TENANT_ID, "deleted = true")));
    awaitUntilAsserted(() -> assertEquals(0, databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID)));

    var dateInPast = Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS));
    databaseHelper.updateAuthorityArchiveUpdateDate(TENANT_ID, authority1.getId(), dateInPast);
    databaseHelper.updateAuthorityArchiveUpdateDate(TENANT_ID, authority2.getId(), dateInPast);

    verifyExpiredArchivesDeleted(authority1, content1, content2);
  }

  // Tests for DELETE

  @Test
  @DisplayName("DELETE: Return 404 for non-existing entity")
  void deleteAuthority_negative_entityNotFound() throws Exception {

    tryDelete(authorityEndpoint(UUID.randomUUID())).andExpect(status().isNotFound())
      .andExpect(exceptionMatch(AuthorityNotFoundException.class))
      .andExpect(errorMessageMatch(containsString("was not found")));
  }

  @Test
  @DisplayName("DELETE: Return 400 for invalid request id")
  void deleteAuthority_negative_invalidProvidedRequestId() throws Exception {

    tryDelete(authorityEndpoint() + "/{id}", "invalid-uuid").andExpect(status().isBadRequest())
      .andExpect(exceptionMatch(MethodArgumentTypeMismatchException.class))
      .andExpect(errorMessageMatch(containsString(
        "Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'")));
  }

  @Test
  @DisplayName("DELETE: Authority Source File cannot be deleted when it is linked by Authority")
  void deleteAuthoritySourceFile_negative_failDeletingSourceFileLinkedByAuthority() throws Exception {
    var dto = authorityDto(0, 0);
    createSourceFile(0);

    doPost(authorityEndpoint(), dto);
    getConsumedEvent();
    var existingAsString = doGet(authorityEndpoint())
      .andReturn().getResponse().getContentAsString();
    var collection = objectMapper.readValue(existingAsString, AuthorityDtoCollection.class);
    var expected = collection.getAuthorities().getFirst();

    assertEquals(dto.getSourceFileId(), expected.getSourceFileId());

    tryDelete(authoritySourceFilesEndpoint(expected.getSourceFileId()))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is("Unable to delete. Authority source file has referenced authorities")))
      .andExpect(exceptionMatch(RequestBodyValidationException.class));
  }

  private void verifyExpiredArchivesDeleted(Authority authority1, String content1, String content2)
    throws JsonProcessingException {
    doPost(authorityExpireEndpoint(), null);

    getConsumedEvent();
    var consumedEvent = getConsumedEvent();
    assertAll(() -> {
      assertNotNull(consumedEvent);
      assertNotNull(consumedEvent.value());
    });
    var content = authority1.getId().equals(consumedEvent.value().getId()) ? content1 : content2;
    var dto = objectMapper.readValue(content, AuthorityDto.class);
    dto.setVersion(dto.getVersion() + 1);

    verifyConsumedAuthorityEvent(consumedEvent, DELETE, dto);
    assertEquals(AuthorityDeleteEventSubType.HARD_DELETE, consumedEvent.value().getDeleteEventSubType());
    assertEquals(0, databaseHelper.countRows(AUTHORITY_ARCHIVE_TABLE, TENANT_ID));
  }

  private void assertAuthorityCollectionFields(AuthorityDto expected, AuthorityDto actual) {
    assertEquals(expected.getSource(), actual.getSource());
    assertEquals(expected.getNaturalId(), actual.getNaturalId());
    assertEquals(expected.getSourceFileId(), actual.getSourceFileId());
    assertEquals(expected.getPersonalName(), actual.getPersonalName());
    assertEquals(expected.getCorporateName(), actual.getCorporateName());
    assertEquals(expected.getNotes(), actual.getNotes());
    assertEquals(expected.getIdentifiers(), actual.getIdentifiers());
    assertEquals(expected.getSftPersonalName(), actual.getSftPersonalName());
    assertEquals(expected.getSaftPersonalName(), actual.getSaftPersonalName());
    assertEquals(expected.getSftCorporateName(), actual.getSftCorporateName());
    assertEquals(expected.getSaftCorporateName(), actual.getSaftCorporateName());
    assertEquals(expected.getSaftCorporateNameTrunc(), actual.getSaftCorporateNameTrunc());
    assertEquals(expected.getSaftNarrowerTerm(), actual.getSaftNarrowerTerm());
    assertEquals(expected.getSaftBroaderTerm(), actual.getSaftBroaderTerm());
  }

  private static List<Arguments> authorityArchivesQueryProvider() {
    return List.of(
      Arguments.of("headingType=personalName", "personalName", 1),
      Arguments.of("authoritySourceFile.id=51243be4-27cb-4d78-9206-c956299483b1", "personalName", 2),
      Arguments.of("authoritySourceFile.id=51243be4-27cb-4d78-9206-c956299483b1 and headingType=corporateName",
        "corporateName", 1),
      Arguments.of("authoritySourceFile.name=name2", "genreTerm", 1),
      Arguments.of("cql.allRecords=1 NOT authoritySourceFile=\"\"", "genreTerm", 1),
      Arguments.of("subjectHeadingCode=d NOT authoritySourceFile=\"\"", "genreTerm", 1),
      Arguments.of("subjectHeadingCode=d NOT authoritySourceFile.id=\"\"", "genreTerm", 1),
      Arguments.of("subjectHeadingCode=d NOT authoritySourceFile.name=\"\"", "genreTerm", 1),
      Arguments.of("createdDate>2021-10-25T12:00:00.0 and createdDate<=2021-10-30T12:00:00.0", "genreTerm", 3),
      Arguments.of("updatedDate>=2021-10-24T12:00:00.0 and updatedDate<=2021-10-28T12:00:00.0", "corporateName", 1),
      Arguments.of("authoritySourceFile.name=name1 and createdDate>2021-10-28T12:00:00.0", "corporateName", 1)
    );
  }

  private List<Authority> createAuthorities() {
    createSourceFile(0);
    var entity1 = createAuthority(0, 0);
    var entity2 = createAuthority(1, 0);
    var entity3 = createAuthority(2, 0);

    return List.of(entity1, entity2, entity3);
  }

  private List<AuthorityArchive> createAuthorityArchives() {
    createSourceFile(0);
    var entity1 = authorityArchive(0, 0);
    var entity2 = authorityArchive(1, 0);
    var entity3 = authorityArchive(2, 0);
    databaseHelper.saveAuthorityArchive(TENANT_ID, entity1);
    databaseHelper.saveAuthorityArchive(TENANT_ID, entity2);
    databaseHelper.saveAuthorityArchive(TENANT_ID, entity3);

    return List.of(entity1, entity2, entity3);
  }

  private Authority createAuthority(int authorityNum, int sourceFileNum) {
    var entity = authority(authorityNum, sourceFileNum);
    databaseHelper.saveAuthority(TENANT_ID, entity);
    return entity;
  }

  private AuthoritySourceFile createSourceFile(int sourceFileNum) {
    var entity = authoritySourceFile(sourceFileNum);
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, entity);

    entity.getAuthoritySourceFileCodes().forEach(code ->
      databaseHelper.saveAuthoritySourceFileCode(TENANT_ID, entity.getId(), code));
    return entity;
  }

  private ResultMatcher errorMessageMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].message", errorMessageMatcher);
  }

  @Nullable
  @SneakyThrows
  private ConsumerRecord<String, AuthorityDomainEvent> getConsumedEvent() {
    return consumerRecords.poll(10, TimeUnit.SECONDS);
  }
}
