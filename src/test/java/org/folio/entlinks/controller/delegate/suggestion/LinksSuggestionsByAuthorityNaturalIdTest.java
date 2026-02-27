package org.folio.entlinks.controller.delegate.suggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.folio.entlinks.client.SourceStorageClient;
import org.folio.entlinks.controller.converter.SourceContentMapper;
import org.folio.entlinks.domain.dto.LinkDetails;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.integration.dto.FieldParsedContent;
import org.folio.entlinks.integration.dto.ParsedSubfield;
import org.folio.entlinks.service.consortium.UserTenantsService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingRulesService;
import org.folio.entlinks.service.links.LinksSuggestionsService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinksSuggestionsByAuthorityNaturalIdTest {
  @Mock
  private InstanceAuthorityLinkingRulesService linkingRulesService;
  @Mock
  private LinksSuggestionsService suggestionService;
  @Mock
  private AuthorityRepository authorityRepository;
  @Mock
  private SourceStorageClient sourceStorageClient;
  @Mock
  private SourceContentMapper contentMapper;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private UserTenantsService userTenantsService;

  @InjectMocks
  private LinksSuggestionsByAuthorityNaturalId delegate;

  @Test
  void extractIds_positive() {
    var expectedNaturalIds = List.of("test0", "test1", "test2");

    var subfields = expectedNaturalIds.subList(0, 2).stream()
      .map(s -> new ParsedSubfield('0', s))
      .toList();
    var linkDetails = new LinkDetails().authorityNaturalId(expectedNaturalIds.get(2));
    var field = new FieldParsedContent("100", "/", "/", subfields, linkDetails);

    var actual = delegate.extractIds(field);
    assertThat(actual).isEqualTo(new HashSet<>(expectedNaturalIds));
  }

  @Test
  void extractIds_negative_noSubfieldsAndNullLinkDetails() {
    var field = new FieldParsedContent("100", "/", "/", new ArrayList<>(), null);
    var actual = delegate.extractIds(field);
    assertThat(actual).isEmpty();
  }

  @Test
  void extractIds_negative_noSubfieldsAndNullNaturalId() {
    var field = new FieldParsedContent("100", "/", "/", new ArrayList<>(), new LinkDetails());
    var actual = delegate.extractIds(field);
    assertThat(actual).isEmpty();
  }

  @Test
  void findExistingAuthorities_positive() {
    var ids = new HashSet<String>();
    delegate.findExistingAuthorities(ids);
    verify(authorityRepository).findByNaturalIdInAndDeletedFalse(ids);
  }

  @Test
  void extractId_positive() {
    var authority = new Authority();
    authority.setNaturalId("test");
    var actual = delegate.extractId(authority);
    assertThat(actual).isEqualTo(authority.getNaturalId());
  }
}
