package com.absolutecinema.controller;

import com.absolutecinema.service.MovieService;
import com.absolutecinema.service.PaymentService;
import com.absolutecinema.service.SessionService;
import com.absolutecinema.service.SnackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserController {

    private final MovieService movieService;
    private final SessionService sessionService;
    private final SnackService snackService;

    @Autowired
    public UserController(MovieService movieService, SessionService sessionService, SnackService snackService) {
        this.movieService = movieService;
        this.sessionService = sessionService;
        this.snackService = snackService;
    }

    @GetMapping("/user/seats")
    public String getSeatsPage(@RequestParam Long movieId, Model model) {
        var movie = movieService.findById(movieId);
        if (movie == null) {
            return "redirect:/movies";
        }

        // Get available sessions for the movie
        var sessions = sessionService.getUpcomingSessions(movieId);

        model.addAttribute("movie", movie);
        model.addAttribute("sessions", sessions);
        model.addAttribute("regularPrice", PaymentService.REGULAR_SEAT_PRICE);
        model.addAttribute("premiumPrice", PaymentService.PREMIUM_SEAT_PRICE);
        return "user/seats";
    }

    @GetMapping("/user/snacks")
    public String getSnacksPage(Model model) {
        // Get all available snacks
        var snacks = snackService.getAllSnacks();
        model.addAttribute("snacks", snacks);
        return "user/snackSelection";
    }
}
