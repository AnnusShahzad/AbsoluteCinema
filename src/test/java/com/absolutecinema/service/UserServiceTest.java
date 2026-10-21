package com.absolutecinema.service;

import com.absolutecinema.entity.User;
import com.absolutecinema.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserService userService = new UserService(userRepository, encoder);

    @Test
    void registerCreatesACustomerWithAHashedPassword() {
        when(userRepository.findMaxId()).thenReturn(Optional.of(41L));
        when(userRepository.findByEmailIgnoreCase("dana@example.com")).thenReturn(List.of());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register("dana_d", " Dana@Example.com ", "secret123", "Dana", "Demo");

        assertThat(user.getId()).isEqualTo(42L);
        assertThat(user.getAccountType()).isEqualTo("user");
        assertThat(user.getEmail()).isEqualTo("dana@example.com");
        assertThat(encoder.matches("secret123", user.getPassword())).isTrue();
    }

    @Test
    void registerRejectsInvalidInput() {
        assertThatThrownBy(() -> userService.register("a", "a@b.co", "secret123", "A", "B"))
                .hasMessageContaining("Username");
        assertThatThrownBy(() -> userService.register("valid_name", "not-an-email", "secret123", "A", "B"))
                .hasMessageContaining("email");
        assertThatThrownBy(() -> userService.register("valid_name", "a@b.co", "short", "A", "B"))
                .hasMessageContaining("at least");
        assertThatThrownBy(() -> userService.register("valid_name", "a@b.co", "secret123", " ", "B"))
                .hasMessageContaining("First name");
    }

    @Test
    void registerRejectsTakenUsernameAndEmail() {
        when(userRepository.findByUsername("taken")).thenReturn(new User());
        assertThatThrownBy(() -> userService.register("taken", "a@b.co", "secret123", "A", "B"))
                .hasMessageContaining("taken");

        User other = new User();
        other.setId(7L);
        when(userRepository.findMaxId()).thenReturn(Optional.of(1L));
        when(userRepository.findByEmailIgnoreCase("used@b.co")).thenReturn(List.of(other));
        assertThatThrownBy(() -> userService.register("fresh", "used@b.co", "secret123", "A", "B"))
                .hasMessageContaining("already in use");
    }

    @Test
    void emailValidationIsStrictAndFastOnHostileInput() {
        assertThat(UserService.isValidEmail("dana@mail.example.com")).isTrue();
        assertThat(UserService.isValidEmail("dana@localhost")).isFalse();
        assertThat(UserService.isValidEmail("dana@example..com")).isFalse();
        assertThat(UserService.isValidEmail("a@" + "b".repeat(260) + ".com")).isFalse();

        String hostile = "!@!." + "!.".repeat(5000);
        long start = System.nanoTime();
        assertThat(UserService.isValidEmail(hostile)).isFalse();
        assertThat(System.nanoTime() - start).isLessThan(1_000_000_000L);
    }

    @Test
    void changePasswordRequiresTheCurrentPassword() {
        User user = new User();
        user.setPassword(encoder.encode("old-password"));

        assertThatThrownBy(() -> userService.changePassword(user, "wrong", "new-password"))
                .hasMessageContaining("current password");

        userService.changePassword(user, "old-password", "new-password");
        assertThat(encoder.matches("new-password", user.getPassword())).isTrue();
    }
}
