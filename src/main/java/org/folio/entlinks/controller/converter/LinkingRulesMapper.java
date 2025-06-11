package org.folio.entlinks.controller.converter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.dto.LinkingRuleDto;
import org.folio.entlinks.domain.dto.LinkingRulePatchRequest;
import org.folio.entlinks.domain.dto.SubfieldValidation;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LinkingRulesMapper {

  List<LinkingRuleDto> convert(List<InstanceAuthorityLinkingRule> instanceAuthorityLinkingRule);

  @Mapping(target = "validation", expression = "java(convert(source.getSubfieldsExistenceValidations()))")
  LinkingRuleDto convert(InstanceAuthorityLinkingRule source);

  @Mapping(target = "subfieldsExistenceValidations", ignore = true)
  @Mapping(target = "subfieldModifications", ignore = true)
  @Mapping(target = "bibField", ignore = true)
  @Mapping(target = "authorityField", ignore = true)
  InstanceAuthorityLinkingRule convert(LinkingRulePatchRequest patchRequest);

  default SubfieldValidation convert(Map<String, Boolean> existence) {
    if (existence == null || existence.isEmpty()) {
      return null;
    }
    return new SubfieldValidation().existence(List.of(existence));
  }

  /**
   * Converts a set of strings to a sorted character array containing the first character of each string.
   * Letters are sorted before non-letters, and characters are sorted naturally within their groups.
   *
   * @param set the input list of strings
   * @return sorted array of first characters from non-empty strings, or empty array if input is null
   */
  default char[] stringListToCharArray(Set<String> set) {
    if (set == null) {
      return new char[0];
    }

    var firstCharacters = set.stream()
      .filter(StringUtils::isNotEmpty)
      .map(s -> s.charAt(0))
      .sorted(this::compareCharacters)
      .toList();

    char[] result = new char[firstCharacters.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = firstCharacters.get(i);
    }
    return result;
  }

  private int compareCharacters(char first, char second) {
    boolean isFirstLetter = Character.isLetter(first);
    boolean isSecondLetter = Character.isLetter(second);

    if (isFirstLetter && !isSecondLetter) {
      return -1;
    }
    if (!isFirstLetter && isSecondLetter) {
      return 1;
    }
    return Character.compare(Character.toLowerCase(first), Character.toLowerCase(second));
  }
}
