package org.folio.entlinks.service.links;

import static java.util.UUID.randomUUID;
import static org.folio.support.DatabaseHelper.AUTHORITY_TABLE;
import static org.folio.support.KafkaTestUtils.createAndStartTestConsumer;
import static org.folio.support.TestDataUtils.linksDto;
import static org.folio.support.TestDataUtils.linksDtoCollection;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.authorityEndpoint;
import static org.folio.support.base.TestConstants.authorityTopic;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.controller.converter.InstanceAuthorityLinkMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.InstanceLinkDto;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.IntegrationTest;
import org.folio.support.DatabaseHelper;
import org.folio.support.TestDataUtils;
import org.folio.support.base.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

@IntegrationTest
@DatabaseCleanup(tables = {
  DatabaseHelper.INSTANCE_AUTHORITY_LINK_TABLE,
  AUTHORITY_TABLE},
  tenants = TENANT_ID)
class InstanceAuthorityLinkingServiceIT extends IntegrationTestBase {

  private KafkaMessageListenerContainer<String, AuthorityDomainEvent> container;
  private BlockingQueue<ConsumerRecord<String, AuthorityDomainEvent>> consumerRecords;
  private FolioExecutionContext context;
  @Autowired
  private InstanceAuthorityLinkingService linkingService;
  @Autowired
  private InstanceAuthorityLinkMapper mapper;

  @BeforeAll
  static void prepare() {
    setUpTenant();
  }

  @BeforeEach
  void setup(@Autowired KafkaProperties kafkaProperties) {
    consumerRecords = new LinkedBlockingQueue<>();
    container = createAndStartTestConsumer(
        authorityTopic(), consumerRecords, kafkaProperties, AuthorityDomainEvent.class);
    context = getFolioExecutionContext();
  }

  @AfterEach
  void tearDown() {
    consumerRecords.clear();
    container.stop();
  }

  @Test
  @SneakyThrows
  void shouldNotChangeAuthorityVersionAfterInstanceLinksUpdateTwice() {
    var instanceId = randomUUID();
    var existedLinks = createLinkDtoCollection(instanceId).getLinks();

    createAuthority(existedLinks.getFirst());
    awaitUntilAsserted(() -> assertEquals(1, databaseHelper.countRows(AUTHORITY_TABLE, TENANT_ID)));

    var incomingLinks = mapper.convertDto(existedLinks);

    context.execute(() -> {
      try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
        var future1 = executorService.submit(() -> linkingService.updateLinks(instanceId, incomingLinks));
        var future2 = executorService.submit(() -> linkingService.updateLinks(instanceId, incomingLinks));
        future1.get();
        future2.get();
      } catch (ExecutionException e) {
        assertEquals(0, databaseHelper.queryAuthorityVersion(TENANT_ID, existedLinks.getFirst().getAuthorityId()));
      }
      return null;
    });

    awaitUntilAsserted(() ->
        assertEquals(0, databaseHelper.queryAuthorityVersion(TENANT_ID, existedLinks.getFirst().getAuthorityId())));
  }

  private FolioExecutionContext getFolioExecutionContext() {
    return new DefaultFolioExecutionContext(new FolioModuleMetadata() {
      @Override
      public String getModuleName() {
        return "mod-entities-links";
      }

      @Override
      public String getDBSchemaName(String tenantId) {
        return String.format("%s_mod_entities_links", tenantId);
      }
    }, okapiHeaders());
  }

  private InstanceLinkDtoCollection createLinkDtoCollection(UUID instanceId) {
    var links = IntStream.of(1)
        .mapToObj(i -> TestDataUtils.Link.of(i, i, TestDataUtils.NATURAL_IDS[i]))
        .toArray(TestDataUtils.Link[]::new);
    return linksDtoCollection(linksDto(instanceId, links));
  }

  private void createAuthority(InstanceLinkDto link) {
    var dto = new AuthorityDto()
        .id(link.getAuthorityId())
        .version(0)
        .source("MARC")
        .naturalId(link.getAuthorityNaturalId())
        .personalName("Nikola Tesla1");
    doPost(authorityEndpoint(), dto, tenantHeaders(TENANT_ID));
  }
}
