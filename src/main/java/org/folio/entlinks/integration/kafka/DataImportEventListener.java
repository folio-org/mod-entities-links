package org.folio.entlinks.integration.kafka;

import static org.folio.rest.util.OkapiConnectionParams.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.PERMISSIONS;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.ActionProfile;
import org.folio.AuthoritySourceFile;
import org.folio.DataImportEventPayload;
import org.folio.Record;
import org.folio.entlinks.client.MappingMetadataClient;
import org.folio.entlinks.controller.delegate.AuthorityServiceDelegate;
import org.folio.entlinks.controller.delegate.AuthoritySourceFileServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.kafka.model.DataImportEventWrapper;
import org.folio.processing.events.EventManager;
import org.folio.processing.events.services.handler.EventHandler;
import org.folio.processing.events.services.publisher.EventPublisher;
import org.folio.processing.mapping.defaultmapper.RecordMapperBuilder;
import org.folio.processing.mapping.defaultmapper.processor.parameters.MappingParameters;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.jaxrs.model.ProfileSnapshotWrapper;
import org.folio.rest.jaxrs.model.ProfileType;
import org.folio.spring.scope.FolioExecutionContextService;
import org.folio.spring.tools.batch.MessageBatchProcessor;
import org.folio.spring.tools.config.properties.FolioEnvironment;
import org.folio.spring.tools.kafka.KafkaUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
@Profile("dev")
public class DataImportEventListener {

  private static final String AUTHORITY_EXTENDED = "AUTHORITY_EXTENDED";
  private static final boolean IS_AUTHORITY_EXTENDED = isAuthorityExtendedMode();

  private static final String RECORD_ID_HEADER = "recordId";
  private static final String CHUNK_ID_HEADER = "chunkId";
  private static final String JOB_EXECUTION_ID_HEADER = "jobExecutionId";

  private final FolioExecutionContextService executionService;
  private final MessageBatchProcessor messageBatchProcessor;
  private final ObjectMapper objectMapper;
  private final AuthorityServiceDelegate delegate;
  private final KafkaTemplate<String, Event> kafkaTemplate;
  private final AuthoritySourceFileServiceDelegate sourceFileServiceDelegate;
  private final MappingMetadataClient mappingMetadataClient;

  @KafkaListener(id = "mod-entities-links-data-import-listener",
                 containerFactory = "diListenerFactory",
                 topicPattern = "#{folioKafkaProperties.listener['data-import'].topicPattern}",
                 groupId = "#{folioKafkaProperties.listener['data-import'].groupId}",
                 concurrency = "#{folioKafkaProperties.listener['data-import'].concurrency}")
  public void handleEvents(List<DataImportEventWrapper> consumerRecords) {
    log.info("Processing data-import event [number of records: {}]", consumerRecords.size());
    EventManager.registerEventHandler(new AuthorityCreateEventHandler());
    EventManager.registerCustomKafkaEventPublisher(new SpringKafkaEventPublisher());
    var enventByTenant = consumerRecords.stream()
      .collect(Collectors.groupingBy(DataImportEventWrapper::tenant));
    List<CompletableFuture<Void>> allFutures = new ArrayList<>();
    for (var entry : enventByTenant.entrySet()) {
      var tenant = entry.getKey();
      var records = entry.getValue();
      var futures = executionService.execute(tenant, entry.getValue().getFirst().getHeadersMap(),
        () -> records.stream().map(this::processEvent).toList());
      allFutures.addAll(futures);
    }

    CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
  }

  private CompletableFuture<Void> processEvent(DataImportEventWrapper diEvent) {
    return EventManager.handleEvent(diEvent.payload(), new ProfileSnapshotWrapper()).thenApply(e -> null);
  }

  private static boolean isAuthorityExtendedMode() {
    return Boolean.parseBoolean(
      System.getenv().getOrDefault(AUTHORITY_EXTENDED, "false"));
  }

  private List<RecordHeader> getHeaders(DataImportEventPayload eventPayload, String recordId, String chunkId,
                                        String jobExecutionId) {
    List<RecordHeader> headers = new ArrayList<>();
    Optional.ofNullable(eventPayload.getToken())
      .ifPresent(token -> headers.add(getRecordHeader(TOKEN, token)));
    Optional.ofNullable(eventPayload.getContext().get(PERMISSIONS))
      .ifPresent(permissions -> headers.add(getRecordHeader(PERMISSIONS, permissions)));
    Optional.ofNullable(eventPayload.getContext())
      .map(it -> it.get(USER_ID_HEADER))
      .ifPresent(userId -> headers.add(getRecordHeader(USER_ID_HEADER, userId)));

    headers.add(getRecordHeader(URL, eventPayload.getOkapiUrl()));
    headers.add(getRecordHeader(TENANT, eventPayload.getTenant()));
    if (StringUtils.isBlank(recordId)) {
      log.warn("checkAndAddHeaders:: RecordId is empty for jobExecutionId: '{}' ", jobExecutionId);
    } else {
      headers.add(getRecordHeader(RECORD_ID_HEADER, recordId));
    }
    if (StringUtils.isBlank(chunkId)) {
      log.warn("checkAndAddHeaders:: ChunkId is empty for jobExecutionId: '{}' ", jobExecutionId);
    } else {
      headers.add(getRecordHeader(CHUNK_ID_HEADER, chunkId));
    }
    if (StringUtils.isBlank(jobExecutionId)) {
      log.warn("checkAndAddHeaders:: jobExecutionId is empty recordId: {} chunkId: '{}'", recordId, chunkId);
    } else {
      headers.add(getRecordHeader(JOB_EXECUTION_ID_HEADER, jobExecutionId));
    }
    return headers;
  }

