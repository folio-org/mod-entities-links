//package org.folio.entlinks.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.folio.entlinks.LinkingPairType.INSTANCE_AUTHORITY;
//import static org.mockito.Mockito.when;
//
//import org.folio.entlinks.model.converter.LinkingRulesMapperImpl;
//import org.folio.entlinks.model.entity.InstanceAuthorityLinkingRule;
//import org.folio.entlinks.repository.LinkingRulesRepository;
//import org.folio.spring.test.type.UnitTest;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.boot.json.JsonParseException;
//
//@UnitTest
//@ExtendWith(MockitoExtension.class)
//class InstanceAuthorityLinkingRuleServiceTest {
//
//  @Mock
//  private LinkingRulesRepository repository;
//
//  private LinkingRulesService service;
//
//  @BeforeEach
//  void setUp() {
//    service = new LinkingRulesService(repository, new LinkingRulesMapperImpl());
//  }
//
//  @Test
//  void getInstanceAuthorityRules_negative_invalidJsonFormat() {
//    var invalidRules = InstanceAuthorityLinkingRule.builder()
//      .linkingPairType(INSTANCE_AUTHORITY.name())
//      .jsonb("invalid json")
//      .build();
//
//    when(repository.findByLinkingPairType(INSTANCE_AUTHORITY.name())).thenReturn(invalidRules);
//
//    var exception = Assertions.assertThrows(JsonParseException.class,
//      () -> service.getLinkingRules(INSTANCE_AUTHORITY));
//
//    assertThat(exception)
//      .hasMessage("Cannot parse JSON");
//  }
//}