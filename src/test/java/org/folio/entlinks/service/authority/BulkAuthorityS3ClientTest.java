package org.folio.entlinks.service.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Code partially generated using GitHub Copilot.
 * */
@UnitTest
@ExtendWith(MockitoExtension.class)
class BulkAuthorityS3ClientTest {

  private static final String AUTHORITY_UUID = "58949d4b-2da2-43ce-b12b-319dd22f5990";
  @Mock
  private FolioS3Client s3Client;
  @InjectMocks
  private BulkAuthorityS3Client client;

  @Test
  void readFile_ReturnsListOfStringAuthority() {
    // Arrange
    var remoteFileName = "test-file";
    var authorityJson = "{\"id\": \"" + AUTHORITY_UUID + "\", \"personalName\": \"Test Authority\"}";
    var inputStream = new ByteArrayInputStream(authorityJson.getBytes());
    when(s3Client.read(remoteFileName)).thenReturn(inputStream);

    // Act
    var resultList = client.readFile(remoteFileName);

    // Assert
    assertEquals(1, resultList.size());
    var stringAuthority = resultList.getFirst();
    assertThat(stringAuthority).contains(AUTHORITY_UUID, "Test Authority");
  }

  @Test
  void readFile_ReturnsEmptyListWhenFileIsEmpty() {
    // Arrange
    var remoteFileName = "empty-file";
    var inputStream = new ByteArrayInputStream(new byte[0]);
    when(s3Client.read(remoteFileName)).thenReturn(inputStream);

    // Act
    var resultList = client.readFile(remoteFileName);

    // Assert
    assertThat(resultList).isEmpty();
  }

  @Test
  void readFile_ThrowsIllegalStateExceptionWhenIoExceptionOccurs() {
    // Arrange
    var remoteFileName = "error-file";
    when(s3Client.read(remoteFileName)).thenAnswer(invocation -> {
      throw new IOException("Test IOException");
    });

    // Act & Assert
    var exception = assertThrows(IllegalStateException.class, () -> client.readFile(remoteFileName));
    assertThat(exception).hasMessageContaining("Error reading file: " + remoteFileName);
  }

  @Test
  void uploadErrorFiles_uploads() throws IOException {
    // Arrange
    var remoteFileName = "test-file";
    var bulkContext = new AuthoritiesBulkContext(remoteFileName);

    // Act
    client.uploadErrorFiles(bulkContext);

    // Assert
    verify(s3Client).upload(bulkContext.getLocalFailedEntitiesFilePath(), bulkContext.getFailedEntitiesFilePath());
    verify(s3Client).upload(bulkContext.getLocalErrorsFilePath(), bulkContext.getErrorsFilePath());
  }

}
