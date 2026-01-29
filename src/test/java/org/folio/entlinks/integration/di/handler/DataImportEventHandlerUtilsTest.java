package org.folio.entlinks.integration.di.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.CHUNK_ID_HEADER;
import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.JOB_EXECUTION_ID_HEADER;
import static org.folio.entlinks.integration.di.handler.DataImportEventHandlerUtils.RECORD_ID_HEADER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.util.MessageSupplier;
import org.folio.DataImportEventPayload;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DataImportEventHandlerUtilsTest {

  private static final String JOB_EXECUTION_ID = "jobId";
  private static final String CHUNK_ID = "chunkId";
  private static final String RECORD_ID = "recordId";

  @Mock
  private Logger logger;
  @Mock
  private MessageFactory messageFactory;
  @Mock
  private Message message;

  @Test
  void logDataImport_withLevel() {
    var payload = getPayloadWithContext();
    // Act
    DataImportEventHandlerUtils.logDataImport(logger, Level.INFO, "Test message", payload);
    // Assert
    verify(logger).log(Level.INFO, "{} [jobExecutionId: {}, chunkId: {}, recordId: {}]",
        "Test message", JOB_EXECUTION_ID, CHUNK_ID, RECORD_ID);
  }

  @Test
  void logDataImport_withThrowable() {
    var payload = getPayloadWithContext();
    var exception = new RuntimeException("test error");
    // Act
    DataImportEventHandlerUtils.logDataImport(logger, "Test error message", payload, exception);
    // Assert
    verify(logger).error(any(MessageSupplier.class), eq(exception));
  }

  @Test
  void getRecordIdSuccessfully() {
    HashMap<String, String> context = new HashMap<>();
    context.put(RECORD_ID_HEADER, RECORD_ID);
    var payload = new DataImportEventPayload().withContext(context);
    // Act
    var result = DataImportEventHandlerUtils.getRecordId(payload);
    // Assert
    assertThat(result).isEqualTo(RECORD_ID);
  }

  @Test
  void getChunkIdSuccessfully() {
    HashMap<String, String> context = new HashMap<>();
    context.put(CHUNK_ID_HEADER, CHUNK_ID);
    var payload = new DataImportEventPayload().withContext(context);
    // Act
    var result = DataImportEventHandlerUtils.getChunkId(payload);
    // Assert
    assertThat(result).isEqualTo(CHUNK_ID);
  }

  @Test
  void getJobExecutionIdSuccessfully() {
    HashMap<String, String> context = new HashMap<>();
    context.put(JOB_EXECUTION_ID_HEADER, JOB_EXECUTION_ID);
    var payload = new DataImportEventPayload().withContext(context);
    // Act
    var result = DataImportEventHandlerUtils.getJobExecutionId(payload);
    // Assert
    assertThat(result).isEqualTo(JOB_EXECUTION_ID);
  }

  @Test
  void getRecordId_emptyResult() {
    var payload = new DataImportEventPayload().withContext(new HashMap<>());
    // Act
    var result = DataImportEventHandlerUtils.getRecordId(payload);
    // Assert
    assertThat(result).isNull();
  }

  @Test
  void getChunkId_emptyResult() {
    var payload = new DataImportEventPayload().withContext(new HashMap<>());
    // Act
    var result = DataImportEventHandlerUtils.getChunkId(payload);
    // Assert
    assertThat(result).isNull();
  }

  @Test
  void getJobExecutionId_emptyResult() {
    var payload = new DataImportEventPayload().withContext(new HashMap<>());
    // Act
    var result = DataImportEventHandlerUtils.getJobExecutionId(payload);
    // Assert
    assertThat(result).isNull();
  }

  private DataImportEventPayload getPayloadWithContext() {
    HashMap<String, String> context = new HashMap<>();
    context.put(RECORD_ID_HEADER, RECORD_ID);
    context.put(CHUNK_ID_HEADER, CHUNK_ID);
    context.put(JOB_EXECUTION_ID_HEADER, JOB_EXECUTION_ID);
    return new DataImportEventPayload().withContext(context);
  }
}
