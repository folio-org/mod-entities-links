package org.folio.entlinks.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("settings")
@Deprecated(forRemoval = true)
public interface SettingsClient {

  @GetExchange(value = "/entries", accept = APPLICATION_JSON_VALUE)
  SettingsEntries getSettingsEntries(@RequestParam("query") String query, @RequestParam("limit") int limit);

  record SettingsEntries(List<SettingEntry> items, ResultInfo resultInfo) { }

  record SettingEntry(UUID id, String scope, String key, AuthoritiesExpirationSettingValue value, UUID userId) {
    public SettingEntry(UUID id, String scope, String key, AuthoritiesExpirationSettingValue value) {
      this(id, scope, key, value, null);
    }
  }

  record AuthoritiesExpirationSettingValue(Boolean expirationEnabled, Integer retentionInDays) { }

  record ResultInfo(Integer totalRecords) { }
}
