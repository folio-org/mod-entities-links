package org.folio.entlinks.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("instance-storage")
public interface InstanceStorageClient {

  @GetExchange(value = "/instances", accept = APPLICATION_JSON_VALUE)
  InventoryInstanceDtoCollection getInstanceStorageInstances(@RequestParam String query, @RequestParam int limit);

  record InventoryInstanceDto(String id, String title, String source) { }

  record InventoryInstanceDtoCollection(List<InventoryInstanceDto> instances) { }
}
