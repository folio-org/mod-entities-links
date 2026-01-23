package org.folio.entlinks.service.settings;

import lombok.Getter;

@Getter
public enum TenantSetting {

  MAPPING_EXTENDED("mapping.extended", "authorities"),
  ARCHIVES_EXPIRATION_ENABLED("archives.expiration.enabled", "authorities"),
  ARCHIVES_EXPIRATION_PERIOD("archives.expiration.period", "authorities");

  private final String key;
  private final String group;

  TenantSetting(String key, String group) {
    this.key = key;
    this.group = group;
  }
}
