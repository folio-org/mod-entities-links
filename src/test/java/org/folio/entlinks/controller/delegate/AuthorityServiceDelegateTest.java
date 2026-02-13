package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.folio.entlinks.config.properties.LocalStorageProperties;
import org.folio.entlinks.controller.converter.AuthorityMapper;
import org.folio.entlinks.domain.dto.AuthorityBulkRequest;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.AuthorityDtoCollection;
import org.folio.entlinks.domain.dto.AuthorityIdDto;
import org.folio.entlinks.domain.dto.AuthorityIdDtoCollection;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.service.authority.AuthoritiesBulkContext;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.entlinks.service.authority.AuthorityS3Service;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.entlinks.service.authority.AuthorityUpdateResult;
import org.folio.entlinks.service.consortium.UserTenantsService;
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
  @Mock private AuthorityS3Service authorityS3Service;
  @Mock private LocalStorageProperties localStorageProperties;
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

    verify(service).getAllIds(offset, limit, cql);
    verifyNoMoreInteractions(service);
  }

  @Test
  void shouldCreateAuthority() {
    // given
    var id = UUID.randomUUID();
    var entity = new Authority();
    entity.setId(id);
    var expectedDto = new AuthorityDto().id(id);
    var dto = new AuthorityDto().id(id);
    when(mapper.toEntity(dto)).thenReturn(entity);
    when(service.create(entity)).thenReturn(entity);
    when(mapper.toDto(entity)).thenReturn(expectedDto);

    // when
    var created = delegate.createAuthority(dto);

    // then
    assertEquals(expectedDto, created);
    verify(eventPublisher).publishCreateEvent(captor.capture());
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
    when(service.deleteById(id)).thenReturn(entity);
    when(mapper.toDto(entity)).thenReturn(dto);

    // when
    delegate.deleteAuthorityById(id);

    // then
    verify(eventPublisher).publishSoftDeleteEvent(captor.capture());
    assertEquals(dto, captor.getValue());
    verify(dataStatService).deleteByAuthorityId(id);
    verifyNoMoreInteractions(service);
  }

  @Test
  void shouldRetrieveAuthorityCollection_fullAuthorities() {
    // given
    var offset = 0;
    var limit = 2;
    var cql = "query";
    var total = 5;
    var authority1 = new Authority();
    authority1.setId(UUID.randomUUID());
    var authority2 = new Authority();
    authority2.setId(UUID.randomUUID());
    var page = new PageImpl<>(List.of(authority1, authority2), Pageable.unpaged(), total);
    var dto1 = new AuthorityDto().id(authority1.getId());
    var dto2 = new AuthorityDto().id(authority2.getId());
    var expectedCollection = new AuthorityDtoCollection(List.of(dto1, dto2), total);

    when(service.getAll(offset, limit, cql)).thenReturn(page);
    when(mapper.toAuthorityCollection(any())).thenReturn(expectedCollection);

    // when
    var result = delegate.retrieveAuthorityCollection(offset, limit, cql, false);

    // then
    assertThat(result)
      .isEqualTo(expectedCollection)
      .isInstanceOf(AuthorityDtoCollection.class);
    verify(service).getAll(offset, limit, cql);
    verify(mapper).toAuthorityCollection(any());
    verifyNoMoreInteractions(service);
  }

  @Test
  void shouldGetAuthorityById() {
    // given
    var id = UUID.randomUUID();
    var entity = new Authority();
    entity.setId(id);
    var expectedDto = new AuthorityDto().id(id);

    when(service.getById(id)).thenReturn(entity);
    when(mapper.toDto(entity)).thenReturn(expectedDto);

    // when
    var result = delegate.getAuthorityById(id);

    // then
    assertThat(result).isEqualTo(expectedDto);
    verify(service).getById(id);
    verify(mapper).toDto(entity);
  }

  @Test
  void shouldThrowExceptionWhenUpdateAuthorityWithMismatchedId() {
    // given
    var pathId = UUID.randomUUID();
    var bodyId = UUID.randomUUID();
    var modificationDto = new AuthorityDto().id(bodyId);

    // when & then
    assertThatThrownBy(() -> delegate.updateAuthority(pathId, modificationDto))
      .isInstanceOf(RequestBodyValidationException.class)
      .hasMessageContaining("Request should have id = " + pathId);
  }

  @Test
  void shouldCreateAuthoritiesInBulk() {
    // given
    var fileName = "authorities.json";
    var subPath = "test/path";
    var errorsCount = 0;
    var request = new AuthorityBulkRequest().recordsFileName(fileName);

    when(localStorageProperties.getS3LocalSubPath()).thenReturn(subPath);
    when(authorityS3Service.processAuthorities(any(AuthoritiesBulkContext.class), any(Consumer.class)))
      .thenReturn(errorsCount);

    // when
    var result = delegate.createAuthorities(request);

    // then
    assertNotNull(result);
    assertThat(result.getErrorsNumber()).isEqualTo(errorsCount);
    assertThat(result.getErrorRecordsFileName()).isNull();
    assertThat(result.getErrorsFileName()).isNull();
    verify(localStorageProperties).getS3LocalSubPath();
    verify(authorityS3Service).processAuthorities(any(AuthoritiesBulkContext.class), any(Consumer.class));
  }

  @Test
  void shouldCreateAuthoritiesInBulk_withErrors() {
    // given
    var fileName = "authorities.json";
    var subPath = "test/path";
    var errorsCount = 5;
    var request = new AuthorityBulkRequest().recordsFileName(fileName);

    when(localStorageProperties.getS3LocalSubPath()).thenReturn(subPath);
    when(authorityS3Service.processAuthorities(any(AuthoritiesBulkContext.class), any(Consumer.class)))
      .thenReturn(errorsCount);

    // when
    var result = delegate.createAuthorities(request);

    // then
    assertNotNull(result);
    assertThat(result.getErrorsNumber()).isEqualTo(errorsCount);
    assertThat(result.getErrorRecordsFileName()).isNotNull();
    assertThat(result.getErrorsFileName()).isNotNull();
    verify(localStorageProperties).getS3LocalSubPath();
    verify(authorityS3Service).processAuthorities(any(AuthoritiesBulkContext.class), any(Consumer.class));
  }

  @Test
  void shouldUseConsortiumServiceWhenCentralTenantPresent() {
    // given
    var centralTenant = "central";
    when(userTenantsService.getCentralTenant(TENANT_ID)).thenReturn(Optional.of(centralTenant));

    var consortiumService = mock(AuthorityService.class);
    // Re-initialize delegate to pick up the consortium service
    var consortiumDelegate = new AuthorityServiceDelegate(
      service,
      consortiumService,
      mapper,
      context,
      eventPublisher,
      authorityS3Service,
      localStorageProperties,
      userTenantsService,
      dataStatService
    );

    var offset = 0;
    var limit = 2;
    var cql = "query";
    var total = 5;
    var page = new PageImpl<>(List.of(UUID.randomUUID(), UUID.randomUUID()), Pageable.unpaged(), total);

    when(consortiumService.getAllIds(offset, limit, cql)).thenReturn(page);

    // when
    var result = consortiumDelegate.retrieveAuthorityCollection(offset, limit, cql, true);

    // then
    assertThat(result).isInstanceOf(AuthorityIdDtoCollection.class);
    verify(consortiumService).getAllIds(offset, limit, cql);
    verifyNoInteractions(service); // regular service should not be called
  }
}


