package org.folio.entlinks.service.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.folio.entlinks.controller.converter.AuthorityMapperImpl;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityS3ServiceTest {

  private static final String AUTHORITY_UUID = "58949d4b-2da2-43ce-b12b-319dd22f5990";
  @Mock
  private BulkAuthorityS3Client s3Client;
  @Mock
  private AuthorityMapperImpl mapper;
  @Mock
  private Consumer<List<Authority>> bulkConsumer;
  private AuthorityS3Service authorityS3Service;

  @BeforeEach
  void setUp() {
    authorityS3Service = new AuthorityS3Service(s3Client, new ObjectMapper(), mapper);
  }

  @Test
  void processAuthorities_SuccessfulProcessing_NoErrorReturned() throws IOException {
    // Arrange
    AuthoritiesBulkContext bulkContext = mock(AuthoritiesBulkContext.class);
    var authorityJson = "{\"id\": \"" + AUTHORITY_UUID + "\", \"personalName\": \"Test Authority\"}";
    when(s3Client.readFile(any())).thenReturn(List.of(authorityJson));
    when(mapper.toEntity(any())).thenCallRealMethod();

    // Act
    int errorCount = authorityS3Service.processAuthorities(bulkContext, bulkConsumer);

    // Assert
    assertEquals(0, errorCount);
    var testAuthority = new Authority();
    testAuthority.setId(UUID.fromString(AUTHORITY_UUID));
    testAuthority.setHeading("Test Authority");
    testAuthority.setHeadingType("personalName");
    verify(bulkConsumer).accept(List.of(testAuthority));
    verify(bulkContext).deleteLocalFiles();
    verify(s3Client, never()).uploadErrorFiles(any());
  }
}
