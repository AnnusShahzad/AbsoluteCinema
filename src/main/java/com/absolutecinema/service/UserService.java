package com.absolutecinema.service;

import com.absolutecinema.entity.User;
import com.absolutecinema.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/** Account rules: who can register, and how profiles and passwords change. */
@Service
public class UserService {

    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_.]{3,30}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final int MAX_NAME_LENGTH = 50;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User findByEmail(String email) {
        return email == null ? null
                : userRepository.findByEmailIgnoreCase(email.trim()).stream().findFirst().orElse(null);
    }

    public boolean checkUsernameExists(String username) {
        return userRepository.findByUsername(username) != null;
    }

    /** Creates a customer account. Throws IllegalArgumentException with a user-facing message. */
    @Transactional
    public User register(String username, String email, String password, String firstName, String lastName) {
        username = username == null ? "" : username.trim();
        if (!USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-30 characters: letters, numbers, dots or underscores");
        }
        if (checkUsernameExists(username)) {
            throw new IllegalArgumentException("That username is already taken");
        }
        User user = new User();
        // Ids are assigned by the application because the users table has no identity column
        user.setId(userRepository.findMaxId().orElse(0L) + 1);
        user.setUsername(username);
        user.setAccountType("user");
        applyProfile(user, firstName, lastName, email);
        user.setPassword(passwordEncoder.encode(requireValidPassword(password)));
        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public User updateProfile(User user, String firstName, String lastName, String email) {
        applyProfile(user, firstName, lastName, email);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Your current password is incorrect");
        }
        setPassword(user, newPassword);
    }

    @Transactional
    public void setPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(requireValidPassword(newPassword)));
        userRepository.save(user);
    }

    private void applyProfile(User user, String firstName, String lastName, String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (!EMAIL.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }
        boolean takenByOther = userRepository.findByEmailIgnoreCase(normalizedEmail).stream()
                .anyMatch(owner -> !owner.getId().equals(user.getId()));
        if (takenByOther) {
            throw new IllegalArgumentException("That email address is already in use");
        }
        user.setFirstName(requireName(firstName, "First name"));
        user.setLastName(requireName(lastName, "Last name"));
        user.setEmail(normalizedEmail);
    }

    private static String requireName(String value, String field) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(field + " is required (up to " + MAX_NAME_LENGTH + " characters)");
        }
        return name;
    }

    private static String requireValidPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        return password;
    }
}
