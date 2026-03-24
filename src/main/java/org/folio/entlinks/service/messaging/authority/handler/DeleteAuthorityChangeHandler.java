package org.folio.entlinks.service.messaging.authority.handler;

import static java.util.Collections.emptyList;
import static org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType.DELETE;

import java.util.List;
import java.util.stream.Collectors;
import org.folio.entlinks.config.properties.InstanceAuthorityChangeProperties;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeHolder;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.springframework.stereotype.Component;

@Component
public class DeleteAuthorityChangeHandler extends AbstractAuthorityChangeHandler {

  private final InstanceAuthorityLinkingService linkingService;

  public DeleteAuthorityChangeHandler(InstanceAuthorityLinkingService linkingService,
                                      InstanceAuthorityChangeProperties instanceAuthorityChangeProperties) {
    super(instanceAuthorityChangeProperties, linkingService);
    this.linkingService = linkingService;
  }

  @Override
  public List<LinksChangeEvent> handle(List<AuthorityChangeHolder> changes) {
    if (changes == null || changes.isEmpty()) {
      return emptyList();
    }

    var linksEvents = changes.stream()
      .filter(change -> change.getNumberOfLinks() > 0)
      .map(change -> handleLinksByPartitions(
        change.getAuthorityId(),
        links -> constructEvent(change.getAuthorityDataStatId(), change.getAuthorityId(), links, emptyList())
      ))
      .flatMap(List::stream)
      .toList();

    var authorityIds = linksEvents.stream().map(LinksChangeEvent::getAuthorityId).collect(Collectors.toSet());

    // delete the links
    linkingService.deleteByAuthorityIdIn(authorityIds);
    return linksEvents;
  }

  @Override
  public LinksChangeEvent.TypeEnum getReplyEventType() {
    return LinksChangeEvent.TypeEnum.DELETE;
  }

  @Override
  public AuthorityChangeType supportedAuthorityChangeType() {
    return DELETE;
  }
}
