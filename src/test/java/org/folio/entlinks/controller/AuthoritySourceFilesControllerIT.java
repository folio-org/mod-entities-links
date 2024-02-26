package org.folio.entlinks.controller;

import static java.util.UUID.randomUUID;
import static org.folio.entlinks.domain.entity.AuthoritySourceFileSource.FOLIO;
import static org.folio.support.TestDataUtils.AuthorityTestData.authorityDto;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.authoritySourceFilesEndpoint;
import static org.folio.support.base.TestConstants.authoritySourceFilesHridEndpoint;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto.SourceEnum;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDtoHridManagement;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDtoHridManagement;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.entity.AuthoritySourceFileSource;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.base.IntegrationTestBase;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.AUTHORITY_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE,
  DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE}
)
class AuthoritySourceFilesControllerIT extends IntegrationTestBase {

  private static final String CREATED_DATE = "2021-10-28T06:31:31+05:00";

  private static final UUID[] SOURCE_FILE_IDS = new UUID[] {randomUUID(), randomUUID(), randomUUID()};
  private static final Integer[] SOURCE_FILE_CODE_IDS = new Integer[] {1, 2, 3};
  private static final String[] SOURCE_FILE_CODES = new String[] {"c", "co", "cod"};
  private static final String[] SOURCE_FILE_NAMES = new String[] {"name1", "name2", "name3"};
  private static final AuthoritySourceFileSource[] SOURCE_FILE_SOURCES = new AuthoritySourceFileSource[] {
    AuthoritySourceFileSource.LOCAL,
    AuthoritySourceFileSource.LOCAL,
    FOLIO
  };
  private static final String[] SOURCE_FILE_TYPES = new String[] {"type1", "type2", "type3"};
  private static final String[] SOURCE_FILE_URLS = new String[] {"base.url1", "baseUrl2", "base/url3"};

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  // Tests for Get Collection
  @Test
  @DisplayName("Get Collection: find no Authority Source Files")
  void getAuthoritySourceFiles_positive_noEntitiesFound() throws Exception {
    doGet(authoritySourceFilesEndpoint())
      .andExpect(jsonPath("totalRecords", is(0)));
  }

