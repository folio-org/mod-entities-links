package org.folio.entlinks.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.entlinks.domain.entity.AuthoritySourceFileSource;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.testing.extension.EnablePostgres;
import org.folio.spring.testing.type.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
@JdbcTest
@EnablePostgres
@AutoConfigureJson
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthoritySourceFileJdbcRepositoryIT {

  private @MockitoSpyBean JdbcTemplate jdbcTemplate;
  private @MockitoBean FolioExecutionContext context;
  private AuthoritySourceFileJdbcRepository repository;

  @BeforeEach
  void setUp() {
    repository = spy(new AuthoritySourceFileJdbcRepository(jdbcTemplate, context));
    when(context.getFolioModuleMetadata()).thenReturn(new FolioModuleMetadata() {
      @Override
      public String getModuleName() {
        return null;
      }

      @Override
      public String getDBSchemaName(String tenantId) {
        return "public";
      }
    });
    when(context.getTenantId()).thenReturn(TENANT_ID);
  }

  @Test
  void testInsert() {
    var entity = getAuthoritySourceFile();
    repository.insert(entity);

    assertSourceFile(entity);
    assertSourceFileCode(entity.getId(), new String[] {"n", "nb"});
  }

  @Test
  void testUpdate() {
    var entity = getAuthoritySourceFile();
    repository.insert(entity);

    var newCodes = new HashSet<>(entity.getAuthoritySourceFileCodes());
    newCodes.add(new AuthoritySourceFileCode(3, null, "nc"));
    entity.setAuthoritySourceFileCodes(newCodes);
    entity.setHridStartNumber(2);
    entity.setBaseUrl("newBaseUrl");
    entity.setBaseUrlProtocol("https");
    entity.setName("newName");
    entity.setSource(AuthoritySourceFileSource.LOCAL);
    entity.setType("newType");
    entity.setSelectable(false);
    entity.setVersion(2);

    repository.update(entity, 0);

    assertSourceFile(entity);
    assertSourceFileCode(entity.getId(), new String[] {"n", "nb", "nc"});
  }

  @Test
  void testDelete() {
    var entity = getAuthoritySourceFile();
    repository.insert(entity);

    repository.delete(entity.getId());

    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM authority_source_file WHERE id = ?",
      Integer.class, entity.getId()))
      .isZero();
    assertSourceFileCode(entity.getId(), new String[] { });
  }

  private void assertSourceFileCode(UUID sourceFileId, String[] expectedCodes) {
    var codes = jdbcTemplate
      .queryForList("SELECT code FROM authority_source_file_code WHERE authority_source_file_id = ?",
        String.class, sourceFileId);
    assertThat(codes)
      .hasSize(expectedCodes.length)
      .containsExactlyInAnyOrder(expectedCodes);
  }

  private void assertSourceFile(AuthoritySourceFile expectedEntity) {
    jdbcTemplate.query("SELECT * FROM authority_source_file WHERE id = ?",
      rs -> {
        assertThat(rs.getString("name")).isEqualTo(expectedEntity.getName());
        assertThat(rs.getString("source")).isEqualTo(expectedEntity.getSource().name());
        assertThat(rs.getString("type")).isEqualTo(expectedEntity.getType());
        assertThat(rs.getString("base_url_protocol")).isEqualTo(expectedEntity.getBaseUrlProtocol());
        assertThat(rs.getString("base_url")).isEqualTo(expectedEntity.getBaseUrl());
        assertThat(rs.getInt("hrid_start_number")).isEqualTo(expectedEntity.getHridStartNumber());
        assertThat(rs.getBoolean("selectable")).isEqualTo(expectedEntity.isSelectable());
      }, expectedEntity.getId());
  }

  private AuthoritySourceFile getAuthoritySourceFile() {
    var entity =
      new AuthoritySourceFile(UUID.randomUUID(), "Test", "folio", "http", "baseUrl", AuthoritySourceFileSource.FOLIO,
        Set.of(
          new AuthoritySourceFileCode(1, null, "n"),
          new AuthoritySourceFileCode(2, null, "nb")
        ),
        null, true, 1, true, 1);
    entity.setCreatedByUserId(UUID.randomUUID());
    entity.setUpdatedByUserId(UUID.randomUUID());
    entity.setCreatedDate(Timestamp.from(Instant.now()));
    entity.setUpdatedDate(Timestamp.from(Instant.now()));
    return entity;
  }
}
