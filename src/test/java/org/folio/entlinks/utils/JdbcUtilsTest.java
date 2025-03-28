package org.folio.entlinks.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class JdbcUtilsTest {

  private FolioExecutionContext context;

  @BeforeEach
  void setUp() {
    var metadata = mock(FolioModuleMetadata.class);
    context = mock(FolioExecutionContext.class);
    when(context.getFolioModuleMetadata()).thenReturn(metadata);
    when(context.getTenantId()).thenReturn("testTenant");
    when(metadata.getDBSchemaName("testTenant")).thenReturn("testSchema");
  }

  @Test
  void testGetSchemaName() {
    String schemaName = JdbcUtils.getSchemaName(context);
    assertEquals("testSchema", schemaName);
  }

  @Test
  void testGetFullPath() {
    String fullPath = JdbcUtils.getFullPath(context, "testTable");
    assertEquals("testSchema.testTable", fullPath);
  }

  @Test
  void testGetParamPlaceholderSingle() {
    String placeholder = JdbcUtils.getParamPlaceholder(3);
    assertEquals("?,?,?", placeholder);
  }

  @Test
  void testGetParamPlaceholderMultiple() {
    String placeholder = JdbcUtils.getParamPlaceholder(2, 3);
    assertEquals("(?,?,?),(?,?,?)", placeholder);
  }

  @Test
  void testGetParamPlaceholderInvalidSize() {
    assertThrows(IllegalArgumentException.class, () -> JdbcUtils.getParamPlaceholder(0, 3));
  }

  @Test
  void testGetParamPlaceholderInvalidParamsNum() {
    assertThrows(IllegalArgumentException.class, () -> JdbcUtils.getParamPlaceholder(2, 0));
  }
}
