package org.folio.entlinks.integration.kafka.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.folio.DataImportEventPayload;
import org.folio.entlinks.integration.kafka.model.Event;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class DataImportEventDeserializer implements Deserializer<DataImportEventPayload> {

  private final ObjectMapper objectMapper;

  @Override
  public DataImportEventPayload deserialize(String topic, byte[] data) {
    try {
      var eventPayload = objectMapper.readValue(data, Event.class);
      log.info("Deserialized event: {}, tenant: {}", eventPayload.getEventType(),
        eventPayload.getEventMetadata().getTenantId());
      return objectMapper.readValue(eventPayload.getEventPayload(), DataImportEventPayload.class);
    } catch (IOException e) {
      throw new SerializationException(
        "Can't deserialize data [" + Arrays.toString(data) + "] from topic [" + topic + "]", e);
    }
  }
}
