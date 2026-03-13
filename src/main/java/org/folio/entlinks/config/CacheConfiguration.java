package org.folio.entlinks.config;

import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_EXTENDED_MAPPING_CACHE;
import static org.folio.entlinks.config.constants.CacheNames.DATA_IMPORT_CANCELED_JOB_CACHE;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import org.folio.entlinks.config.properties.CustomCacheProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
@EnableConfigurationProperties({CacheProperties.class, CustomCacheProperties.class})
public class CacheConfiguration {

  @Bean
  public CacheManager cacheManager(CacheProperties cacheProperties, CustomCacheProperties customCacheProperties) {
    Collection<Cache> caches = new ArrayList<>();
    for (String cacheName : cacheProperties.getCacheNames()) {
      var customCacheSpec = customCacheProperties.getSpec().get(cacheName);
      if (customCacheSpec == null) {
        caches.add(buildDefaultCache(cacheName, cacheProperties.getCaffeine().getSpec()));
      } else {
        caches.add(buildCustomCache(cacheName, customCacheSpec.ttl(), customCacheSpec.maximumSize()));
      }
    }
    var manager = new SimpleCacheManager();
    manager.setCaches(caches);
    return manager;
  }

  @Bean
  public Cache authorityExtendedMappingEnabledCache(CacheManager cacheManager) {
    return getCache(cacheManager, AUTHORITY_EXTENDED_MAPPING_CACHE);
  }

  @Bean
  public Cache dataImportCanceledJobCache(CacheManager cacheManager) {
    return getCache(cacheManager, DATA_IMPORT_CANCELED_JOB_CACHE);
  }

  private @NonNull Cache getCache(CacheManager cacheManager, String cacheName) {
    var cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      throw new IllegalStateException(
        "Required cache '" + cacheName + "' is not configured. "
        + "Ensure '" + cacheName + "' is listed under spring.cache.cache-names.");
    }
    return cache;
  }

  private CaffeineCache buildDefaultCache(String name, String spec) {
    if (spec == null || spec.isBlank()) {
      throw new IllegalStateException(
        "No Caffeine spec configured for cache '" + name + "'. "
        + "Set spring.cache.caffeine.spec or add a custom spec under folio.cache.spec.");
    }
    return new CaffeineCache(name, Caffeine.from(spec).build());
  }

  private CaffeineCache buildCustomCache(String name, Duration durationToExpire, long maximumSize) {
    return new CaffeineCache(name, Caffeine.newBuilder()
      .expireAfterWrite(durationToExpire)
      .maximumSize(maximumSize)
      .build());
  }
}
