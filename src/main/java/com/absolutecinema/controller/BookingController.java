package com.absolutecinema.controller;

import com.absolutecinema.entity.User;
import com.absolutecinema.service.BookingService;
import com.absolutecinema.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @GetMapping("/history")
    public String getBookingHistory(Authentication auth, Model model) {
        User user = userService.findByUsername(auth.getName());
        model.addAttribute("bookings", bookingService.getUserBookings(user));
        return "bookings/history";
    }

    /** Admin only (see SecurityConfig). */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }
}
