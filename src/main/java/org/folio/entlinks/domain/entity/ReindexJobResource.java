package org.folio.entlinks.domain.entity;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum ReindexJobResource {

  AUTHORITY("Authority");

  private final String authority;

  ReindexJobResource(String authority) {
    this.authority = authority;
  }

  public static ReindexJobResource fromValue(String name) {
    if (name == null) {
      return null;
    }

    return Arrays.stream(values())
        .filter(value -> name.equalsIgnoreCase(value.authority))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid Reindex Job Resource name: " + name));
  }
}
