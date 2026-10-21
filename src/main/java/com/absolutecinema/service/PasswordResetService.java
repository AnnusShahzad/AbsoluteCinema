package com.absolutecinema.service;

import com.absolutecinema.entity.PasswordResetToken;
import com.absolutecinema.entity.User;
import com.absolutecinema.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Self-service password reset with short-lived 6-digit codes. Responses never reveal whether an
 * email address has an account.
 */
@Service
public class PasswordResetService {

    static final Duration CODE_LIFETIME = Duration.ofMinutes(15);
    static final int MAX_ATTEMPTS = 5;
    private static final String INVALID_CODE = "That code is invalid or has expired. Request a new one.";

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(PasswordResetTokenRepository tokenRepository, UserService userService,
            PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /** Issues a new code if the email belongs to an account, replacing any earlier code. */
    @Transactional
    public void requestCode(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            logger.info("Password reset requested for an unknown email address");
            return;
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        tokenRepository.deleteByUser(user);
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCodeHash(passwordEncoder.encode(code));
        token.setExpiresAt(LocalDateTime.now().plus(CODE_LIFETIME));
        tokenRepository.save(token);
        deliver(user, code);
    }

    /** Checks a code without using it up; wrong guesses count toward the attempt limit. */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verifyCode(String email, String code) {
        validToken(email, code);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void resetPassword(String email, String code, String newPassword) {
        PasswordResetToken token = validToken(email, code);
        userService.setPassword(token.getUser(), newPassword);
        tokenRepository.delete(token);
    }

    private PasswordResetToken validToken(String email, String code) {
        User user = userService.findByEmail(email);
        PasswordResetToken token = user == null ? null : tokenRepository.findFirstByUserOrderByIdDesc(user).orElse(null);
        if (token == null || token.getExpiresAt().isBefore(LocalDateTime.now())
                || token.getFailedAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException(INVALID_CODE);
        }
        if (code == null || !passwordEncoder.matches(code, token.getCodeHash())) {
            token.setFailedAttempts(token.getFailedAttempts() + 1);
            tokenRepository.save(token);
            throw new IllegalArgumentException(INVALID_CODE);
        }
        return token;
    }

    /**
     * There is no mail server in this project, so the code is written to the application log.
     * Swap this for an email or SMS sender when deploying.
     */
    private void deliver(User user, String code) {
        logger.info("Password reset code for {}: {} (valid for {} minutes)", user.getEmail(), code,
                CODE_LIFETIME.toMinutes());
    }
}
