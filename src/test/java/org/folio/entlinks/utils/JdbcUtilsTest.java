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
  private FolioModuleMetadata metadata;

  @BeforeEach
  void setUp() {
    metadata = mock(FolioModuleMetadata.class);
    context = mock(FolioExecutionContext.class);
    when(context.getFolioModuleMetadata()).thenReturn(metadata);
    when(context.getTenantId()).thenReturn("testTenant");
    when(metadata.getDBSchemaName("testTenant")).thenReturn("testSchema");
    when(metadata.getDBSchemaName("anotherTenant")).thenReturn("anotherSchema");
  }

  @Test
  void testGetSchemaName() {
    var schemaName = JdbcUtils.getSchemaName(context);
    assertEquals("testSchema", schemaName);
  }

  @Test
  void testGetFullPath() {
    var fullPath = JdbcUtils.getFullPath(context, "testTable");
    assertEquals("testSchema.testTable", fullPath);
  }

  @Test
  void testGetFullPathWithTenant() {
    var fullPath = JdbcUtils.getFullPath(metadata, "anotherTenant", "testTable");
    assertEquals("anotherSchema.testTable", fullPath);
  }

  @Test
  void testGetParamPlaceholderSingle() {
    var placeholder = JdbcUtils.getParamPlaceholder(3);
    assertEquals("?,?,?", placeholder);
  }

  @Test
  void testGetParamPlaceholderMultiple() {
    var placeholder = JdbcUtils.getParamPlaceholder(2, 3);
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
