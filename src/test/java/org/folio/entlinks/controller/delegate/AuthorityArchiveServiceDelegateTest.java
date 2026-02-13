package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.service.settings.TenantSetting.ARCHIVES_EXPIRATION_ENABLED;
import static org.folio.entlinks.service.settings.TenantSetting.ARCHIVES_EXPIRATION_PERIOD;
import static org.mockito.ArgumentMatchers.any;
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
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityIdDto;
import org.folio.entlinks.domain.dto.AuthorityIdDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.repository.AuthorityArchiveRepository;
import org.folio.entlinks.service.authority.AuthorityArchiveService;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingCollection;
import org.folio.tenant.settings.service.TenantSettingsService;
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

  @Mock private AuthorityArchiveService service;
  @Mock private TenantSettingsService tenantSettingsService;
  @Mock private AuthorityArchiveRepository authorityArchiveRepository;
  @Mock private AuthorityDomainEventPublisher eventPublisher;
  @Mock private AuthorityMapper authorityMapper;

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
    var enabledSetting = new Setting()
      .key(ARCHIVES_EXPIRATION_ENABLED.getKey())
      .value(Boolean.FALSE);
    var periodSetting = new Setting()
      .key(ARCHIVES_EXPIRATION_PERIOD.getKey())
      .value(7);
    var settingCollection = new SettingCollection()
      .settings(List.of(enabledSetting, periodSetting));
    when(tenantSettingsService.getGroupSettings(ARCHIVES_EXPIRATION_ENABLED.getGroup()))
      .thenReturn(Optional.of(settingCollection));

    delegate.expire();

    verifyNoInteractions(service);
    verifyNoInteractions(authorityArchiveRepository);
  }

  @Test
  void shouldNotExpireAuthorityArchivesWhenNoSettingsFound() {
    when(tenantSettingsService.getGroupSettings(ARCHIVES_EXPIRATION_ENABLED.getGroup()))
      .thenReturn(Optional.empty());

    delegate.expire();

    verifyNoInteractions(service);
    verifyNoInteractions(authorityArchiveRepository);
  }

  @Test
  void shouldExpireAuthorityArchivesWithRetentionPeriodFromSettings() {
    var archive = new AuthorityArchive();
    var dto = new AuthorityDto();
    var enabledSetting = new Setting()
      .key(ARCHIVES_EXPIRATION_ENABLED.getKey())
      .value(Boolean.TRUE);
    var periodSetting = new Setting()
      .key(ARCHIVES_EXPIRATION_PERIOD.getKey())
      .value(1);
    var settingCollection = new SettingCollection()
      .settings(List.of(enabledSetting, periodSetting));

    archive.setUpdatedDate(Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS)));
    when(authorityMapper.toDto(archive)).thenReturn(dto);
    when(tenantSettingsService.getGroupSettings(ARCHIVES_EXPIRATION_ENABLED.getGroup()))
      .thenReturn(Optional.of(settingCollection));
    when(authorityArchiveRepository.streamByUpdatedTillDateAndSourcePrefix(any(LocalDateTime.class)))
        .thenReturn(Stream.of(archive));

    delegate.expire();

    verify(service).delete(archive);
    verify(eventPublisher).publishHardDeleteEvent(dto);
  }
}
