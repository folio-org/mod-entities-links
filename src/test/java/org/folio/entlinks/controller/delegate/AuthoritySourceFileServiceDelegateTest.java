package org.folio.entlinks.controller.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.CREATE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.DELETE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumPropagationService.PropagationType.UPDATE;
import static org.folio.support.base.TestConstants.CENTRAL_TENANT_ID;
import static org.folio.support.base.TestConstants.INPUT_BASE_URL;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.folio.entlinks.controller.converter.AuthoritySourceFileMapper;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFileDtoCollection;
import org.folio.entlinks.domain.dto.AuthoritySourceFileHridDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePatchDtoHridManagement;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDto;
import org.folio.entlinks.domain.dto.AuthoritySourceFilePostDtoHridManagement;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileSource;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.service.authority.AuthoritySourceFileDomainEventPublisher;
import org.folio.entlinks.service.authority.AuthoritySourceFileService;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.consortium.UserTenantsService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthoritySourceFilePropagationService;
import org.folio.entlinks.service.consortium.propagation.model.AuthoritySourceFilePropagationData;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.testing.type.UnitTest;
import org.folio.support.TestDataUtils;
import org.folio.tenant.domain.dto.Parameter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthoritySourceFileServiceDelegateTest {

  private static final String SANITIZED_BASE_URL = "id.loc.gov/authorities/test-source/";

  @Mock
  private AuthoritySourceFileMapper mapper;
  @Mock
  private AuthoritySourceFileService service;
  @Mock
  private UserTenantsService tenantsService;
  @Mock
  private ConsortiumAuthoritySourceFilePropagationService propagationService;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private SystemUserScopedExecutionService executionService;
  @Mock
  private ConsortiumTenantsService consortiumTenantsService;
  @Mock
  private AuthoritySourceFileDomainEventPublisher eventPublisher;

  @InjectMocks
  private AuthoritySourceFileServiceDelegate delegate;

  @Captor
  private ArgumentCaptor<AuthoritySourceFile> sourceFileArgumentCaptor;

  @Test
  void shouldGetSourceFileCollectionByQuery() {
    var expectedCollection = new AuthoritySourceFileDtoCollection();
    when(service.getAll(any(Integer.class), any(Integer.class), any(String.class)))
      .thenReturn(new PageImpl<>(List.of()));
    when(mapper.toAuthoritySourceFileCollection(any())).thenReturn(expectedCollection);

    var sourceFiles = delegate.getAuthoritySourceFiles(0, 100, "cql.allRecords=1");

    assertEquals(expectedCollection, sourceFiles);
  }

  @Test
  void shouldGetSourceFileById() {
    var sourceFile = new AuthoritySourceFile();
    var id = UUID.randomUUID();
    var expected = new AuthoritySourceFileDto();
    when(service.getById(id)).thenReturn(sourceFile);
    when(mapper.toDto(sourceFile)).thenReturn(expected);

    var sourceFileDto = delegate.getAuthoritySourceFileById(id);

    assertEquals(expected, sourceFileDto);
  }

  @Test
  void shouldNormalizeBaseUrlForSourceFileCreate() {
    var dto = new AuthoritySourceFilePostDto().baseUrl(INPUT_BASE_URL)
      .hridManagement(new AuthoritySourceFilePostDtoHridManagement().startNumber(10));
    var expected = new AuthoritySourceFile();
    expected.setBaseUrl(INPUT_BASE_URL);
    expected.setSequenceName("sequence_name");
    expected.setHridStartNumber(dto.getHridManagement().getStartNumber());

    when(mapper.toEntity(dto)).thenReturn(expected);
    when(service.create(expected)).thenReturn(expected);
    when(mapper.toDto(expected)).thenReturn(new AuthoritySourceFileDto());
    when(context.getTenantId()).thenReturn(TENANT_ID);

    delegate.createAuthoritySourceFile(dto);

    assertEquals(SANITIZED_BASE_URL, expected.getBaseUrl());
    verify(mapper).toEntity(dto);
    verify(service).create(expected);
    verify(mapper).toDto(expected);
    verify(service).createSequence(expected.getSequenceName(), expected.getHridStartNumber());
    verify(propagationService).propagate(getMockData(expected, null), CREATE, TENANT_ID);
    verifyNoMoreInteractions(mapper, service);
  }

  @Test
  void shouldNormalizeBaseUrlForSourceFileCreateOnConsortiumCentralTenant() {
    var dto = new AuthoritySourceFilePostDto().baseUrl(INPUT_BASE_URL)
      .hridManagement(new AuthoritySourceFilePostDtoHridManagement().startNumber(10));
    var expected = new AuthoritySourceFile();
    expected.setBaseUrl(INPUT_BASE_URL);
    expected.setSequenceName("sequence_name");
    expected.setHridStartNumber(dto.getHridManagement().getStartNumber());

    mockAsConsortiumCentralTenant();
    when(mapper.toEntity(dto)).thenReturn(expected);
    when(service.create(expected)).thenReturn(expected);
    when(mapper.toDto(expected)).thenReturn(new AuthoritySourceFileDto());

    delegate.createAuthoritySourceFile(dto);

    assertEquals(SANITIZED_BASE_URL, expected.getBaseUrl());
    verify(mapper).toEntity(dto);
    verify(service).create(expected);
    verify(mapper).toDto(expected);
    verify(service).createSequence(expected.getSequenceName(), expected.getHridStartNumber());
    verify(propagationService).propagate(getMockData(expected, null), CREATE, CENTRAL_TENANT_ID);
    verifyNoMoreInteractions(mapper, service);
  }

  @Test
  void shouldNormalizeBaseUrlForSourceFilePartialUpdate() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setBaseUrl(INPUT_BASE_URL);
    existing.setSource(AuthoritySourceFileSource.FOLIO);
    var expected = new AuthoritySourceFile(existing);
    expected.setBaseUrl(SANITIZED_BASE_URL);

    mockAsNonConsortiumTenant();
    when(service.getById(existing.getId())).thenReturn(existing);
    when(service.authoritiesExistForSourceFile(existing.getId())).thenReturn(true);
    when(mapper.partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class)))
      .thenAnswer(i -> i.getArguments()[1]);
    when(service.update(any(UUID.class), any(AuthoritySourceFile.class), any())).thenAnswer(i -> i.getArguments()[1]);
    var dto = new AuthoritySourceFilePatchDto().baseUrl(INPUT_BASE_URL);

    delegate.patchAuthoritySourceFile(existing.getId(), dto);

    verify(service).update(eq(existing.getId()), sourceFileArgumentCaptor.capture(), eq(null));
    var patchedSourceFile = sourceFileArgumentCaptor.getValue();
    assertThat(expected).usingDefaultComparator().isEqualTo(patchedSourceFile);
    verify(service).authoritiesExistForSourceFile(existing.getId());
    verify(mapper).partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class));
    verify(service).getById(any(UUID.class));
    verify(propagationService).propagate(getMockData(expected, null), UPDATE, TENANT_ID);
    verifyNoMoreInteractions(mapper, service);
  }

  @Test
  void shouldNotProduceEventForSourceFilePartialUpdate() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setBaseUrl(INPUT_BASE_URL);
    existing.setSource(AuthoritySourceFileSource.FOLIO);
    var expected = new AuthoritySourceFile(existing);
    expected.setBaseUrl(SANITIZED_BASE_URL);

    mockAsNonConsortiumTenant();
    when(service.getById(existing.getId())).thenReturn(existing);
    when(service.authoritiesExistForSourceFile(existing.getId())).thenReturn(true);
    when(mapper.partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class)))
      .thenAnswer(i -> i.getArguments()[1]);
    when(service.update(any(UUID.class), any(AuthoritySourceFile.class), eq(null))).thenReturn(expected);
    var dto = new AuthoritySourceFilePatchDto().baseUrl(INPUT_BASE_URL);

    delegate.patchAuthoritySourceFile(existing.getId(), dto);

    verify(service).update(eq(existing.getId()), sourceFileArgumentCaptor.capture(), any());
    var patchedSourceFile = sourceFileArgumentCaptor.getValue();
    assertThat(expected).usingDefaultComparator().isEqualTo(patchedSourceFile);
    verify(service).authoritiesExistForSourceFile(existing.getId());
    verify(mapper).partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class));
    verify(service).getById(any(UUID.class));
    verify(propagationService).propagate(getMockData(expected, null), UPDATE, TENANT_ID);
    verifyNoMoreInteractions(mapper, service);
  }

  @Test
  void shouldProduceEventForSourceFilePartialUpdate() {
    var changeUrlSuffix = "change/suffix";
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setBaseUrl(INPUT_BASE_URL);
    existing.setSource(AuthoritySourceFileSource.FOLIO);
    var expected = new AuthoritySourceFile(existing);
    expected.setBaseUrl(SANITIZED_BASE_URL + changeUrlSuffix);

    mockAsConsortiumCentralTenant();
    when(service.getById(existing.getId())).thenReturn(existing);
    when(service.authoritiesExistForSourceFile(existing.getId())).thenReturn(true);
    when(mapper.partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class)))
      .thenAnswer(i -> i.getArguments()[1]);
    var existingDto = new AuthoritySourceFileDto().id(existing.getId());
    var modifiedDto = new AuthoritySourceFileDto().id(existing.getId());
    AtomicReference<BiConsumer<AuthoritySourceFile, AuthoritySourceFile>> consumer = new AtomicReference<>();
    when(mapper.toDto(any(AuthoritySourceFile.class))).thenReturn(modifiedDto).thenReturn(existingDto);
    when(service.update(any(UUID.class), any(AuthoritySourceFile.class), any())).thenAnswer(invocation -> {
      consumer.set(invocation.getArgument(2));
      consumer.get().accept(expected, existing);
      return expected;
    });
    var dto = new AuthoritySourceFilePatchDto().baseUrl(INPUT_BASE_URL + changeUrlSuffix);

    delegate.patchAuthoritySourceFile(existing.getId(), dto);

    verify(service).update(eq(existing.getId()), sourceFileArgumentCaptor.capture(), any());
    var patchedSourceFile = sourceFileArgumentCaptor.getValue();
    assertThat(expected).usingDefaultComparator().isEqualTo(patchedSourceFile);
    verify(service).authoritiesExistForSourceFile(existing.getId());
    verify(mapper).partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class));
    verify(service).getById(any(UUID.class));
    verify(propagationService).propagate(getMockData(expected, consumer.get()), UPDATE, CENTRAL_TENANT_ID);
    verifyNoMoreInteractions(mapper, service);
  }

  @Test
  void shouldNotProduceEventForSourceFilePartialUpdateWhenNoBaseUrlChange() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setBaseUrl(INPUT_BASE_URL);
    existing.setSource(AuthoritySourceFileSource.FOLIO);
    var expected = new AuthoritySourceFile(existing);
    expected.setBaseUrl(SANITIZED_BASE_URL);

    mockAsNonConsortiumTenant();
    when(service.getById(existing.getId())).thenReturn(existing);
    when(service.authoritiesExistForSourceFile(existing.getId())).thenReturn(true);
    when(mapper.partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class)))
        .thenAnswer(i -> i.getArguments()[1]);
    when(service.update(any(UUID.class), any(AuthoritySourceFile.class), eq(null))).thenReturn(expected);
    var dto = new AuthoritySourceFilePatchDto();

    delegate.patchAuthoritySourceFile(existing.getId(), dto);

    verify(service).update(eq(existing.getId()), sourceFileArgumentCaptor.capture(), any());
    var patchedSourceFile = sourceFileArgumentCaptor.getValue();
    assertThat(expected).usingDefaultComparator().isEqualTo(patchedSourceFile);
    verify(service).authoritiesExistForSourceFile(existing.getId());
    verify(mapper).partialUpdate(any(AuthoritySourceFilePatchDto.class), any(AuthoritySourceFile.class));
    verify(service).getById(any(UUID.class));
    verify(propagationService).propagate(getMockData(expected, null), UPDATE, TENANT_ID);
    verifyNoMoreInteractions(mapper, service);
  }

  @ParameterizedTest
  @MethodSource("patchValidationFailureData")
  void shouldNotPatchAuthoritySourceFile_whenSourceFolioOrAuthoritiesReferenced(AuthoritySourceFileSource source,
                                                                                Boolean authoritiesReferencedAtCentral,
                                                                                Boolean authoritiesReferencedAtMember,
                                                                                List<Parameter> errors) {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setSource(source);
    existing.setSelectable(false);

    var id = existing.getId();
    when(service.getById(id)).thenReturn(existing);
    when(service.authoritiesExistForSourceFile(id)).thenReturn(authoritiesReferencedAtCentral);
    if (!authoritiesReferencedAtCentral) {
      when(context.getTenantId()).thenReturn("central");
      when(consortiumTenantsService.getConsortiumTenants("central")).thenReturn(List.of("member"));
      when(service.authoritiesExistForSourceFile(id, "member", "authority")).thenReturn(authoritiesReferencedAtMember);
    }
    var dto = new AuthoritySourceFilePatchDto()
        .name("name").type("type").selectable(true).version(1).baseUrl("baseUrl").code("a")
        .hridManagement(new AuthoritySourceFilePatchDtoHridManagement().startNumber(1));
    var expected = new RequestBodyValidationException(
        "Unable to patch. Authority source file source is FOLIO or it has authority references", errors);

    var ex = assertThrows(RequestBodyValidationException.class,
      () -> delegate.patchAuthoritySourceFile(id, dto));

    assertThat(ex.getMessage()).isEqualTo(expected.getMessage());
    assertThat(ex.getInvalidParameters()).isEqualTo(expected.getInvalidParameters());
    verifyNoInteractions(mapper, propagationService);
    verifyNoMoreInteractions(service);
  }

  @Test
  void shouldNotPatchAuthoritySourceFile_doNotAddHridToErrors() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setSource(AuthoritySourceFileSource.FOLIO);
    existing.setSelectable(false);
    var dto = new AuthoritySourceFilePatchDto()
      .name("name")
      .hridManagement(new AuthoritySourceFilePatchDtoHridManagement());
    var expected = new RequestBodyValidationException(
      "Unable to patch. Authority source file source is FOLIO or it has authority references",
      List.of(new Parameter("name").value("name")));

    var id = existing.getId();
    when(service.getById(id)).thenReturn(existing);
    when(service.authoritiesExistForSourceFile(id)).thenReturn(false);

    var ex = assertThrows(RequestBodyValidationException.class,
      () -> delegate.patchAuthoritySourceFile(id, dto));

    assertThat(ex.getMessage()).isEqualTo(expected.getMessage());
    assertThat(ex.getInvalidParameters()).isEqualTo(expected.getInvalidParameters());
    verifyNoInteractions(mapper, propagationService);
    verifyNoMoreInteractions(service);
  }

  @Test
  void shouldDeleteAuthoritySourceFileById() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);

    when(service.getById(existing.getId())).thenReturn(existing);
    when(context.getTenantId()).thenReturn(TENANT_ID);
    when(service.authoritiesExistForSourceFile(existing.getId())).thenReturn(false);
    when(consortiumTenantsService.getConsortiumTenants(anyString())).thenReturn(Collections.emptyList());

    delegate.deleteAuthoritySourceFileById(existing.getId());

    verify(service).deleteSequence(existing.getSequenceName());
    verify(service).deleteById(existing.getId());
    verify(propagationService).propagate(getMockData(existing, null), DELETE, TENANT_ID);
  }

  @Test
  void shouldNotDeleteIfReferencedAuthorityExistForSourceFile() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    var id = existing.getId();
    when(service.getById(id)).thenReturn(existing);
    when(context.getTenantId()).thenReturn(TENANT_ID);
    when(service.authoritiesExistForSourceFile(id)).thenReturn(false);
    when(consortiumTenantsService.getConsortiumTenants(anyString())).thenReturn(List.of("college"));
    when(service.authoritiesExistForSourceFile(id, "college", "authority")).thenReturn(true);

    var exc = assertThrows(RequestBodyValidationException.class,
        () -> delegate.deleteAuthoritySourceFileById(id));

    assertThat(exc.getMessage())
        .isEqualTo("Unable to delete. Authority source file has referenced authorities");

    verifyNoMoreInteractions(service);
    verifyNoInteractions(propagationService);
  }

  @Test
  void shouldNotCreateForConsortiumMemberTenant() {
    var dto = new AuthoritySourceFilePostDto();

    mockAsConsortiumMemberTenant();

    var exc = assertThrows(RequestBodyValidationException.class, () -> delegate.createAuthoritySourceFile(dto));

    assertThat(exc.getMessage()).isEqualTo("Action 'CREATE' is not supported for consortium member tenant");
    assertThat(exc.getInvalidParameters()).hasSize(1);
    assertThat(exc.getInvalidParameters().get(0))
      .matches(param -> param.getKey().equals("tenantId") && param.getValue().equals(TENANT_ID));
    verifyNoInteractions(mapper);
    verifyNoInteractions(service);
  }

  @Test
  void shouldGetNextHrid() {
    var id = UUID.randomUUID();
    var code = "CODE10";

    mockAsNonConsortiumTenant();
    when(service.nextHrid(id)).thenReturn(code);
    when(executionService.executeSystemUserScoped(eq(TENANT_ID), any()))
      .thenAnswer(invocation -> ((Callable<?>) invocation.getArgument(1)).call());

    var hridDto = delegate.getAuthoritySourceFileNextHrid(id);

    assertThat(hridDto)
      .extracting(AuthoritySourceFileHridDto::getId, AuthoritySourceFileHridDto::getHrid)
      .containsExactly(id, code);
    verify(service).nextHrid(id);
  }

  @Test
  void shouldGetNextHridForConsortiumMemberTenant_withSwitchToCentralTenant() {
    var id = UUID.randomUUID();
    var code = "CODE10";

    mockAsConsortiumMemberTenant();
    when(service.nextHrid(id)).thenReturn(code);
    when(executionService.executeSystemUserScoped(eq(CENTRAL_TENANT_ID), any()))
      .thenAnswer(invocation -> ((Callable<?>) invocation.getArgument(1)).call());

    var hridDto = delegate.getAuthoritySourceFileNextHrid(id);

    assertThat(hridDto)
      .extracting(AuthoritySourceFileHridDto::getId, AuthoritySourceFileHridDto::getHrid)
      .containsExactly(id, code);
    verify(service).nextHrid(id);
  }

  @Test
  void shouldNotUpdateConsortiumShadowCopy() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setSource(AuthoritySourceFileSource.FOLIO);
    var id = existing.getId();
    var dto = new AuthoritySourceFilePatchDto();

    mockAsConsortiumMemberTenant();
    when(service.getById(existing.getId())).thenReturn(existing);

    var exc = assertThrows(RequestBodyValidationException.class,
      () -> delegate.patchAuthoritySourceFile(id, dto));

    assertThat(exc.getMessage()).isEqualTo("Action 'UPDATE' is not supported for consortium member tenant");
    assertThat(exc.getInvalidParameters()).hasSize(1);
    assertThat(exc.getInvalidParameters().getFirst())
      .matches(param -> param.getKey().equals("tenantId") && param.getValue().equals(TENANT_ID));
    verifyNoInteractions(mapper);
    verifyNoMoreInteractions(service);
    verifyNoInteractions(propagationService);
  }

  @Test
  void shouldNotDeleteConsortiumShadowCopy() {
    var existing = TestDataUtils.AuthorityTestData.authoritySourceFile(0);
    existing.setSource(AuthoritySourceFileSource.FOLIO);
    var id = existing.getId();

    mockAsConsortiumMemberTenant();
    when(service.getById(existing.getId())).thenReturn(existing);

    var exc = assertThrows(RequestBodyValidationException.class,
      () -> delegate.deleteAuthoritySourceFileById(id));

    assertThat(exc.getMessage()).isEqualTo("Action 'DELETE' is not supported for consortium member tenant");
    assertThat(exc.getInvalidParameters()).hasSize(1);
    assertThat(exc.getInvalidParameters().getFirst())
      .matches(param -> param.getKey().equals("tenantId") && param.getValue().equals(TENANT_ID));
    verifyNoMoreInteractions(service);
    verifyNoInteractions(propagationService);
  }

  static Stream<Arguments> patchValidationFailureData() {
    return Stream.of(
      Arguments.of(AuthoritySourceFileSource.FOLIO, false, false, List.of(new Parameter("name").value("name"),
        new Parameter("code").value("a"),
        new Parameter("hridManagement.startNumber").value("1"))),
      Arguments.of(AuthoritySourceFileSource.LOCAL, true, false, List.of(new Parameter("code").value("a"),
        new Parameter("hridManagement.startNumber").value("1"))),
      Arguments.of(AuthoritySourceFileSource.LOCAL, false, true, List.of(new Parameter("code").value("a"),
        new Parameter("hridManagement.startNumber").value("1"))));
  }

  private void mockAsNonConsortiumTenant() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
    when(tenantsService.getCentralTenant(TENANT_ID)).thenReturn(Optional.empty());
  }

  private void mockAsConsortiumCentralTenant() {
    when(context.getTenantId()).thenReturn(CENTRAL_TENANT_ID);
    when(tenantsService.getCentralTenant(CENTRAL_TENANT_ID)).thenReturn(Optional.of(CENTRAL_TENANT_ID));
  }

  private void mockAsConsortiumMemberTenant() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
    when(tenantsService.getCentralTenant(TENANT_ID)).thenReturn(Optional.of(CENTRAL_TENANT_ID));
  }

  private AuthoritySourceFilePropagationData<AuthoritySourceFile> getMockData(
    AuthoritySourceFile asf, BiConsumer<AuthoritySourceFile, AuthoritySourceFile> consumer) {
    return new AuthoritySourceFilePropagationData<>(asf, consumer);
  }
}
