package org.folio.entlinks.service.messaging.authority.model;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.Getter;
import org.bouncycastle.util.Arrays;
import org.folio.entlinks.domain.dto.FieldChange;
import org.folio.entlinks.domain.dto.SubfieldChange;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.impl.SubfieldImpl;

public class FieldChangeHolder {

  private final InstanceAuthorityLinkingRule linkingRule;

  private final @Getter String bibField;

  private final List<Subfield> authSubfields;

  private final List<SubfieldChange> extraSubfieldChanges = new ArrayList<>();

  public FieldChangeHolder(DataField dataField, InstanceAuthorityLinkingRule linkingRule) {
    this.linkingRule = linkingRule;
    this.bibField = linkingRule.getBibField();
    this.authSubfields = getAuthSubfields(dataField.getSubfields());
  }

  public void addExtraSubfieldChange(SubfieldChange change) {
    if (change != null) {
      extraSubfieldChanges.add(change);
    }
  }

  public FieldChange toFieldChange() {
    var subfieldChanges = toSubfieldsChange();
    if (!extraSubfieldChanges.isEmpty()) {
      subfieldChanges.addAll(extraSubfieldChanges);
    }
    return new FieldChange().field(getBibField()).subfields(subfieldChanges);
  }

  private List<SubfieldChange> toSubfieldsChange() {
    var result = new ArrayList<SubfieldChange>();
    var subfieldCodes = new HashSet<Character>();

    // create subfield changes for subfields that exist in authority
    for (var subfield : authSubfields) {
      var code = subfield.getCode();
      var change = new SubfieldChange()
        .code(Character.toString(code))
        .value(subfield.getData());
      subfieldCodes.add(code);
      result.add(change);
    }

    // create subfield changes for subfields that missing in authority but still could be controlled
    for (char controlledSubfield : linkingRule.getAuthoritySubfields()) {
      var code = getCode(controlledSubfield);
      if (!subfieldCodes.contains(code)) {
        result.add(new SubfieldChange().code(Character.toString(code)).value(EMPTY));
      }
    }

    return result;
  }

  private List<Subfield> getAuthSubfields(List<Subfield> subfields) {
    var usualSubfields = new ArrayList<Subfield>();
    var modifiedSubfields = new ArrayList<Subfield>();
    for (var subfield : subfields) {
      if (Arrays.contains(linkingRule.getAuthoritySubfields(), subfield.getCode())) {
        var code = getCode(subfield);
        if (code == subfield.getCode()) {
          usualSubfields.add(subfield);
        } else {
          modifiedSubfields.add(new SubfieldImpl(code, subfield.getData()));
        }
      }
    }
    var newSubfields = new ArrayList<Subfield>();
    newSubfields.addAll(modifiedSubfields);
    newSubfields.addAll(usualSubfields);
    return newSubfields;
  }

  private char getCode(Subfield subfield) {
    return getCode(subfield.getCode());
  }

  private char getCode(char subfieldCode) {
    var subfieldModifications = linkingRule.getSubfieldModifications();
    if (subfieldModifications != null && !subfieldModifications.isEmpty()) {
      for (SubfieldModification subfieldModification : subfieldModifications) {
        if (subfieldModification.getSource().charAt(0) == subfieldCode) {
          return subfieldModification.getTarget().charAt(0);
        }
      }
    }
    return subfieldCode;
  }
}
