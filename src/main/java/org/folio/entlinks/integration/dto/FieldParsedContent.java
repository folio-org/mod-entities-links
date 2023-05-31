package org.folio.entlinks.integration.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.folio.entlinks.domain.dto.LinkDetails;

@Getter
@Setter
@AllArgsConstructor
public class FieldParsedContent {
  private String ind1;
  private String ind2;
  private Map<String, String> subfields;
  private LinkDetails linkDetails;
}
