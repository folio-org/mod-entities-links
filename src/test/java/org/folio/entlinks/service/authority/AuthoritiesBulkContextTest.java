package org.folio.entlinks.service.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class AuthoritiesBulkContextTest {

  private AuthoritiesBulkContext authoritiesBulkContext;

  @BeforeEach
  void setUp() throws IOException {
    String initialFilePath = "/path/to/initial/file";
    authoritiesBulkContext = new AuthoritiesBulkContext(initialFilePath);
  }

  @Test
  void constructor_PropertiesInitializedCorrectly() {
    // Assert
    assertEquals("path/to/initial/file", authoritiesBulkContext.getInitialFilePath());
    assertEquals("/path/to/initial", authoritiesBulkContext.getInitialFileLocation());
    assertEquals("path/to/initial/file_failedEntities", authoritiesBulkContext.getFailedEntitiesFilePath());
    assertEquals("path/to/initial/file_errors", authoritiesBulkContext.getErrorsFilePath());

    assertNotNull(authoritiesBulkContext.getLocalFailedEntitiesFile());
    assertNotNull(authoritiesBulkContext.getLocalErrorsFile());
  }

  @Test
  void getFailedEntitiesFileName_ReturnsCorrectValue() {
    // Act
    String result = authoritiesBulkContext.getFailedEntitiesFileName();

    // Assert
    assertEquals("_failedEntities", result);
  }

  @Test
  void getErrorsFileName_ReturnsCorrectValue() {
    // Act
    String result = authoritiesBulkContext.getErrorsFileName();

    // Assert
    assertEquals("_errors", result);
  }

  @Test
  void getLocalFailedEntitiesFilePath_ReturnsCorrectValue() {
    // Act
    String result = authoritiesBulkContext.getLocalFailedEntitiesFilePath();

    // Assert
    assertEquals("temp/path/to/initial/file_failedEntities", result);
  }

  @Test
  void getLocalErrorsFilePath_ReturnsCorrectValue() {
    // Act
    String result = authoritiesBulkContext.getLocalErrorsFilePath();

    // Assert
    assertEquals("temp/path/to/initial/file_errors", result);
  }

  @Test
  void deleteLocalFiles_DeletesFilesIfExists() throws IOException {
    // Arrange
    Files.createDirectories(Paths.get(authoritiesBulkContext.getLocalFailedEntitiesFilePath()));
    Files.createDirectories(Paths.get(authoritiesBulkContext.getLocalErrorsFilePath()));

    // Act
    authoritiesBulkContext.deleteLocalFiles();

    // Assert
    assertFalse(Files.exists(Paths.get(authoritiesBulkContext.getLocalFailedEntitiesFilePath())));
    assertFalse(Files.exists(Paths.get(authoritiesBulkContext.getLocalErrorsFilePath())));
  }
}
