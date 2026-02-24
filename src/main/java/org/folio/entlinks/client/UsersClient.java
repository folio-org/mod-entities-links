package org.folio.entlinks.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "users", contentType = APPLICATION_JSON_VALUE)
public interface UsersClient {

  @GetExchange
  UserCollection query(@RequestParam("query") String query);

  @JsonIgnoreProperties(ignoreUnknown = true)
  record User(String id, String username, Personal personal) { }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Personal(@Nullable String firstName, String lastName) {
    public Personal(String lastName) {
      this(null, lastName);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record UserCollection(List<User> users) { }
}
