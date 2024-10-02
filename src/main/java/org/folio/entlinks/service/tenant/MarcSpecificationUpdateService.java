package org.folio.entlinks.service.tenant;

import lombok.RequiredArgsConstructor;
import org.folio.entlinks.domain.entity.InstanceAuthorityLinkingRule;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.rspec.domain.dto.DefinitionType;
import org.folio.rspec.domain.dto.Family;
import org.folio.rspec.domain.dto.FamilyProfile;
import org.folio.rspec.domain.dto.Scope;
import org.folio.rspec.domain.dto.SubfieldDto;
import org.folio.rspec.domain.dto.SubfieldUpdateRequestEvent;
import org.folio.rspec.domain.dto.UpdateRequestEvent;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarcSpecificationUpdateService {

  private static final String SUBFIELD_9_CODE = "9";
  private static final String SUBFIELD_9_LABEL = "Linked authority UUID";

  private final InstanceAuthorityLinkingRulesService linkingRulesService;
  private final EventProducer<UpdateRequestEvent> eventProducer;

  @Retryable(maxAttempts = 10, backoff = @Backoff(delay = 5000))
  public void sendSpecificationRequests() {
    var requestEvents = linkingRulesService.getLinkingRules().stream()
      .map(InstanceAuthorityLinkingRule::getBibField)
      .distinct()
      .map(this::toSubfieldCreationRequest)
      .toList();

    eventProducer.sendMessages(requestEvents);
  }

  private UpdateRequestEvent toSubfieldCreationRequest(String tag) {
    var requestEvent = new SubfieldUpdateRequestEvent();
    requestEvent.setFamily(Family.MARC);
    requestEvent.setProfile(FamilyProfile.BIBLIOGRAPHIC);
    requestEvent.setDefinitionType(DefinitionType.SUBFIELD);
    requestEvent.setTargetFieldTag(tag);
    requestEvent.setSubfield(new SubfieldDto()
      .code(SUBFIELD_9_CODE)
      .label(SUBFIELD_9_LABEL)
      .deprecated(false)
      .required(false)
      .repeatable(false)
      .scope(Scope.SYSTEM)
    );
    return requestEvent;
  }
}
