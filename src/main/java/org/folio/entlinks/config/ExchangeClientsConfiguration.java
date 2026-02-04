package org.folio.entlinks.config;

import org.folio.entlinks.client.ConsortiumTenantsClient;
import org.folio.entlinks.client.InstanceStorageClient;
import org.folio.entlinks.client.MappingRulesClient;
import org.folio.entlinks.client.SettingsClient;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.client.UserTenantsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ExchangeClientsConfiguration {

  @Bean
  public ConsortiumTenantsClient consortiumTenantsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(ConsortiumTenantsClient.class);
  }

  @Bean
  public InstanceStorageClient instanceStorageClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InstanceStorageClient.class);
  }

  @Bean
  public MappingRulesClient mappingRulesClient(HttpServiceProxyFactory factory) {
    return factory.createClient(MappingRulesClient.class);
  }

  @Bean
  public SettingsClient settingsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SettingsClient.class);
  }

  @Bean
  public SourceStorageClient sourceStorageClient(HttpServiceProxyFactory factory) {
    return factory.createClient(SourceStorageClient.class);
  }

  @Bean
  public UserTenantsClient userTenantsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UserTenantsClient.class);
  }
}