  private RecordHeader getRecordHeader(String headerName, String headerValue) {
    return new RecordHeader(headerName, headerValue.getBytes(StandardCharsets.UTF_8));
  }

  public class AuthorityCreateEventHandler implements EventHandler {

    @Override
    public CompletableFuture<DataImportEventPayload> handle(DataImportEventPayload payload) {
      var authorityJson = payload.getContext().get("MARC_AUTHORITY");
      try {
        var srsRecord =
          new JsonObject((String) objectMapper.readValue(authorityJson, Record.class).getParsedRecord().getContent());
        var recordMapper = IS_AUTHORITY_EXTENDED
                           ? RecordMapperBuilder.buildMapper(
          ActionProfile.FolioRecord.MARC_AUTHORITY_EXTENDED.value())
                           : RecordMapperBuilder.buildMapper("MARC_AUTHORITY");

        var authoritySourceFiles = sourceFileServiceDelegate.getAuthoritySourceFiles(0, 1000, null)
          .getAuthoritySourceFiles()
          .stream()
          .map(authoritySourceFileDto -> {
            try {
              return objectMapper.readValue(objectMapper.writeValueAsString(authoritySourceFileDto),
                AuthoritySourceFile.class);
            } catch (JsonProcessingException e) {
              throw new RuntimeException(e);
            }
          })
          .toList();
        var mappingParameters = new MappingParameters();
        mappingParameters.setAuthoritySourceFiles(authoritySourceFiles);
        var mappingMetadata = mappingMetadataClient.getMappingMetadata(payload.getJobExecutionId());
        var rawAuthority =
          recordMapper.mapRecord(srsRecord, mappingParameters, new JsonObject(mappingMetadata.mappingRules()));
        var authority = objectMapper.readValue(objectMapper.writeValueAsString(rawAuthority), AuthorityDto.class);
        authority.setSource("MARC");
        var createdAuthority = delegate.createAuthority(authority);
        payload.getContext().put("AUTHORITY", objectMapper.writeValueAsString(createdAuthority));
        payload.setEventType("DI_INVENTORY_AUTHORITY_CREATED");
        payload.getEventsChain().add(payload.getEventType());
        payload.setCurrentNode(payload.getCurrentNode().getChildSnapshotWrappers().get(0));
        return CompletableFuture.completedFuture(payload);
      } catch (Exception e) {
        log.error("Error while processing event", e);
        return CompletableFuture.failedFuture(e);
      }
    }

    @Override
    public boolean isEligible(DataImportEventPayload payload) {
      var currentNode = payload.getCurrentNode();
      return ProfileType.ACTION_PROFILE == currentNode.getContentType()
             && isEligibleActionProfile(currentNode);
    }

    private boolean isEligibleActionProfile(ProfileSnapshotWrapper currentNode) {
      var actionProfile = JsonObject.mapFrom(currentNode.getContent()).mapTo(ActionProfile.class);
      return ActionProfile.Action.CREATE == actionProfile.getAction()
             && ActionProfile.FolioRecord.AUTHORITY == actionProfile.getFolioRecord();
    }

    @Override
    public boolean isPostProcessingNeeded() {
      return true;
    }

    @Override
    public String getPostProcessingInitializationEventType() {
      return "DI_INVENTORY_AUTHORITY_CREATED_READY_FOR_POST_PROCESSING";
    }
  }

  public class SpringKafkaEventPublisher implements EventPublisher {

    @Override
    public CompletableFuture<Event> publish(DataImportEventPayload payload) {
      var event = new Event();
      try {
        event.setEventPayload(objectMapper.writeValueAsString(payload));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      event.setId(UUID.randomUUID().toString());
      var eventMetadata = new EventMetadata();
      eventMetadata.setTenantId(payload.getTenant());
      eventMetadata.setPublishedBy("mod-entities-links");
      event.setEventMetadata(eventMetadata);

      var eventName = payload.getEventType();
      var recordId = payload.getContext().get(RECORD_ID_HEADER);
      var chunkId = payload.getContext().get(CHUNK_ID_HEADER);
      var jobExecutionId = payload.getJobExecutionId();
      var authorityCreated =
        new ProducerRecord<String, Event>(KafkaUtils
          .getTenantTopicNameWithNamespace(eventName, FolioEnvironment.getFolioEnvName(), payload.getTenant(),
            "Default"),
          event);
      var messageHeaders = authorityCreated.headers();
      DataImportEventListener.this.getHeaders(payload, recordId, chunkId, jobExecutionId)
        .forEach(messageHeaders::add);
      return kafkaTemplate.send(authorityCreated).thenApply(stringEventSendResult -> event);
    }
  }
}
