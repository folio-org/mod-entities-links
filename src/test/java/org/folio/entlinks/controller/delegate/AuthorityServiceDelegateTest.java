package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityIdDto;
import org.folio.entlinks.domain.dto.AuthorityIdDtoCollection;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.entlinks.service.authority.AuthorityUpdateResult;
import org.folio.entlinks.service.consortium.UserTenantsService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityDataStatsPropagationService;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

//todo: update tests
@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityServiceDelegateTest {

  private final ArgumentCaptor<AuthorityDto> captor = ArgumentCaptor.forClass(AuthorityDto.class);
  @Mock private AuthorityService service;
  @Mock private AuthorityMapper mapper;
  @Mock private AuthorityDomainEventPublisher eventPublisher;
  @Mock private FolioExecutionContext context;
  @Mock private UserTenantsService userTenantsService;
  @Mock private AuthorityDataStatService dataStatService;
  @Mock private ConsortiumAuthorityDataStatsPropagationService dataStatPropagationService;
  @InjectMocks
  private AuthorityServiceDelegate delegate;

  @BeforeEach
  void setUp() {
    lenient().when(context.getTenantId()).thenReturn(TENANT_ID);
    lenient().when(userTenantsService.getCentralTenant(any())).thenReturn(Optional.empty());
  }

  @Test
  void shouldRetrieveAuthorityCollection_idsOnly() {
    var offset = 0;
    var limit = 2;
    var cql = "query";
    var total = 5;
    var page = new PageImpl<>(List.of(UUID.randomUUID(), UUID.randomUUID()), Pageable.unpaged(), total);

    when(service.getAllIds(offset, limit, cql)).thenReturn(page);

    var result = delegate.retrieveAuthorityCollection(offset, limit, cql, true);

    assertThat(result).isInstanceOf(AuthorityIdDtoCollection.class);
    var dtoResult = (AuthorityIdDtoCollection) result;
    assertThat(dtoResult.getTotalRecords()).isEqualTo(total);
    assertThat(dtoResult.getAuthorities())
      .extracting(AuthorityIdDto::getId)
      .containsExactlyElementsOf(page.getContent());
  }

  @Test
  void shouldCreateAuthority() {
    // given
    var id = UUID.randomUUID();
    var entity = new Authority();
    entity.setId(id);
    var expectedDto = new AuthorityDto().id(id);
    var dto = new AuthorityDto().id(id);
    when(mapper.toEntity(any(AuthorityDto.class))).thenReturn(entity);
    when(service.create(entity)).thenReturn(entity);
    when(mapper.toDto(any(Authority.class))).thenReturn(expectedDto);

    // when
    var created = delegate.createAuthority(dto);

    // then
    verify(eventPublisher).publishCreateEvent(captor.capture());
    assertEquals(expectedDto, created);
    assertEquals(expectedDto, captor.getValue());
  }

  @Test
  void shouldUpdateAuthority() {
    // given
    var id = UUID.randomUUID();
    var modificationDto = new AuthorityDto().id(id);
    var existingEntity = new Authority();
    existingEntity.setId(id);
    var modifiedEntity = new Authority();
    modifiedEntity.setId(id);
    var oldDto = new AuthorityDto().id(id);
    var newDto = new AuthorityDto().id(id);

    when(mapper.toEntity(modificationDto)).thenReturn(modifiedEntity);
    when(mapper.toDto(any(Authority.class))).thenReturn(oldDto).thenReturn(newDto);
    when(service.update(eq(modifiedEntity), anyBoolean()))
      .thenReturn(new AuthorityUpdateResult(existingEntity, modifiedEntity));
    var captor2 = ArgumentCaptor.forClass(AuthorityDto.class);

    // when
    delegate.updateAuthority(id, modificationDto);

    // then
    verify(eventPublisher).publishUpdateEvent(captor.capture(), captor2.capture());
    assertEquals(oldDto, captor.getValue());
    assertEquals(newDto, captor2.getValue());
    verify(service).update(any(Authority.class), anyBoolean());
    verifyNoMoreInteractions(service);
    verify(mapper, times(2)).toDto(any(Authority.class));
    verify(mapper).toEntity(any(AuthorityDto.class));
    verifyNoMoreInteractions(mapper);
  }

  @Test
  void shouldDeleteAuthority() {
    // given
    var id = UUID.randomUUID();
    var entity = new Authority();
    entity.setId(id);
    var dto = new AuthorityDto().id(id);
    when(mapper.toDto(entity)).thenReturn(dto);
    when(service.deleteById(id)).thenReturn(entity);

    // when
    delegate.deleteAuthorityById(id);

    // then
    verify(eventPublisher).publishSoftDeleteEvent(captor.capture());
    assertEquals(dto, captor.getValue());
    verify(service).deleteById(id);
    verifyNoMoreInteractions(service);
    verify(mapper).toDto(any(Authority.class));
  }
}
