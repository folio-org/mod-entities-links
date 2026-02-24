package org.folio.entlinks.config;

import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_EXTENDED_MAPPING_CACHE;

import java.util.Optional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguration {

  @Bean
  public Cache authorityExtendedMappingEnabledCache(CacheManager cacheManager) {
    return Optional.ofNullable(cacheManager.getCache(AUTHORITY_EXTENDED_MAPPING_CACHE))
      .orElse(new NoOpCache(AUTHORITY_EXTENDED_MAPPING_CACHE));
  }
}
