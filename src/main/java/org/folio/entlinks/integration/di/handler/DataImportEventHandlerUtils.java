package org.folio.entlinks.integration.di.handler;

import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.folio.DataImportEventPayload;

@UtilityClass
public class DataImportEventHandlerUtils {

  public static final String RECORD_ID_HEADER = "recordId";
  public static final String CHUNK_ID_HEADER = "chunkId";
  public static final String JOB_EXECUTION_ID_HEADER = "jobExecutionId";
  public static final String MARC_SOURCE = "MARC";

  private static final String DI_LOG_TEMPLATE = "{} [jobExecutionId: {}, chunkId: {}, recordId: {}]";

  public static void logDataImport(Logger logger, Level level, String message, DataImportEventPayload payload) {
    var jobExecutionId = getJobExecutionId(payload);
    var chunkId = getChunkId(payload);
    var recordId = getRecordId(payload);
    logger.log(level, DI_LOG_TEMPLATE, message, jobExecutionId, chunkId, recordId);
  }

  public static void logDataImportError(Logger logger, String message, DataImportEventPayload payload, Throwable t) {
    var jobExecutionId = getJobExecutionId(payload);
    var chunkId = getChunkId(payload);
    var recordId = getRecordId(payload);
    logger.error(() ->
      logger.getMessageFactory().newMessage(DI_LOG_TEMPLATE, message, jobExecutionId, chunkId, recordId), t);
  }

  public static String getRecordId(DataImportEventPayload payload) {
    return payload.getContext().get(RECORD_ID_HEADER);
  }

  public static String getChunkId(DataImportEventPayload payload) {
    return payload.getContext().get(CHUNK_ID_HEADER);
  }

  public static String getJobExecutionId(DataImportEventPayload payload) {
    return payload.getContext().get(JOB_EXECUTION_ID_HEADER);
  }
}
