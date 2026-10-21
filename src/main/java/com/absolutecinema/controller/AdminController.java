package com.absolutecinema.controller;

import com.absolutecinema.entity.User;
import com.absolutecinema.service.AnalyticsService;
import com.absolutecinema.service.BookingService;
import com.absolutecinema.service.LoyaltyService;
import com.absolutecinema.service.MovieService;
import com.absolutecinema.service.SnackService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int REPORT_MONTHS = 12;

    private final MovieService movieService;
    private final SnackService snackService;
    private final BookingService bookingService;
    private final LoyaltyService loyaltyService;
    private final AnalyticsService analyticsService;

    public AdminController(MovieService movieService, SnackService snackService, BookingService bookingService,
            LoyaltyService loyaltyService, AnalyticsService analyticsService) {
        this.movieService = movieService;
        this.snackService = snackService;
        this.bookingService = bookingService;
        this.loyaltyService = loyaltyService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAllAttributes(analyticsService.getDashboardData());
        return "admin/dashboard";
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("report", analyticsService.getReport(REPORT_MONTHS));
        return "admin/analytics";
    }

    @GetMapping("/managebooking")
    public String manageBookings(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "admin/managebooking";
    }

    @GetMapping("/manageproduct")
    public String manageProducts(Model model) {
        model.addAttribute("movies", movieService.getAllMovies());
        model.addAttribute("snacks", snackService.findAll());
        return "admin/manageproduct";
    }

    @GetMapping("/manageloyalty")
    public String manageLoyalty(Model model) {
        List<User> members = loyaltyService.getMembers();
        model.addAttribute("members", members);
        model.addAttribute("tiers", members.stream()
                .collect(Collectors.toMap(User::getId, member -> LoyaltyService.tierFor(member.getPoints()))));
        model.addAttribute("summary", loyaltyService.summarize(members));
        model.addAttribute("silverThreshold", LoyaltyService.SILVER_THRESHOLD);
        model.addAttribute("goldThreshold", LoyaltyService.GOLD_THRESHOLD);
        model.addAttribute("redemptionCost", LoyaltyService.REDEMPTION_COST);
        model.addAttribute("pointsPerTicket", LoyaltyService.POINTS_PER_TICKET);
        model.addAttribute("pointsPerSnack", LoyaltyService.POINTS_PER_SNACK);
        return "admin/manageloyalty";
    }

    @GetMapping("/managefeedback")
    public String manageFeedback() {
        return "admin/managefeedback";
    }

    @GetMapping("/settings")
    public String settings() {
        return "admin/settings";
    }
}
