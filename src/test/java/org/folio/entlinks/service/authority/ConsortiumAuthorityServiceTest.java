package org.folio.entlinks.service.authority;

import static org.folio.support.base.TestConstants.CONSORTIUM_SOURCE_PREFIX;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.repository.AuthorityRepository;
import org.folio.entlinks.exception.ConsortiumIllegalActionException;
import org.folio.spring.testing.extension.Random;
import org.folio.spring.testing.extension.impl.RandomParametersExtension;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith({MockitoExtension.class, RandomParametersExtension.class})
class ConsortiumAuthorityServiceTest {

  private @Mock AuthorityRepository repository;
  private @InjectMocks ConsortiumAuthorityService consortiumAuthorityService;

  private @Mock BiConsumer<Authority, Authority> authorityConsumer;
  private @Mock Consumer<Authority> authorityCallback;

  @Test
  void updateInner_Forced_UpdateInnerCalled(@Random Authority authority) {
    // Arrange
    boolean forced = true;
    when(repository.findByIdAndDeletedFalse(any())).thenReturn(Optional.of(authority));

    // Act
    consortiumAuthorityService.updateInner(authority, forced, authorityConsumer);

    // Assert
    verify(authorityConsumer).accept(any(), any());
    verify(repository).findByIdAndDeletedFalse(any());
  }

  @Test
  void updateInner_NotForced_ValidateCalled(@Random Authority authority) {
    // Arrange
    boolean forced = false;
    authority.setSource(CONSORTIUM_SOURCE_PREFIX);
    when(repository.findByIdAndDeletedFalse(any())).thenReturn(Optional.of(authority));

    // Act
    assertThrows(ConsortiumIllegalActionException.class,
      () -> consortiumAuthorityService.updateInner(authority, forced, authorityConsumer));

    // Assert
    verifyNoInteractions(authorityConsumer); // Should not call updateInner
    verify(repository).findByIdAndDeletedFalse(any(UUID.class));
  }

  @Test
  void deleteByIdInner_Forced_DeleteByIdInnerCalled(@Random UUID id, @Random Authority authority) {
    // Arrange
    boolean forced = true;
    when(repository.findByIdAndDeletedFalse(any())).thenReturn(Optional.of(authority));

    // Act
    consortiumAuthorityService.deleteByIdInner(id, forced, authorityCallback);

    // Assert
    verify(authorityCallback).accept(any());
    verify(repository).findByIdAndDeletedFalse(any());
  }

  @Test
  void deleteByIdInner_NotForced_ValidateCalled(@Random UUID id, @Random Authority authority) {
    // Arrange
    boolean forced = false;
    authority.setSource(CONSORTIUM_SOURCE_PREFIX);
    when(repository.findByIdAndDeletedFalse(any())).thenReturn(Optional.of(authority));

    // Act
    assertThrows(ConsortiumIllegalActionException.class,
      () -> consortiumAuthorityService.deleteByIdInner(id, forced, authorityCallback));

    // Assert
    verifyNoInteractions(authorityCallback);
    verify(repository).findByIdAndDeletedFalse(any(UUID.class));
  }

}
