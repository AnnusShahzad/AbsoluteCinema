package com.absolutecinema.service;

import com.absolutecinema.entity.Booking;
import com.absolutecinema.entity.Movie;
import com.absolutecinema.entity.BookingSnack;
import com.absolutecinema.entity.Snack;
import com.absolutecinema.entity.User;
import com.absolutecinema.repository.BookingRepository;
import com.absolutecinema.repository.BookingSnackRepository;
import com.absolutecinema.repository.SnackRepository;
import com.absolutecinema.repository.MovieRepository;
import com.absolutecinema.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final double TICKET_PRICE = 12.0; // Base ticket price
    private static final double SNACK_PRICE = 5.0; // Base snack price

    private final BookingRepository bookingRepository;
    private final BookingSnackRepository bookingSnackRepository;
    private final SnackRepository snackRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public PaymentService(BookingRepository bookingRepository,
            BookingSnackRepository bookingSnackRepository,
            SnackRepository snackRepository,
            UserRepository userRepository,
            MovieRepository movieRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingSnackRepository = bookingSnackRepository;
        this.snackRepository = snackRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    public boolean canRedeemPoints(User user) {
        return user.getPoints() >= LoyaltyService.REDEMPTION_COST;
    }

    public double calculateDiscount(User user, boolean applyRedemption) {
        if (applyRedemption && canRedeemPoints(user)) {
            return TICKET_PRICE + SNACK_PRICE; // Free movie ticket + snack
        }
        return 0.0;
    }

    @Transactional
    public void processPayment(String paymentMethodId, double amount, List<Object> seats,
            Map<String, Object> snacks, User user, Long movieId, Long sessionId, boolean applyRedemption)
            throws RuntimeException {
        try {
            // Set Stripe API key here to ensure it's always set
            Stripe.apiKey = stripeSecretKey;

            logger.info("Processing payment for user: {}, amount: {}, movieId: {}, sessionId: {}",
                    user.getUsername(), amount, movieId, sessionId);

            // Validate input
            if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
                throw new IllegalArgumentException("Payment method ID is required");
            }
            if (amount < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }
            if (movieId == null || sessionId == null) {
                throw new IllegalArgumentException("Movie ID and Session ID are required");
            }

            // Apply loyalty points discount if requested
            double finalAmount = amount;
            if (applyRedemption) {
                if (!canRedeemPoints(user)) {
                    throw new IllegalArgumentException("Insufficient loyalty points for redemption");
                }
                double discount = calculateDiscount(user, true);
                finalAmount = Math.max(0, amount - discount);

                // Deduct points from user
                user.setPoints(user.getPoints() - LoyaltyService.REDEMPTION_COST);
                userRepository.save(user);
                logger.info("Applied loyalty points redemption for user: {}, discount: {}", user.getUsername(),
                        discount);
            }

            // Create payment intent with final amount
            Map<String, Object> params = new HashMap<>();
            params.put("amount", (int) (finalAmount * 100)); // Convert to cents
            params.put("currency", "pkr");
            params.put("payment_method", paymentMethodId);
            params.put("confirm", true);
            params.put("return_url", "http://localhost:8080/user/payment/success");

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            if ("succeeded".equals(paymentIntent.getStatus())) {
                int ticketCount = seats != null ? seats.size() : 0;
                Booking booking = new Booking();
                booking.setUser(user);
                booking.setMovieId(movieId);
                booking.setSessionId(sessionId);
                booking.setNumberOfTickets(ticketCount);
                booking.setTotalPrice(finalAmount);
                booking.setBookingTime(java.time.LocalDateTime.now());
                booking = bookingRepository.save(booking);

                Movie movie = movieRepository.findById(movieId)
                        .orElseThrow(() -> new IllegalArgumentException("Movie not found with ID: " + movieId));
                movie.setTicketssold(movie.getTicketssold() + ticketCount);
                movieRepository.save(movie);

                // Process snacks if any
                if (snacks != null && !snacks.isEmpty()) {
                    for (Map.Entry<String, Object> entry : snacks.entrySet()) {
                        Object snackObj = entry.getValue();
                        if (snackObj instanceof Map) {
                            Map<String, Object> snackMap = (Map<String, Object>) snackObj;
                            Object idObj = snackMap.get("id");
                            Object quantityObj = snackMap.get("quantity");

                            if (idObj == null || quantityObj == null) {
                                logger.warn("Skipping snack with missing id or quantity: {}", snackMap);
                                continue;
                            }

                            try {
                                Long snackId = Long.parseLong(idObj.toString());
                                Integer quantity = Integer.parseInt(quantityObj.toString());

                                if (quantity <= 0) {
                                    logger.warn("Skipping snack with invalid quantity: {}", quantity);
                                    continue;
                                }

                                Snack snack = snackRepository.findById(snackId)
                                        .orElseThrow(() -> new RuntimeException("Snack not found with ID: " + snackId));

                                // Update snack quantity
                                snack.setQuantity(snack.getQuantity() - quantity);
                                snack.setSnackssold(snack.getSnackssold() + quantity);
                                snackRepository.save(snack);

                                // Create booking snack
                                BookingSnack bookingSnack = new BookingSnack();
                                bookingSnack.setBooking(booking);
                                bookingSnack.setSnack(snack);
                                bookingSnack.setQuantity(quantity);
                                bookingSnackRepository.save(bookingSnack);

                                logger.info("Processed snack: {} x {}", snack.getName(), quantity);
                            } catch (NumberFormatException e) {
                                logger.error("Error parsing snack ID or quantity: {}", e.getMessage());
                                throw new RuntimeException("Invalid snack ID or quantity format");
                            }
                        } else {
                            logger.warn("Skipping invalid snack object: {}", snackObj);
                        }
                    }
                }

                // Add loyalty points for the purchase (50 per ticket, 20 per snack)
                user.setPoints(user.getPoints()
                        + LoyaltyService.pointsEarned(ticketCount, snacks != null ? snacks.size() : 0));
                userRepository.save(user);

                logger.info("Payment processed successfully for booking ID: {}", booking.getId());
            } else {
                String errorMessage = paymentIntent.getLastPaymentError() != null
                        ? paymentIntent.getLastPaymentError().getMessage()
                        : "Unknown payment error";
                logger.error("Payment failed: {}", errorMessage);
                throw new RuntimeException("Payment failed: " + errorMessage);
            }
        } catch (StripeException e) {
            logger.error("Stripe payment processing failed: {}", e.getMessage());
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during payment processing: {}", e.getMessage());
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }
}