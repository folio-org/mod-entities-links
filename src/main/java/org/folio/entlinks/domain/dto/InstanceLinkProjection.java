package org.folio.entlinks.domain.dto;

import java.sql.Timestamp;
import java.util.UUID;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkStatus;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;

/**
 * DTO record for native SQL query projection combining InstanceAuthorityLink and authority natural ID.
 */
public record InstanceLinkProjection(
    Long id,
    UUID authorityId,
    UUID instanceId,
    String status,
    String errorCause,
    Timestamp updatedAt,
    Integer linkingRuleId,
    String authorityNaturalId
) {

  /**
   * Converts this projection to InstanceAuthorityLink entity with authority natural ID and linking rule ID set.
   */
  public InstanceAuthorityLink toEntity() {
    var link = new InstanceAuthorityLink();
    link.setId(id);
    link.setAuthorityId(authorityId);
    link.setInstanceId(instanceId);

    if (status != null) {
      link.setStatus(InstanceAuthorityLinkStatus.valueOf(status));
    }
    link.setErrorCause(errorCause);
    link.setUpdatedAt(updatedAt);

    // Set linking rule with just the ID (no need to fetch full rule data)
    if (linkingRuleId != null) {
      var rule = new InstanceAuthorityLinkingRule();
      rule.setId(linkingRuleId);
      link.setLinkingRule(rule);
    }

    link.setAuthorityNaturalId(authorityNaturalId);

    return link;
  }
}
