package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.client.SettingsClient.AuthoritiesExpirationSettingValue;
import static org.folio.entlinks.integration.SettingsService.AUTHORITIES_EXPIRE_SETTING_KEY;
import static org.folio.entlinks.integration.SettingsService.AUTHORITIES_EXPIRE_SETTING_SCOPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.entlinks.client.SettingsClient;
import org.folio.entlinks.config.properties.AuthorityArchiveProperties;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityIdDto;
import org.folio.entlinks.domain.dto.AuthorityIdDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.repository.AuthorityArchiveRepository;
import org.folio.entlinks.integration.SettingsService;
import org.folio.entlinks.service.authority.AuthorityArchiveService;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityArchivePropagationService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityArchiveServiceDelegateTest {

  private static final String TENANT_ID = "tenantId";

  @Mock
  private AuthorityArchiveService service;

  @Mock
  private SettingsService settingsService;

  @Mock
  private AuthorityArchiveRepository authorityArchiveRepository;

  @Mock
  private AuthorityArchiveProperties authorityArchiveProperties;

  @Mock
  private AuthorityDomainEventPublisher eventPublisher;

  @Mock
  private ConsortiumAuthorityArchivePropagationService propagationService;

  @Mock
  private AuthorityMapper authorityMapper;

  @Mock
  private FolioExecutionContext context;

  @InjectMocks
  private AuthorityArchiveServiceDelegate delegate;

  @Test
  void shouldRetrieveAuthorityCollection_idsOnly() {
    var offset = 0;
    var limit = 2;
    var cql = "query";
    var total = 5;
    var page = new PageImpl<>(List.of(UUID.randomUUID(), UUID.randomUUID()), Pageable.unpaged(), total);

    when(service.getAllIds(offset, limit, cql)).thenReturn(page);

    var result = delegate.retrieveAuthorityArchives(offset, limit, cql, true);

    assertThat(result).isInstanceOf(AuthorityIdDtoCollection.class);
    var dtoResult = (AuthorityIdDtoCollection) result;
    assertThat(dtoResult.getTotalRecords()).isEqualTo(total);
    assertThat(dtoResult.getAuthorities())
        .extracting(AuthorityIdDto::getId)
        .containsExactlyElementsOf(page.getContent());
  }

  @Test
  void shouldNotExpireAuthorityArchivesWhenOperationDisabledBySettings() {
    var setting = new SettingsClient.SettingEntry(UUID.randomUUID(), AUTHORITIES_EXPIRE_SETTING_SCOPE,
        AUTHORITIES_EXPIRE_SETTING_KEY, new AuthoritiesExpirationSettingValue(false, null));
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.of(setting));

    delegate.expire();

    verifyNoInteractions(service);
    verifyNoInteractions(authorityArchiveRepository);
  }

  @Test
  void shouldExpireAuthorityArchivesWithDefaultRetentionPeriod() {
    var archive = new AuthorityArchive();
    var dto = new AuthorityDto();
    archive.setUpdatedDate(Timestamp.from(Instant.now().minus(10, ChronoUnit.DAYS)));
    when(authorityMapper.toDto(archive)).thenReturn(dto);
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.empty());
    when(authorityArchiveProperties.getRetentionPeriodInDays()).thenReturn(7);
    when(authorityArchiveRepository.streamByUpdatedTillDateAndSourcePrefix(any(LocalDateTime.class), anyString()))
        .thenReturn(Stream.of(archive));
    when(context.getTenantId()).thenReturn(TENANT_ID);

    delegate.expire();

    verify(service).delete(archive);
    verify(eventPublisher).publishHardDeleteEvent(dto);
    verify(propagationService)
        .propagate(archive, ConsortiumPropagationService.PropagationType.DELETE, TENANT_ID);
  }

  @Test
  void shouldExpireAuthorityArchivesWithRetentionPeriodFromSettings() {
    var archive = new AuthorityArchive();
    var dto = new AuthorityDto();
    var setting = new SettingsClient.SettingEntry(UUID.randomUUID(), AUTHORITIES_EXPIRE_SETTING_SCOPE,
        AUTHORITIES_EXPIRE_SETTING_KEY, new AuthoritiesExpirationSettingValue(true, 1));
    archive.setUpdatedDate(Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS)));
    when(authorityMapper.toDto(archive)).thenReturn(dto);
    when(settingsService.getAuthorityExpireSetting()).thenReturn(Optional.of(setting));
    when(authorityArchiveRepository.streamByUpdatedTillDateAndSourcePrefix(any(LocalDateTime.class), anyString()))
        .thenReturn(Stream.of(archive));
    when(context.getTenantId()).thenReturn(TENANT_ID);

    delegate.expire();

    verify(service).delete(archive);
    verify(eventPublisher).publishHardDeleteEvent(dto);
    verify(propagationService)
        .propagate(archive, ConsortiumPropagationService.PropagationType.DELETE, TENANT_ID);
  }
}