  @Test
  @DisplayName("Get Collection: find all Authority Source Files")
  void getCollection_positive_entitiesFound() throws Exception {
    var createdEntities = createAuthoritySourceTypes();

    tryGet(authoritySourceFilesEndpoint())
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(createdEntities.size())))
      .andExpect(jsonPath("authoritySourceFiles[1].hridManagement.startNumber", is(2)))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata", notNullValue()))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata.updatedByUserId", is(USER_ID)));
  }

  @ParameterizedTest
  @CsvSource({
    "0, 3, descending, name3",
    "1, 3, ascending, name2",
    "2, 2, descending, name1"
  })
  @DisplayName("Get Collection: return list of source files for the given limit and offset")
  void getCollection_positive_entitiesSortedByNameAndLimitedWithOffset(String offset, String limit, String sortOrder,
                                                                       String firstNoteTypeName) throws Exception {
    createAuthoritySourceTypes();

    var cqlQuery = "(cql.allRecords=1)sortby name/sort." + sortOrder;
    doGet(authoritySourceFilesEndpoint() + "?limit={l}&offset={o}&query={cql}", limit, offset, cqlQuery)
      .andExpect(jsonPath("authoritySourceFiles[0].name", is(firstNoteTypeName)))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("authoritySourceFiles[0].metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("totalRecords").value(3));
  }

  // Tests for Get By ID

  @Test
  @DisplayName("Get By ID: return note type by given ID")
  void getById_positive_foundByIdForExistingEntity() throws Exception {
    var sourceFile = prepareAuthoritySourceFile(0);
    createAuthoritySourceFile(sourceFile);

    doGet(authoritySourceFilesEndpoint(sourceFile.getId()))
      .andExpect(jsonPath("name", is(sourceFile.getName())))
      .andExpect(jsonPath("hridManagement.startNumber", is(1)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)));
  }

  @Test
  @DisplayName("Get By ID: return 404 for not existing entity")
  void getById_negative_noAuthoritySourceFileExistForGivenId() throws Exception {

    tryGet(authoritySourceFilesEndpoint(UUID.randomUUID()))
      .andExpect(status().isNotFound())
      .andExpect(exceptionMatch(AuthoritySourceFileNotFoundException.class))
      .andExpect(errorMessageMatch(containsString("was not found")));
  }

  @Test
  @DisplayName("Get By ID: return 400 when id is invalid")
  void getById_negative_IdIsInvalid() throws Exception {
    tryGet(authoritySourceFilesEndpoint() + "/{id}", "invalid-uuid")
      .andExpect(status().isBadRequest())
      .andExpect(exceptionMatch(MethodArgumentTypeMismatchException.class))
      .andExpect(errorMessageMatch(containsString(
        "Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'")));
  }

  // Tests for POST

  @Test
  @DisplayName("POST: create new Authority Source File")
  void createAuthoritySourceFile_positive_entityCreated() throws Exception {
    assumeTrue(databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID) == 0);

    var id = UUID.randomUUID();
    var dto = new AuthoritySourceFilePostDto()
      .id(id).name("name")
      // set max length (25) for the prefix/code
      .code("abcdefghijklmnopqrstuvwxy")
      .type("type").baseUrl("http://vocab.getty.edu/aat")
      .hridManagement(new AuthoritySourceFilePostDtoHridManagement().startNumber(10));
    var expectedSequenceName = "hrid_authority_local_file_abcdefghijklmnopqrstuvwxy_seq";

    tryPost(authoritySourceFilesEndpoint(), dto)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("name", is(dto.getName())))
      .andExpect(jsonPath("source", is("local")))
      .andExpect(jsonPath("codes", is(List.of(dto.getCode()))))
      .andExpect(jsonPath("baseUrl", is("http://vocab.getty.edu/aat/")))
      .andExpect(jsonPath("selectable", is(true)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)));

    assertEquals(1, databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
    assertEquals(expectedSequenceName, databaseHelper.queryAuthoritySourceFileSequenceName(TENANT_ID, id));
    assertEquals(dto.getHridManagement().getStartNumber(),
      databaseHelper.queryAuthoritySourceFileSequenceStartNumber(expectedSequenceName));
  }

  @Test
  @DisplayName("POST: create Authority Source File with existing ID")
  void createAuthoritySourceFile_negative_existsById() throws Exception {
    var entity = prepareAuthoritySourceFile(0);
    createAuthoritySourceFile(entity);

    var dto = new AuthoritySourceFilePostDto("name111", "code").id(entity.getId())
      .baseUrl("http://url").type("type");

    tryPost(authoritySourceFilesEndpoint(), dto)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is("Authority Source File with the given 'id' already exists.")))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));

    assertEquals(1,
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: create new Authority Source File with existed name")
  void createAuthoritySourceFile_negative_existedName() throws Exception {
    var createdEntities = createAuthoritySourceTypes();

    var dto = new AuthoritySourceFilePostDto(createdEntities.get(0).getName(), "code")
      .baseUrl("http://url").type("type");

    tryPost(authoritySourceFilesEndpoint(), dto)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is("Authority source file with the given 'name' already exists.")))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));

    assertEquals(createdEntities.size(),
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: create new Authority Source File with existed url")
  void createAuthoritySourceFile_negative_existedUrl() throws Exception {
    var createdEntities = createAuthoritySourceTypes();

    var dto = new AuthoritySourceFilePostDto("new name", "code")
      .baseUrl(createdEntities.get(0).getFullBaseUrl()).type("type");

    tryPost(authoritySourceFilesEndpoint(), dto)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is("Authority source file with the given 'baseUrl' already exists.")))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));

    assertEquals(createdEntities.size(),
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: create new Authority Source File with existed code")
  void createAuthoritySourceFile_negative_existedCode() throws Exception {
    var createdEntities = createAuthoritySourceTypes();

    var dto = new AuthoritySourceFilePostDto("new name", "co").baseUrl("http://new/url").type("type");

    tryPost(authoritySourceFilesEndpoint(), dto)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is("Authority source file with the given 'code' already exists.")))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));

    assertEquals(createdEntities.size(),
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: create new Authority Source File with invalid code value")
  void createAuthoritySourceFile_negative_invalidCodeValue() throws Exception {
    var dto = new AuthoritySourceFilePostDto().name("new name").baseUrl("http://new/url").type("type");

    for (var code : List.of("123", "0x123", "abc ", "$")) {
      dto.setCode(code);
      tryPost(authoritySourceFilesEndpoint(), dto)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(errorMessageMatch(
          containsString("Authority Source File prefix should be non-empty sequence of letters")))
        .andExpect(exceptionMatch(RequestBodyValidationException.class));
    }

    // cases which violates code length constraints
    for (var code : List.of("", "abcdefghijklmnopqrstuvwxyz")) {
      dto.setCode(code);
      tryPost(authoritySourceFilesEndpoint(), dto)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(errorMessageMatch(is(
          "size must be between 1 and 25")))
        .andExpect(exceptionMatch(MethodArgumentNotValidException.class));
    }

    assertEquals(0,
      databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
  }

  @Test
  @DisplayName("POST: return 422 for authority source file without name")
  void createAuthoritySourceFile_negative_entityWithoutNameNotCreated() throws Exception {
    var dto = new AuthoritySourceFilePostDto(null, "code").type("type");

    tryPost(authoritySourceFilesEndpoint(), dto)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(exceptionMatch(MethodArgumentNotValidException.class))
      .andExpect(jsonPath("$.errors.[0].parameters[0].key", is("name")))
      .andExpect(jsonPath("$.errors.[0].parameters[0].value", is("null")))
      .andExpect(errorMessageMatch(containsString("must not be null")));

  }

  // Tests for PATCH

  @Test
  @DisplayName("PATCH: partially update Authority Source File")
  void updateAuthoritySourceFilePartially_positive_entityGetUpdated() throws Exception {
    var createDto = new AuthoritySourceFilePostDto("name", "code").type("type").baseUrl("http://url");
    var hridStartNumber = 125;
    var partiallyModified = new AuthoritySourceFilePatchDto()
      .version(1)
      .name("name1")
      .type("type1")
      .baseUrl("https://url/")
      .selectable(false)
      .code("replacedCode")
      .hridManagement(new AuthoritySourceFilePatchDtoHridManagement().startNumber(hridStartNumber));

    var created = doPostAndReturn(authoritySourceFilesEndpoint(), createDto, AuthoritySourceFileDto.class);

    doPatch(authoritySourceFilesEndpoint(created.getId()), partiallyModified)
      .andExpect(status().isNoContent());

    doGet(authoritySourceFilesEndpoint(created.getId()))
      .andExpect(jsonPath("source", is(SourceEnum.LOCAL.getValue())))
      .andExpect(jsonPath("name", is(partiallyModified.getName())))
      .andExpect(jsonPath("type", is(partiallyModified.getType())))
      .andExpect(jsonPath("baseUrl", is(partiallyModified.getBaseUrl())))
      .andExpect(jsonPath("selectable", is(partiallyModified.getSelectable())))
      .andExpect(jsonPath("codes", is(List.of("replacedCode"))))
      .andExpect(jsonPath("_version", is(1)))
      .andExpect(jsonPath("hridManagement.startNumber", is(hridStartNumber)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)));
  }

  @Test
  @DisplayName("PATCH: partially update Authority Source File with reference in Authority")
  void updateAuthoritySourceFilePartially_positive_whenAuthorityReferenced() throws Exception {
    var createDto = new AuthoritySourceFilePostDto("name", "codeXXX").type("type").baseUrl("http://url");
    var partiallyModified = new AuthoritySourceFilePatchDto()
      .version(1)
      .name("name1")
      .type("type1")
      .selectable(false)
      .baseUrl("https://url.upd/");

    var created = doPostAndReturn(authoritySourceFilesEndpoint(), createDto, AuthoritySourceFileDto.class);
    var authorityPostDto = authorityDto(0, 0);
    authorityPostDto.setSourceFileId(created.getId());
    doPost(authorityEndpoint(), authorityPostDto, AuthorityDto.class)
        .andExpect(status().isCreated());

    doPatch(authoritySourceFilesEndpoint(created.getId()), partiallyModified)
      .andExpect(status().isNoContent());

    var content = doGet(authoritySourceFilesEndpoint(created.getId()))
      .andExpect(jsonPath("source", is(SourceEnum.LOCAL.getValue())))
      .andExpect(jsonPath("name", is(partiallyModified.getName())))
      .andExpect(jsonPath("type", is(partiallyModified.getType())))
      .andExpect(jsonPath("baseUrl", is(partiallyModified.getBaseUrl())))
      .andExpect(jsonPath("selectable", is(partiallyModified.getSelectable())))
      .andExpect(jsonPath("codes", hasSize(1)))
      .andExpect(jsonPath("_version", is(1)))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
      .andReturn().getResponse().getContentAsString();
    var resultDto = objectMapper.readValue(content, AuthoritySourceFileDto.class);

    assertThat(new HashSet<>(resultDto.getCodes()), equalTo(new HashSet<>(created.getCodes())));
  }

  @Test
  @DisplayName("PATCH: update of Authority Source File with old version")
  void updateWithOldVersion_negative_shouldReturnOptimisticLockingError() throws Exception {
    var dto = new AuthoritySourceFilePostDto("name", "codee").type("type").baseUrl("http://url");
    var created = doPostAndReturn(authoritySourceFilesEndpoint(), dto, AuthoritySourceFileDto.class);

    var patchDto = new AuthoritySourceFilePatchDto();
    patchDto.setVersion(0);
    doPatch(authoritySourceFilesEndpoint(created.getId()), patchDto)
        .andExpect(status().isNoContent());

    var expectedError = String.format("Cannot update record %s because it has been changed (optimistic locking): "
            + "Stored _version is %d, _version of request is %d", created.getId().toString(),
        1, 0);
    tryPatch(authoritySourceFilesEndpoint(created.getId()), patchDto)
        .andExpect(status().isConflict())
        .andExpect(errorMessageMatch(is(expectedError)))
        .andExpect(exceptionMatch(OptimisticLockingException.class));
  }

  // Tests for DELETE

  @Test
  @DisplayName("DELETE: Should delete existing authority source file")
  void deleteAuthoritySourceFile_positive_deleteExistingEntity() {
    var id = UUID.randomUUID();
    var code = "abc";
    var startNumber = 1;
    var dto = new AuthoritySourceFilePostDto()
        .id(id)
        .name("name")
        .code(code)
        .hridManagement(new AuthoritySourceFilePostDtoHridManagement().startNumber(startNumber));

    doPost(authoritySourceFilesEndpoint(), dto);
    doDelete(authoritySourceFilesEndpoint(id));

    assertEquals(0, databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_TABLE, TENANT_ID));
    assertEquals(0, databaseHelper.countRows(DatabaseHelper.AUTHORITY_SOURCE_FILE_CODE_TABLE, TENANT_ID));
    var sequenceName = String.format("hrid_authority_local_file_%s_seq", code);
    assertNull(databaseHelper.queryAuthoritySourceFileSequenceStartNumber(sequenceName));
  }

  @Test
  @DisplayName("DELETE: Folio Authority Source File cannot be deleted")
  void deleteAuthority_negative_folioType() throws Exception {
    var entity = prepareFolioSourceFile(0);
    createAuthoritySourceFile(entity);

    tryDelete(authoritySourceFilesEndpoint(entity.getId()))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(exceptionMatch(RequestBodyValidationException.class))
      .andExpect(errorMessageMatch(containsString(
        "Cannot delete Authority source file with source 'folio'")));
  }

  @Test
  @DisplayName("DELETE: Return 404 for non-existing entity")
  void deleteAuthoritySourceFile_negative_entityNotFound() throws Exception {

    tryDelete(authoritySourceFilesEndpoint(UUID.randomUUID())).andExpect(status().isNotFound())
      .andExpect(exceptionMatch(AuthoritySourceFileNotFoundException.class))
      .andExpect(errorMessageMatch(containsString("was not found")));
  }

  @Test
  @DisplayName("DELETE: Return 400 for invalid request id")
  void deleteAuthoritySourceFile_negative_invalidProvidedRequestId() throws Exception {

    tryDelete(authoritySourceFilesEndpoint() + "/{id}", "invalid-uuid").andExpect(status().isBadRequest())
      .andExpect(exceptionMatch(MethodArgumentTypeMismatchException.class))
      .andExpect(errorMessageMatch(containsString(
        "Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'")));
  }

  @Test
  @DisplayName("POST: Generate next HR ID for the specified Authority Source File")
  void newAuthoritySourceFileNextHrid_positive_hridReturned() throws Exception {
    // Arrange
    var id = UUID.randomUUID();
    var code = "abc";
    var startNumber = 10;
    var dto = new AuthoritySourceFilePostDto()
      .id(id).name("name")
      .code(code)
      .hridManagement(new AuthoritySourceFilePostDtoHridManagement().startNumber(startNumber));

    doPost(authoritySourceFilesEndpoint(), dto);

    // Act & Assert
    for (int i = 0; i < 5; i++) {
      tryPost(authoritySourceFilesHridEndpoint(id), null)
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("id", is(id.toString())))
        .andExpect(jsonPath("hrid", is(code + (startNumber + i))));
    }

  }

  private List<AuthoritySourceFile> createAuthoritySourceTypes() {
    var sourceFile1 = prepareAuthoritySourceFile(0);
    var sourceFile2 = prepareAuthoritySourceFile(1);
    var sourceFile3 = prepareAuthoritySourceFile(2);

    createAuthoritySourceFile(sourceFile1);
    createAuthoritySourceFile(sourceFile2);
    createAuthoritySourceFile(sourceFile3);

    return List.of(sourceFile1, sourceFile2, sourceFile3);
  }

  private AuthoritySourceFile prepareAuthoritySourceFile(int i) {
    var entity = new AuthoritySourceFile();
    entity.setId(SOURCE_FILE_IDS[i]);
    entity.setName(SOURCE_FILE_NAMES[i]);
    entity.setSource(SOURCE_FILE_SOURCES[i]);
    entity.setType(SOURCE_FILE_TYPES[i]);
    entity.setBaseUrlProtocol(i % 2 == 0 ? "https" : "http");
    entity.setBaseUrl(SOURCE_FILE_URLS[i] + "/");
    entity.setHridStartNumber(i + 1);

    var code = prepareAuthoritySourceFileCode(i);
    entity.setCreatedDate(Timestamp.from(Instant.parse(CREATED_DATE)));
    entity.setCreatedByUserId(UUID.fromString(USER_ID));
    entity.setUpdatedDate(Timestamp.from(Instant.parse(CREATED_DATE)));
    entity.setUpdatedByUserId(UUID.fromString(USER_ID));
    entity.setAuthoritySourceFileCodes(Set.of(code));

    return entity;
  }

  private AuthoritySourceFile prepareFolioSourceFile(int sourceFileIdNum) {
    var entity = new AuthoritySourceFile();
    entity.setId(SOURCE_FILE_IDS[sourceFileIdNum]);
    entity.setName(SOURCE_FILE_NAMES[sourceFileIdNum]);
    entity.setSource(FOLIO);
    entity.setType(SOURCE_FILE_TYPES[sourceFileIdNum]);
    entity.setBaseUrl(SOURCE_FILE_URLS[sourceFileIdNum] + "/");

    var code = prepareAuthoritySourceFileCode(sourceFileIdNum);
    entity.setCreatedDate(Timestamp.from(Instant.parse(CREATED_DATE)));
    entity.setCreatedByUserId(UUID.fromString(USER_ID));
    entity.setUpdatedDate(Timestamp.from(Instant.parse(CREATED_DATE)));
    entity.setUpdatedByUserId(UUID.fromString(USER_ID));
    entity.addCode(code);

    return entity;
  }

  private void createAuthoritySourceFile(AuthoritySourceFile entity) {
    databaseHelper.saveAuthoritySourceFile(TENANT_ID, entity);
    createAuthoritySourceFileCode(entity);
  }

  private AuthoritySourceFileCode prepareAuthoritySourceFileCode(int i) {
    var code = new AuthoritySourceFileCode();
    code.setId(SOURCE_FILE_CODE_IDS[i]);
    code.setCode(SOURCE_FILE_CODES[i]);
    return code;
  }

  private void createAuthoritySourceFileCode(AuthoritySourceFile entity) {
    for (var code : entity.getAuthoritySourceFileCodes()) {
      databaseHelper.saveAuthoritySourceFileCode(TENANT_ID, entity.getId(), code);
    }
  }

  private ResultMatcher errorMessageMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].message", errorMessageMatcher);
  }
}
