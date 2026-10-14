package com.sozureke.ledger.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createCustomer("Ann", "secret");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
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
        User user = User.customer("Ann", "hash");
        when(userRepository.findByIdAndKind(id, UserKind.CUSTOMER)).thenReturn(Optional.of(user));

        User result = userService.getById(id);

        assertThat(result).isSameAs(user);
    }

}
