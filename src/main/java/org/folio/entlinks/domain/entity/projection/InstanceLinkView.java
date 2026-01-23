package org.folio.entlinks.domain.entity.projection;

import org.folio.entlinks.domain.entity.InstanceAuthorityLink;

/**
 * Projection for {@link InstanceAuthorityLink} with authority natural ID.
 */
public interface InstanceLinkView {

  InstanceAuthorityLink getLink();

  String getAuthorityNaturalId();
}
