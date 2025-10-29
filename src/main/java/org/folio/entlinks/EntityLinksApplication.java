package org.folio.entlinks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableKafka
@EnableCaching
@EnableAsync
@SpringBootApplication
public class EntityLinksApplication {

  public static void main(String[] args) {
    SpringApplication.run(EntityLinksApplication.class, args);
  }

  @Profile("dev")
  @ComponentScan("org.folio")
  @Configuration
  public class TestConfig {

  }
}
