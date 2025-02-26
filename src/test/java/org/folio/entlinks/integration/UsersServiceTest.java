package org.folio.entlinks.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.folio.spring.client.UsersClient;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

  @Mock
  private UsersClient usersClient;

  @Mock
  private ResultList resultList;

  @InjectMocks
  private UsersService usersService;

  private UsersClient.User user;

  @BeforeEach
  void setUp() {
    user = UsersClient.User.builder().id("test-user-id").build();
  }

  @Test
  void shouldReturnSystemUserIdWhenUserFound() {
    when(usersClient.query(anyString())).thenReturn(resultList);
    when(resultList.getResult()).thenReturn(List.of(user));

    var result = usersService.getSystemUserId("query");

    assertEquals("test-user-id", result);
  }

  @Test
  void shouldReturnNullWhenUserNotFound() {
    when(usersClient.query(anyString())).thenReturn(resultList);
    when(resultList.getResult()).thenReturn(Collections.emptyList());

    var result = usersService.getSystemUserId("query");

    assertNull(result);
  }

  @Test
  void shouldReturnNullWhenQueryIsEmpty() {
    var result = usersService.getSystemUserId("");

    assertNull(result);
  }
}
