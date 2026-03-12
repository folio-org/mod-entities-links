package org.folio.entlinks.integration.di;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DataImportCanceledJobService {

  private final @Qualifier("dataImportCanceledJobCache") Cache cache;

  public void registerCanceledJob(@NonNull String jobId, @NonNull String tenantId) {
    log.info("Registering canceled data-import job [jobId: {}, tenantId: {}]", jobId, tenantId);
    cache.put(buildKey(jobId, tenantId), Boolean.TRUE);
  }

  public boolean isJobCanceled(@NonNull String jobId, @NonNull String tenantId) {
    return Objects.nonNull(cache.get(buildKey(jobId, tenantId)));
  }

  private String buildKey(String jobId, String tenantId) {
    return tenantId + ":" + jobId;
  }
}
