package org.folio.entlinks.service.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.spring.testing.type.UnitTest;
import org.folio.spring.tools.kafka.FolioKafkaProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ExtendedKafkaAdminServiceTest {

  private static final String DEFAULT_NAMESPACE = "Default";
  private static final String TENANT = "test";
  private static final String TOPIC_NAME = "TOPIC";
  private static final String DI_TOPIC_NAME = "DI_TOPIC";

  @Mock
  private KafkaAdmin kafkaAdmin;
  @Mock
  private BeanFactory beanFactory;
  @Mock
  private KafkaListenerEndpointRegistry registry;
  @Mock
  private FolioKafkaProperties kafkaProperties;
  @Mock
  private FolioKafkaProperties.KafkaTopic topic;
  @InjectMocks
  private ExtendedKafkaAdminService service;

  @Test
  void testToTenantSpecificTopic_withDiPrefix() {
    when(topic.getName()).thenReturn(DI_TOPIC_NAME);
    when(topic.getNumPartitions()).thenReturn(1);
    when(topic.getReplicationFactor()).thenReturn((short) 1);

    var result = service.toTenantSpecificTopic(List.of(topic), TENANT);
    assertEquals(1, result.size());
    assertTrue(result.getFirst().name().contains(DEFAULT_NAMESPACE));
  }

  @Test
  void testToTenantSpecificTopic_withoutDiPrefix() {
    when(topic.getName()).thenReturn(TOPIC_NAME);
    when(topic.getNumPartitions()).thenReturn(2);
    when(topic.getReplicationFactor()).thenReturn((short) 2);

    var result = service.toTenantSpecificTopic(List.of(topic), TENANT);
    assertEquals(1, result.size());
    assertFalse(result.getFirst().name().contains(DEFAULT_NAMESPACE));
  }
}
