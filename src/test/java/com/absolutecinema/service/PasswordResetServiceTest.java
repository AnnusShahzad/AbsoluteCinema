package com.absolutecinema.service;

import com.absolutecinema.entity.PasswordResetToken;
import com.absolutecinema.entity.User;
import com.absolutecinema.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private final PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
    private final UserService userService = mock(UserService.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final PasswordResetService service = new PasswordResetService(tokenRepository, userService, encoder);

    private final User user = new User();
    private final PasswordResetToken token = new PasswordResetToken();

    @BeforeEach
    void setUp() {
        user.setEmail("demo@example.com");
        when(userService.findByEmail("demo@example.com")).thenReturn(user);
        token.setUser(user);
        token.setCodeHash(encoder.encode("123456"));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(tokenRepository.findFirstByUserOrderByIdDesc(user)).thenReturn(Optional.of(token));
    }

    @Test
    void requestCodeStoresOnlyAHashOfASixDigitCode() {
        service.requestCode("demo@example.com");

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).deleteByUser(user);
        verify(tokenRepository).save(saved.capture());
        assertThat(saved.getValue().getCodeHash()).startsWith("$2");
        assertThat(saved.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(14));
    }

    @Test
    void requestCodeForUnknownEmailDoesNothingVisible() {
        service.requestCode("nobody@example.com");

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void correctCodeResetsThePasswordAndConsumesTheToken() {
        service.resetPassword("demo@example.com", "123456", "new-password");

        verify(userService).setPassword(user, "new-password");
        verify(tokenRepository).delete(token);
    }

    @Test
    void wrongCodesCountTowardTheAttemptLimit() {
        for (int i = 0; i < PasswordResetService.MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> service.verifyCode("demo@example.com", "000000"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(token.getFailedAttempts()).isEqualTo(PasswordResetService.MAX_ATTEMPTS);
        // Even the right code is refused once the limit is reached
        assertThatThrownBy(() -> service.verifyCode("demo@example.com", "123456"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiredCodesAreRefused() {
        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        assertThatThrownBy(() -> service.resetPassword("demo@example.com", "123456", "new-password"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userService, never()).setPassword(any(), any());
    }
}
