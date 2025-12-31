package org.folio.entlinks.domain.entity.projection;

import java.util.UUID;
import lombok.Data;

@Data
public class LinkCountViewImpl implements LinkCountView {

  private UUID id;
  private Integer totalLinks;
}
