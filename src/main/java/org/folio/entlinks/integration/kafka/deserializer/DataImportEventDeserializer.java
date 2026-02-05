package org.folio.entlinks.integration.kafka.deserializer;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.folio.DataImportEventPayload;
import org.folio.rest.jaxrs.model.Event;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Log4j2
@Component
@RequiredArgsConstructor
public class DataImportEventDeserializer implements Deserializer<DataImportEventPayload> {

  private final JsonMapper jsonMapper;

  @Override
  public DataImportEventPayload deserialize(String topic, byte[] data) {
    try {
      var eventPayload = jsonMapper.readValue(data, Event.class);
      log.info("Deserialized event: {}, tenant: {}", eventPayload.getEventType(),
        eventPayload.getEventMetadata().getTenantId());
      return jsonMapper.readValue(eventPayload.getEventPayload(), DataImportEventPayload.class);
    } catch (JacksonException e) {
      throw new SerializationException(
        "Can't deserialize data [" + Arrays.toString(data) + "] from topic [" + topic + "]", e);
    }
  }
}
