package org.folio.entlinks.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_EXTENDED_MAPPING_CACHE;
import static org.folio.entlinks.config.constants.CacheNames.DATA_IMPORT_CANCELED_JOB_CACHE;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.folio.entlinks.config.properties.CustomCacheProperties;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CacheConfigurationTest {

  private static final String CAFFEINE_SPEC = "maximumSize=100,expireAfterWrite=3600s";
  private static final String DEFAULT_CACHE_NAME = "default-cache";
  private static final String CUSTOM_CACHE_NAME = "custom-cache";

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache mockCache;

  private final CacheConfiguration configuration = new CacheConfiguration();

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(cacheManager, mockCache);
  }

  @Test
  void cacheManager_positive_buildsDefaultCacheFromCaffeineSpec() throws Exception {
    // Arrange
    var cacheProperties = new CacheProperties();
    cacheProperties.getCacheNames().add(DEFAULT_CACHE_NAME);
    cacheProperties.getCaffeine().setSpec(CAFFEINE_SPEC);
    var customCacheProperties = new CustomCacheProperties();

    // Act
    var manager = configuration.cacheManager(cacheProperties, customCacheProperties);
    ((InitializingBean) manager).afterPropertiesSet();

    // Assert
    assertThat(manager.getCache(DEFAULT_CACHE_NAME)).isNotNull();
  }

  @Test
  void cacheManager_positive_buildsCustomCacheFromCustomSpec() throws Exception {
    // Arrange
    var cacheProperties = new CacheProperties();
    cacheProperties.getCacheNames().add(CUSTOM_CACHE_NAME);
    var customCacheProperties = new CustomCacheProperties();
    customCacheProperties.getSpec().put(CUSTOM_CACHE_NAME,
      new CustomCacheProperties.CustomCache(200L, Duration.ofHours(24)));

    // Act
    var manager = configuration.cacheManager(cacheProperties, customCacheProperties);
    ((InitializingBean) manager).afterPropertiesSet();

    // Assert
    assertThat(manager.getCache(CUSTOM_CACHE_NAME)).isNotNull();
  }

  @Test
  void cacheManager_positive_buildsMixedCachesWithDefaultAndCustomSpecs() throws Exception {
    // Arrange
    var cacheProperties = new CacheProperties();
    cacheProperties.getCacheNames().addAll(List.of(DEFAULT_CACHE_NAME, CUSTOM_CACHE_NAME));
    cacheProperties.getCaffeine().setSpec(CAFFEINE_SPEC);
    var customCacheProperties = new CustomCacheProperties();
    customCacheProperties.getSpec().put(CUSTOM_CACHE_NAME,
      new CustomCacheProperties.CustomCache(50L, Duration.ofMinutes(30)));

    // Act
    var manager = configuration.cacheManager(cacheProperties, customCacheProperties);
    ((InitializingBean) manager).afterPropertiesSet();

    // Assert
    assertThat(manager.getCache(DEFAULT_CACHE_NAME)).isNotNull();
    assertThat(manager.getCache(CUSTOM_CACHE_NAME)).isNotNull();
  }

  @Test
  void cacheManager_negative_throwsWhenCaffeineSpecIsNull() {
    // Arrange
    var cacheProperties = new CacheProperties();
    cacheProperties.getCacheNames().add(DEFAULT_CACHE_NAME);
    var customCacheProperties = new CustomCacheProperties();

    // Act & Assert
    assertThatThrownBy(() -> configuration.cacheManager(cacheProperties, customCacheProperties))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("No Caffeine spec configured for cache '" + DEFAULT_CACHE_NAME + "'");
  }

  @Test
  void cacheManager_negative_throwsWhenCaffeineSpecIsBlank() {
    // Arrange
    var cacheProperties = new CacheProperties();
    cacheProperties.getCacheNames().add(DEFAULT_CACHE_NAME);
    cacheProperties.getCaffeine().setSpec("   ");
    var customCacheProperties = new CustomCacheProperties();

    // Act & Assert
    assertThatThrownBy(() -> configuration.cacheManager(cacheProperties, customCacheProperties))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("No Caffeine spec configured for cache '" + DEFAULT_CACHE_NAME + "'");
  }

  @Test
  void authorityExtendedMappingEnabledCache_positive_returnsCacheFromManager() {
    // Arrange
    when(cacheManager.getCache(AUTHORITY_EXTENDED_MAPPING_CACHE)).thenReturn(mockCache);

    // Act
    var result = configuration.authorityExtendedMappingEnabledCache(cacheManager);

    // Assert
    assertThat(result).isEqualTo(mockCache);
  }

  @Test
  void authorityExtendedMappingEnabledCache_negative_throwsWhenCacheNotConfigured() {
    // Arrange
    when(cacheManager.getCache(AUTHORITY_EXTENDED_MAPPING_CACHE)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> configuration.authorityExtendedMappingEnabledCache(cacheManager))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Required cache '" + AUTHORITY_EXTENDED_MAPPING_CACHE + "'");
  }

  @Test
  void dataImportCanceledJobCache_positive_returnsCacheFromManager() {
    // Arrange
    when(cacheManager.getCache(DATA_IMPORT_CANCELED_JOB_CACHE)).thenReturn(mockCache);

    // Act
    var result = configuration.dataImportCanceledJobCache(cacheManager);

    // Assert
    assertThat(result).isEqualTo(mockCache);
  }

  @Test
  void dataImportCanceledJobCache_negative_throwsWhenCacheNotConfigured() {
    // Arrange
    when(cacheManager.getCache(DATA_IMPORT_CANCELED_JOB_CACHE)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> configuration.dataImportCanceledJobCache(cacheManager))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Required cache '" + DATA_IMPORT_CANCELED_JOB_CACHE + "'");
  }
}
