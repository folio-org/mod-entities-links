package org.folio.entlinks.service.authority;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.s3.client.FolioS3Client;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Code partially generated using GitHub Copilot.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class BulkAuthorityS3Client {

  private final FolioS3Client s3Client;

  @Retryable(
    maxRetriesString = "${folio.remote-storage.retryCount}",
    delayString = "${folio.remote-storage.retryDelayMs}")
  public List<String> readFile(String remoteFileName) {
    log.info("readFile::Reading lines from the file [filename: {}]", remoteFileName);
    try (var inputStream = s3Client.read(remoteFileName);
         var reader = new BufferedReader(new InputStreamReader(inputStream))) {
      return reader.lines().toList();
    } catch (IOException e) {
      log.error("readFile::Error reading file [filename: {}]", remoteFileName, e);
      throw new IllegalStateException("Error reading file: " + remoteFileName, e);
    }
  }

  @Retryable(
    maxRetriesString = "${folio.remote-storage.retryCount}",
    delayString = "${folio.remote-storage.retryDelayMs}")
  public void uploadErrorFiles(AuthoritiesBulkContext bulkContext) {
    log.info("uploadErrorFiles::Uploading [failedEntities: {}, errors: {}]",
      bulkContext.getFailedEntitiesFilePath(), bulkContext.getErrorsFilePath());
    s3Client.upload(bulkContext.getLocalFailedEntitiesFilePath(), bulkContext.getFailedEntitiesFilePath());
    s3Client.upload(bulkContext.getLocalErrorsFilePath(), bulkContext.getErrorsFilePath());
  }
}
