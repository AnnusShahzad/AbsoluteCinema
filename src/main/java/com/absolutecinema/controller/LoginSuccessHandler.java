package com.absolutecinema.controller;

import com.absolutecinema.entity.User;
import com.absolutecinema.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Sends each account type to its own home page after signing in. */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public LoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static String homeFor(String accountType) {
        if ("admin".equalsIgnoreCase(accountType)) {
            return "/admin/dashboard";
        }
        if ("employee".equalsIgnoreCase(accountType)) {
            return "/employee/dashboard";
        }
        return "/user/dashboard";
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        User user = userRepository.findByUsername(authentication.getName());
        response.sendRedirect(request.getContextPath() + homeFor(user == null ? null : user.getAccountType()));
    }
}
