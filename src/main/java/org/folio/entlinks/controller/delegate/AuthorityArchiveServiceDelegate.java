package org.folio.entlinks.controller.delegate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.SettingsClient;
import org.folio.entlinks.config.properties.AuthorityArchiveProperties;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityDtoCollection;
import org.folio.entlinks.domain.entity.AuthorityArchive;
import org.folio.entlinks.domain.entity.AuthorityBase;
import org.folio.entlinks.domain.repository.AuthorityArchiveRepository;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.integration.SettingsService;
import org.folio.entlinks.service.authority.AuthorityArchiveService;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityArchivePropagationService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthorityArchiveServiceDelegate {

  private final AuthorityArchiveService authorityArchiveService;
  private final SettingsService settingsService;
  private final AuthorityArchiveRepository authorityArchiveRepository;
  private final AuthorityArchiveProperties authorityArchiveProperties;
  private final AuthorityDomainEventPublisher eventPublisher;
  private final ConsortiumAuthorityArchivePropagationService propagationService;
  private final AuthorityMapper authorityMapper;
  private final FolioExecutionContext context;

  public AuthorityDtoCollection retrieveAuthorityArchives(Integer offset, Integer limit, String cqlQuery,
                                                          Boolean idOnly) {
    if (Boolean.TRUE.equals(idOnly)) {
      var entities = authorityArchiveService.getAllIds(offset, limit, cqlQuery)
        .map(id -> new AuthorityDto().id(id)).stream().toList();
      return new AuthorityDtoCollection(entities, entities.size());
    }

    var entitiesPage = authorityArchiveService.getAll(offset, limit, cqlQuery)
      .map(AuthorityBase.class::cast);
    return authorityMapper.toAuthorityCollection(entitiesPage);
  }

  @Transactional(readOnly = true)
  public void expire() {
    var retention = fetchAuthoritiesRetentionDuration();

    if (retention.isEmpty()) {
      return;
    }

    var tillDate = LocalDateTime.now().minusDays(retention.get());
    try (Stream<AuthorityArchive> archives = authorityArchiveRepository.streamByUpdatedTillDate(tillDate)) {
      archives.forEach(this::process);
    }
  }

  private void process(AuthorityArchive archive) {
    authorityArchiveService.delete(archive);
    var dto = authorityMapper.toDto(archive);
    eventPublisher.publishHardDeleteEvent(dto);
    propagationService.propagate(archive, ConsortiumPropagationService.PropagationType.DELETE, context.getTenantId());
  }

  private Optional<Integer> fetchAuthoritiesRetentionDuration() {
    Optional<SettingsClient.SettingEntry> expireSetting;
    try {
      expireSetting = settingsService.getAuthorityExpireSetting();
    } catch (FolioIntegrationException e) {
      log.warn("Exception during settings fetching: ", e);
      expireSetting = Optional.empty();
    }

    if (expireSetting.isPresent() && expireSetting.get().value() != null
        && Boolean.FALSE.equals(expireSetting.get().value().expirationEnabled())) {
      log.info("Authority archives expiration is disabled for the tenant through setting");
      return Optional.empty();
    }

    return expireSetting
      .map(SettingsClient.SettingEntry::value)
      .map(SettingsClient.AuthoritiesExpirationSettingValue::retentionInDays)
      .or(() -> {
        log.warn("No Retention setting was defined for Authorities Expiration, using the default one: {} days",
          authorityArchiveProperties.getRetentionPeriodInDays());
        return Optional.of(authorityArchiveProperties.getRetentionPeriodInDays());
      });
  }
}
