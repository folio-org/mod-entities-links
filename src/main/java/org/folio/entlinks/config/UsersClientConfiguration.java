package org.folio.entlinks.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "folio.system-user.enabled", havingValue = "false", matchIfMissing = true)
public class UsersClientConfiguration {
}
