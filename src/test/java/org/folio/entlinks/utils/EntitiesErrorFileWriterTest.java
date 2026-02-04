package org.folio.entlinks.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class EntitiesErrorFileWriterTest {

  @Mock
  private ObjectMapper objectMapper;

  private EntitiesErrorFileWriter errorFileWriter;

  private File errorEntitiesFile;
  private File errorsFile;

  @BeforeEach
  void setUp() throws IOException {
    // Create temporary files for testing
    errorEntitiesFile = File.createTempFile("errorEntities", ".txt");
    errorsFile = File.createTempFile("errors", ".txt");

    errorFileWriter = new EntitiesErrorFileWriter(errorEntitiesFile, errorsFile, objectMapper);
  }

  @AfterEach
  void tearDown() {
    // Delete temporary files
    errorEntitiesFile.delete();
    errorsFile.delete();
  }

  @Test
  void write_WritesToFile_Successfully() throws Exception {
    // Arrange
    String entityJson = "{\"id\": 1, \"name\": \"Test Entity\"}";
    String errorMessage = "Simulated error message";

    when(objectMapper.writeValueAsString(any())).thenReturn(entityJson);

    // Act
    errorFileWriter.write(new TestEntity(1, "Test Entity"), new RuntimeException(errorMessage),
      TestEntity::name);
    errorFileWriter.close();

    // Assert
    assertFileContent(errorEntitiesFile, entityJson + System.lineSeparator());
    assertFileContent(errorsFile, "Test Entity,Simulated error message" + System.lineSeparator());
  }

  private void assertFileContent(File file, String expectedContent) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      StringBuilder content = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        content.append(line).append(System.lineSeparator());
      }
      assertEquals(expectedContent, content.toString());
    }
  }

  private record TestEntity(int id, String name) { }
}
