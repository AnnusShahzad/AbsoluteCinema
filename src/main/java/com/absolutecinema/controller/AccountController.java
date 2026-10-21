package com.absolutecinema.controller;

import com.absolutecinema.entity.User;
import com.absolutecinema.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Profile and password settings, shared by customers, staff and admins. */
@Controller
@RequestMapping("/account")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/settings")
    public String settings(Authentication auth, Model model) {
        User user = userService.findByUsername(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("homeUrl", LoginSuccessHandler.homeFor(user.getAccountType()));
        model.addAttribute("minPasswordLength", UserService.MIN_PASSWORD_LENGTH);
        return "account/settings";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication auth, @RequestParam String firstName, @RequestParam String lastName,
            @RequestParam String email, RedirectAttributes redirect) {
        try {
            userService.updateProfile(userService.findByUsername(auth.getName()), firstName, lastName, email);
            redirect.addFlashAttribute("profileMessage", "Profile updated.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("profileError", e.getMessage());
        }
        return "redirect:/account/settings";
    }

    @PostMapping("/password")
    public String changePassword(Authentication auth, @RequestParam String currentPassword,
            @RequestParam String newPassword, @RequestParam String confirmPassword, RedirectAttributes redirect) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("The new passwords do not match");
            }
            userService.changePassword(userService.findByUsername(auth.getName()), currentPassword, newPassword);
            redirect.addFlashAttribute("passwordMessage", "Password changed.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("passwordError", e.getMessage());
        }
        return "redirect:/account/settings#password";
    }
}
