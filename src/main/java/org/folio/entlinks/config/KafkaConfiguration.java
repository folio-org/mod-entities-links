package org.folio.entlinks.config;

import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.folio.entlinks.domain.dto.LinkUpdateReport;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.folio.entlinks.integration.dto.event.DomainEvent;
import org.folio.entlinks.integration.kafka.AuthorityChangeFilterStrategy;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.rspec.domain.dto.SpecificationUpdatedEvent;
import org.folio.rspec.domain.dto.UpdateRequestEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Responsible for Kafka configuration.
 */
@Configuration
public class KafkaConfiguration {

  private final ObjectMapper objectMapper;

  public KafkaConfiguration(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Creates and configures {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} as
   * Spring bean for consuming authority events from Apache Kafka.
   *
   * @return {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} object as Spring bean.
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, AuthorityDomainEvent> authorityListenerFactory(
    ConsumerFactory<String, AuthorityDomainEvent> consumerFactory) {
    var factory = listenerFactory(consumerFactory, true);
    factory.setRecordFilterStrategy(new AuthorityChangeFilterStrategy());
    return factory;
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.ConsumerFactory} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link AuthorityDomainEvent}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.ConsumerFactory} object as Spring bean.
   */
  @Bean
  public ConsumerFactory<String, AuthorityDomainEvent> authorityConsumerFactory(KafkaProperties kafkaProperties) {
    return consumerFactoryForEvent(kafkaProperties, AuthorityDomainEvent.class);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.ConsumerFactory} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link SpecificationUpdatedEvent}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.ConsumerFactory} object as Spring bean.
   */
  @Bean
  public ConsumerFactory<String, SpecificationUpdatedEvent> specificationConsumerFactory(
    KafkaProperties kafkaProperties,
    @Value("#{folioKafkaProperties.listener['specification-storage'].autoOffsetReset}")
    OffsetResetStrategy autoOffsetReset) {
    var overrideProperties = Map.<String, Object>of(AUTO_OFFSET_RESET_CONFIG, autoOffsetReset.toString());
    return consumerFactoryForEvent(kafkaProperties, SpecificationUpdatedEvent.class, overrideProperties);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} as
   * Spring bean for consuming link update report events from Apache Kafka.
   *
   * @return {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} object as Spring bean.
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, SpecificationUpdatedEvent> specificationListenerFactory(
    ConsumerFactory<String, SpecificationUpdatedEvent> consumerFactory) {
    return listenerFactory(consumerFactory, false);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} as
   * Spring bean for consuming link update report events from Apache Kafka.
   *
   * @return {@link org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory} object as Spring bean.
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, LinkUpdateReport> statsListenerFactory(
    ConsumerFactory<String, LinkUpdateReport> consumerFactory) {
    return listenerFactory(consumerFactory, true);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.ConsumerFactory} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link LinkUpdateReport}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.ConsumerFactory} object as Spring bean.
   */
  @Bean
  public ConsumerFactory<String, LinkUpdateReport> linkUpdateReportConsumerFactory(KafkaProperties kafkaProperties) {
    return consumerFactoryForEvent(kafkaProperties, LinkUpdateReport.class);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.ProducerFactory} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link LinksChangeEvent}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.ProducerFactory} object as Spring bean.
   */
  @Bean
  public ProducerFactory<String, LinksChangeEvent> producerFactory(KafkaProperties kafkaProperties) {
    return getProducerConfigProps(kafkaProperties);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.ProducerFactory} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link LinkUpdateReport}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.ProducerFactory} object as Spring bean.
   */
  @Bean
  public ProducerFactory<String, LinkUpdateReport> linkUpdateProducerFactory(KafkaProperties kafkaProperties) {
    return getProducerConfigProps(kafkaProperties);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.KafkaTemplate} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link LinksChangeEvent}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.KafkaTemplate} object as Spring bean.
   */
  @Bean
  public KafkaTemplate<String, LinksChangeEvent> linksChangeKafkaTemplate(
    ProducerFactory<String, LinksChangeEvent> factory) {
    return new KafkaTemplate<>(factory);
  }

  /**
   * Creates and configures {@link org.springframework.kafka.core.KafkaTemplate} as Spring bean.
   *
   * <p>Key type - {@link String}, value - {@link LinkUpdateReport}.</p>
   *
   * @return typed {@link org.springframework.kafka.core.KafkaTemplate} object as Spring bean.
   */
  @Bean
  public KafkaTemplate<String, LinkUpdateReport> linksUpdateKafkaTemplate(
    ProducerFactory<String, LinkUpdateReport> linkUpdateProducerFactory) {
    return new KafkaTemplate<>(linkUpdateProducerFactory);
  }

  @Bean
  public EventProducer<LinksChangeEvent> linksChangeEventMessageProducerService(
    KafkaTemplate<String, LinksChangeEvent> template) {
    return new EventProducer<>(template, "links.instance-authority");
  }

  @Bean
  public EventProducer<LinkUpdateReport> linkUpdateReportMessageProducerService(
    KafkaTemplate<String, LinkUpdateReport> template) {
    return new EventProducer<>(template, "links.instance-authority-stats");
  }

  @Bean
  public <T> ProducerFactory<String, T> domainProducerFactory(KafkaProperties kafkaProperties) {
    return getProducerConfigProps(kafkaProperties);
  }

  @Bean
  public <T> KafkaTemplate<String, DomainEvent<T>> domainKafkaTemplate(
    ProducerFactory<String, DomainEvent<T>> domainProducerFactory) {
    return new KafkaTemplate<>(domainProducerFactory);
  }

  @Bean
  public KafkaTemplate<String, UpdateRequestEvent> specificationRequestKafkaTemplate(
    ProducerFactory<String, UpdateRequestEvent> domainProducerFactory) {
    return new KafkaTemplate<>(domainProducerFactory);
  }

  @Bean("authorityDomainMessageProducer")
  public <T> EventProducer<DomainEvent<T>> authorityDomainMessageProducerService(
    KafkaTemplate<String, DomainEvent<T>> template) {
    return new EventProducer<>(template, "authorities.authority");
  }

  @Bean("authoritySourceFileDomainMessageProducer")
  public <T> EventProducer<DomainEvent<T>> authoritySourceFileDomainMessageProducerService(
    KafkaTemplate<String, DomainEvent<T>> template) {
    return new EventProducer<>(template, "authority.authority-source-file");
  }

  @Bean("subfieldUpdateRequestEventMessageProducer")
  public EventProducer<UpdateRequestEvent> specificationRequestEventMessageProducerService(
    KafkaTemplate<String, UpdateRequestEvent> template) {
    return new EventProducer<>(template, "specification-storage.specification.update");
  }

  private <T> ConcurrentKafkaListenerContainerFactory<String, T> listenerFactory(
    ConsumerFactory<String, T> consumerFactory, boolean isBatch) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, T>();
    factory.setBatchListener(isBatch);
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(new CommonLoggingErrorHandler());
    return factory;
  }

  private <T> ConsumerFactory<String, T> consumerFactoryForEvent(KafkaProperties kafkaProperties, Class<T> eventClass) {
    return consumerFactoryForEvent(kafkaProperties, eventClass, Collections.emptyMap());
  }

  private <T> ConsumerFactory<String, T> consumerFactoryForEvent(KafkaProperties kafkaProperties, Class<T> eventClass,
                                                                 Map<String, Object> overrideProps) {
    var deserializer = new JsonDeserializer<>(eventClass, objectMapper, false);
    Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
    config.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
    config.putAll(overrideProps);
    return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
  }

  private <T> ProducerFactory<String, T> getProducerConfigProps(KafkaProperties kafkaProperties) {
    return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(null),
      new StringSerializer(), new JsonSerializer<>(objectMapper));
  }
}
