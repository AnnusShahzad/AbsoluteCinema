package com.absolutecinema.controller;

import com.absolutecinema.entity.User;
import com.absolutecinema.service.PasswordResetService;
import com.absolutecinema.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public AuthController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/")
    public String home(Authentication auth) {
        return isSignedIn(auth) ? "redirect:" + homeFor(auth) : "redirect:/auth/login";
    }

    @GetMapping("/auth/login")
    public String login(Authentication auth, Model model,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String registered,
            @RequestParam(required = false) String reset) {
        if (isSignedIn(auth)) {
            return "redirect:" + homeFor(auth);
        }
        if (error != null) {
            model.addAttribute("errorMessage", "Incorrect username or password.");
        } else if (registered != null) {
            model.addAttribute("infoMessage", "Account created. Please sign in.");
        } else if (reset != null) {
            model.addAttribute("infoMessage", "Password updated. Please sign in with your new password.");
        } else if (logout != null) {
            model.addAttribute("infoMessage", "You have been signed out.");
        }
        return "auth/signin";
    }

    @GetMapping("/auth/register")
    public String registerForm() {
        return "auth/signup";
    }

    @PostMapping("/auth/register")
    public String register(@RequestParam String username, @RequestParam String password,
            @RequestParam String email, @RequestParam String firstName, @RequestParam String lastName,
            Model model) {
        try {
            userService.register(username, email, password, firstName, lastName);
            return "redirect:/auth/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("form", Map.of("username", username, "email", email,
                    "firstName", firstName, "lastName", lastName));
            return "auth/signup";
        }
    }

    @GetMapping("/auth/forgot-password")
    public String forgotPassword() {
        return "auth/forgot-password";
    }

    @PostMapping("/auth/forgot-password/code")
    @ResponseBody
    public Map<String, Object> sendResetCode(@RequestBody Map<String, String> body) {
        passwordResetService.requestCode(body.get("email"));
        return Map.of("success", true);
    }

    @PostMapping("/auth/forgot-password/verify")
    @ResponseBody
    public Map<String, Object> verifyResetCode(@RequestBody Map<String, String> body) {
        passwordResetService.verifyCode(body.get("email"), body.get("code"));
        return Map.of("success", true);
    }

    @PostMapping("/auth/forgot-password/reset")
    @ResponseBody
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> body) {
        passwordResetService.resetPassword(body.get("email"), body.get("code"), body.get("password"));
        return Map.of("success", true);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejected(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
    }

    private static boolean isSignedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private String homeFor(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        return LoginSuccessHandler.homeFor(user == null ? null : user.getAccountType());
    }
}
