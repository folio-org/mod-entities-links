package org.folio.entlinks.integration.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DataImportCanceledJobServiceTest {

  private static final String JOB_ID = "test-job-id";
  private static final String TENANT_ID = "test-tenant";
  private static final String CACHE_KEY = TENANT_ID + ":" + JOB_ID;

  @Mock
  private Cache cache;

  @InjectMocks
  private DataImportCanceledJobService service;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(cache);
  }

  @Test
  void registerCanceledJob_positive_jobIsStoredInCache() {
    service.registerCanceledJob(JOB_ID, TENANT_ID);

    verify(cache).put(CACHE_KEY, Boolean.TRUE);
  }

  @Test
  void isJobCanceled_positive_jobExistsInCache() {
    var valueWrapper = mock(Cache.ValueWrapper.class);
    when(cache.get(CACHE_KEY)).thenReturn(valueWrapper);

    var result = service.isJobCanceled(JOB_ID, TENANT_ID);

    assertThat(result).isTrue();
  }

  @Test
  void isJobCanceled_negative_jobNotInCache() {
    var result = service.isJobCanceled(JOB_ID, TENANT_ID);

    assertThat(result).isFalse();
    verify(cache).get(CACHE_KEY);
  }
}
