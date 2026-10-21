package com.absolutecinema.controller;

import com.absolutecinema.entity.Booking;
import com.absolutecinema.entity.User;
import com.absolutecinema.service.BookingService;
import com.absolutecinema.service.LoyaltyService;
import com.absolutecinema.service.PaymentService;
import com.absolutecinema.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Controller
@RequestMapping("/user/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final UserService userService;
    private final String stripePublicKey;

    public PaymentController(PaymentService paymentService, BookingService bookingService, UserService userService,
            @Value("${stripe.public.key:}") String stripePublicKey) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.userService = userService;
        this.stripePublicKey = stripePublicKey;
    }

    @GetMapping
    public String paymentPage(Authentication auth, Model model) {
        model.addAttribute("user", userService.findByUsername(auth.getName()));
        model.addAttribute("stripePublicKey", stripePublicKey);
        model.addAttribute("demoMode", paymentService.isDemoMode());
        model.addAttribute("redemptionCost", LoyaltyService.REDEMPTION_COST);
        return "user/payment";
    }

    @PostMapping("/quote")
    @ResponseBody
    public PaymentService.Quote quote(Authentication auth, @RequestBody PaymentService.OrderRequest order) {
        return paymentService.quote(userService.findByUsername(auth.getName()), order);
    }

    @PostMapping("/process")
    @ResponseBody
    public Map<String, Object> process(Authentication auth, @RequestBody PaymentService.OrderRequest order) {
        Booking booking = paymentService.checkout(userService.findByUsername(auth.getName()), order);
        return Map.of("success", true, "redirectUrl", "/user/payment/success?booking=" + booking.getId());
    }

    @GetMapping("/success")
    public String success(Authentication auth, @RequestParam("booking") Long bookingId, Model model) {
        User user = userService.findByUsername(auth.getName());
        Booking booking = bookingService.getUserBookings(user).stream()
                .filter(b -> b.getId().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("booking", booking);
        model.addAttribute("points", user.getPoints());
        return "user/payment-success";
    }

    /** Order problems (bad seat, taken seat, card declined) are shown to the customer as-is. */
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    @ResponseBody
    public ResponseEntity<Map<String, Object>> orderRejected(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
    }
}
