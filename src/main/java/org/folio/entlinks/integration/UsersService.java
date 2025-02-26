package org.folio.entlinks.integration;

import static org.folio.entlinks.config.constants.CacheNames.SYSTEM_USER_CACHE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.client.UsersClient;
import org.folio.spring.model.ResultList;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class UsersService {

  private final UsersClient usersClient;

  @Cacheable(cacheNames = SYSTEM_USER_CACHE,
      key = "@folioExecutionContext.tenantId",
      unless = "#result == null || #result.isEmpty()")
  public String getSystemUserId(String query) {
    log.debug("Get system user by query: {}", query);
    ResultList<UsersClient.User> users = StringUtils.isEmpty(query) ? ResultList.empty() : usersClient.query(query);
    var user = users.getResult()
        .stream()
        .findFirst().orElse(null);
    if (user == null) {
      log.warn("System user not found by query: {}", query);
      return null;
    }
    return user.getId();
  }
}
