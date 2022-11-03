package org.folio.support.base;

import static org.folio.support.TestUtils.asJson;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.folio.support.base.TestConstants.USER_ID;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.support.extension.EnablePostgres;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Map;

@EnablePostgres
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class IntegrationTestBase {

  protected static MockMvc mockMvc;

  @BeforeAll
  static void setUp(@Autowired MockMvc mockMvc) {
    IntegrationTestBase.mockMvc = mockMvc;
    setUpTenant();
  }

  @SneakyThrows
  protected static void setUpTenant() {
    doPost("/_/tenant", new TenantAttributes().moduleTo("mod-entities-links"));
  }

  protected static HttpHeaders defaultHeaders() {
    var httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.add(XOkapiHeaders.TENANT, TENANT_ID);
    httpHeaders.add(XOkapiHeaders.USER_ID, USER_ID);

    return httpHeaders;
  }

  @SneakyThrows
  protected static ResultActions tryGet(String uri, Map<String, String> params, Object... args) {
    var requestParams = new LinkedMultiValueMap<String, String>();
    requestParams.setAll(params);
    return mockMvc.perform(get(uri, args).params(requestParams).headers(defaultHeaders()).accept(APPLICATION_JSON));
  }

  @SneakyThrows
  protected static ResultActions tryGet(String uri, Object... args) {
    return mockMvc.perform(get(uri, args).headers(defaultHeaders()).accept(APPLICATION_JSON));
  }

  @SneakyThrows
  protected static ResultActions doGet(String uri, Map<String, String> params, Object... args) {
    return tryGet(uri, params, args).andExpect(status().isOk());
  }

  @SneakyThrows
  protected static ResultActions doGet(String uri, Object... args) {
    return tryGet(uri, args).andExpect(status().isOk());
  }

  @SneakyThrows
  protected static ResultActions tryPut(String uri, Object body, Object... args) {
    return mockMvc.perform(put(uri, args).content(body == null ? "" : asJson(body)).headers(defaultHeaders()));
  }

  @SneakyThrows
  protected static ResultActions doPut(String uri, Object body, Object... args) {
    return tryPut(uri, body, args).andExpect(status().is2xxSuccessful());
  }

  @SneakyThrows
  protected static ResultActions tryPost(String uri, Object body, Object... args) {
    return mockMvc.perform(post(uri, args).content(asJson(body)).headers(defaultHeaders()));
  }

  @SneakyThrows
  protected static ResultActions doPost(String uri, Object body, Object... args) {
    return tryPost(uri, body, args).andExpect(status().is2xxSuccessful());
  }

  protected ResultMatcher errorParameterMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].parameters.[0].key", errorMessageMatcher);
  }

  protected ResultMatcher errorTypeMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].type", errorMessageMatcher);
  }

  protected ResultMatcher errorCodeMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].code", errorMessageMatcher);
  }

  protected ResultMatcher errorMessageMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].message", errorMessageMatcher);
  }

  protected ResultMatcher errorTotalMatch(int errorTotal) {
    return jsonPath("$.total_records", is(errorTotal));
  }
}
