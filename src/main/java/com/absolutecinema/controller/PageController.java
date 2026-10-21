package com.absolutecinema.controller;

import com.absolutecinema.entity.User;
import com.absolutecinema.service.ContactService;
import com.absolutecinema.service.LoyaltyService;
import com.absolutecinema.service.MovieService;
import com.absolutecinema.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/** Customer-facing pages that are not part of the booking flow. */
@Controller
public class PageController {

    private final UserService userService;
    private final MovieService movieService;
    private final ContactService contactService;

    public PageController(UserService userService, MovieService movieService, ContactService contactService) {
        this.userService = userService;
        this.movieService = movieService;
        this.contactService = contactService;
    }

    @GetMapping("/user/dashboard")
    public String getUserDashboard(Model model) {
        model.addAttribute("movies", movieService.getMoviesByPopularity());
        addLoyaltyRules(model);
        return "user/dashboard";
    }

    @GetMapping("/feedback")
    public String getFeedbackPage(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByUsername(auth.getName()));
        model.addAttribute("movies", movieService.getAllMovies());
        return "feedback/feedback";
    }

    @GetMapping("/contact")
    public String getContactPage() {
        return "contact/contact";
    }

    @PostMapping("/contact")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitContactMessage(@RequestBody Map<String, String> body) {
        try {
            contactService.submit(body.get("name"), body.get("email"), body.get("phone"), body.get("subject"),
                    body.get("message"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/loyalty")
    public String getLoyaltyPage(Authentication auth, Model model) {
        User user = userService.findByUsername(auth.getName());
        model.addAttribute("user", user);
        model.addAttribute("points", user.getPoints());
        model.addAttribute("loyaltyLevel", LoyaltyService.tierFor(user.getPoints()));
        addLoyaltyRules(model);
        return "user/loyaltyuser";
    }

    private static void addLoyaltyRules(Model model) {
        model.addAttribute("pointsPerTicket", LoyaltyService.POINTS_PER_TICKET);
        model.addAttribute("pointsPerSnack", LoyaltyService.POINTS_PER_SNACK);
        model.addAttribute("redemptionCost", LoyaltyService.REDEMPTION_COST);
    }
}
