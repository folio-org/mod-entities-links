package org.folio.entlinks.service.consortium.propagation;

import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.consortium.propagation.model.LinksPropagationData;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

@Service
public class ConsortiumLinksPropagationService extends ConsortiumPropagationService<LinksPropagationData> {

  private static final String ILLEGAL_PROPAGATION_MSG = "Propagation type '%s' is not supported for links.";

  private final InstanceAuthorityLinkingService instanceAuthorityLinkingService;

  protected ConsortiumLinksPropagationService(ConsortiumTenantsService tenantsService,
                                              SystemUserScopedExecutionService executionService,
                                              InstanceAuthorityLinkingService instanceAuthorityLinkingService) {
    super(tenantsService, executionService);
    this.instanceAuthorityLinkingService = instanceAuthorityLinkingService;
  }

  @Override
  protected void doPropagation(LinksPropagationData propagationData,
                               PropagationType propagationType) {
    var instanceId = propagationData.instanceId();
    var links = propagationData.links().stream()
        .map(InstanceAuthorityLink::new)
        .toList();
    switch (propagationType) {
      case CREATE, DELETE -> throw new IllegalArgumentException(ILLEGAL_PROPAGATION_MSG.formatted(propagationType));
      case UPDATE -> instanceAuthorityLinkingService.updateLinks(instanceId, links);
      default -> throw new IllegalStateException("Unexpected value: " + propagationType);
    }
  }
}
