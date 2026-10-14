package com.sozureke.ledger.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    UserService userService;

    @Test
    void createCustomerSavesHashNotRawPassword() {
        when(userRepository.existsByEmail("ann@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createCustomer("ann@example.com", "Ann", "secret");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void createCustomerNormalizesEmailBeforeCheckAndSave() {
        when(userRepository.existsByEmail("ann@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createCustomer("  Ann@Example.COM ", "Ann", "secret");

        assertThat(result.getEmail()).isEqualTo("ann@example.com");
        verify(userRepository).existsByEmail("ann@example.com");
    }

    @Test
    void createCustomerThrowsAndDoesNotSaveWhenEmailTaken() {
        when(userRepository.existsByEmail("ann@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createCustomer("ann@example.com", "Ann", "secret"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode("secret");
    }

    @Test
    void createCustomerTranslatesConstraintViolationFromRaceIntoEmailTaken() {
        when(userRepository.existsByEmail("ann@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> userService.createCustomer("ann@example.com", "Ann", "secret"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndKind(id, UserKind.CUSTOMER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(id)).isInstanceOf(UserNotFoundException.class);
        verify(userRepository).findByIdAndKind(id, UserKind.CUSTOMER);
    }

    @Test
    void getByIdReturnsCustomerWhenFound() {
        UUID id = UUID.randomUUID();
        User user = User.customer("ann@example.com", "Ann", "hash");
        when(userRepository.findByIdAndKind(id, UserKind.CUSTOMER)).thenReturn(Optional.of(user));

        User result = userService.getById(id);

        assertThat(result).isSameAs(user);
    }

    @Test
    void authenticateReturnsUserWhenPasswordMatches() {
        User user = User.customer("ann@example.com", "Ann", "stored-hash");
        when(userRepository.findByEmailAndKind("ann@example.com", UserKind.CUSTOMER)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "stored-hash")).thenReturn(true);

        Optional<User> result = userService.authenticate("Ann@Example.com", "secret");

        assertThat(result).containsSame(user);
    }

    @Test
    void authenticateReturnsEmptyWhenPasswordDoesNotMatch() {
        User user = User.customer("ann@example.com", "Ann", "stored-hash");
        when(userRepository.findByEmailAndKind("ann@example.com", UserKind.CUSTOMER)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThat(userService.authenticate("ann@example.com", "wrong")).isEmpty();
    }

    @Test
    void authenticateStillComparesPasswordWhenEmailIsUnknown() {
        when(userRepository.findByEmailAndKind("ghost@example.com", UserKind.CUSTOMER)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThat(userService.authenticate("ghost@example.com", "secret")).isEmpty();

        verify(passwordEncoder).matches(any(), any());
    }
}
