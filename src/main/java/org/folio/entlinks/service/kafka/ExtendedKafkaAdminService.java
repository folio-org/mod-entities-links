package org.folio.entlinks.service.kafka;

import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.NewTopic;
import org.folio.spring.tools.config.properties.FolioEnvironment;
import org.folio.spring.tools.kafka.FolioKafkaProperties;
import org.folio.spring.tools.kafka.KafkaAdminService;
import org.folio.spring.tools.kafka.KafkaUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class ExtendedKafkaAdminService extends KafkaAdminService {

  private static final String DEFAULT_NAMESPACE = "Default";
  private static final String DI_PREFIX = "DI_";

  public ExtendedKafkaAdminService(KafkaAdmin kafkaAdmin,
                                   BeanFactory beanFactory,
                                   KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry,
                                   FolioKafkaProperties kafkaProperties) {
    super(kafkaAdmin, beanFactory, kafkaListenerEndpointRegistry, kafkaProperties);
  }

  @Override
  protected List<NewTopic> toTenantSpecificTopic(List<FolioKafkaProperties.KafkaTopic> configTopics,
                                                 String tenantId) {
    return configTopics.stream()
        .map(topic -> {
          var topicName = topic.getName().startsWith(DI_PREFIX)
              ? getTenantTopicNameWithNamespace(tenantId, topic.getName())
              : KafkaUtils.getTenantTopicName(topic.getName(), tenantId);
          return new NewTopic(
              topicName,
              Optional.ofNullable(topic.getNumPartitions()),
              Optional.ofNullable(topic.getReplicationFactor())
          );
        })
        .toList();
  }

  private String getTenantTopicNameWithNamespace(String tenantId, String topicName) {
    return KafkaUtils.getTenantTopicNameWithNamespace(topicName, FolioEnvironment.getFolioEnvName(), tenantId,
        DEFAULT_NAMESPACE);
  }
}
