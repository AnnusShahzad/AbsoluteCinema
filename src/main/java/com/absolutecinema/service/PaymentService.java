package com.absolutecinema.service;

import com.absolutecinema.entity.Booking;
import com.absolutecinema.entity.BookingSnack;
import com.absolutecinema.entity.Movie;
import com.absolutecinema.entity.Session;
import com.absolutecinema.entity.Snack;
import com.absolutecinema.entity.User;
import com.absolutecinema.repository.BookingRepository;
import com.absolutecinema.repository.BookingSnackRepository;
import com.absolutecinema.repository.MovieRepository;
import com.absolutecinema.repository.SessionRepository;
import com.absolutecinema.repository.SnackRepository;
import com.absolutecinema.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Prices orders and turns paid orders into bookings. Prices are always computed on the server;
 * the browser only says which seats and snacks were chosen.
 */
@Service
public class PaymentService {

    public static final double REGULAR_SEAT_PRICE = 1200;
    public static final double PREMIUM_SEAT_PRICE = 1500;
    private static final String CURRENCY = "pkr";

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final BookingRepository bookingRepository;
    private final BookingSnackRepository bookingSnackRepository;
    private final SnackRepository snackRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final SessionRepository sessionRepository;
    private final SessionService sessionService;
    private final String stripeSecretKey;

    public PaymentService(BookingRepository bookingRepository,
            BookingSnackRepository bookingSnackRepository,
            SnackRepository snackRepository,
            UserRepository userRepository,
            MovieRepository movieRepository,
            SessionRepository sessionRepository,
            SessionService sessionService,
            @Value("${stripe.secret.key:}") String stripeSecretKey) {
        this.bookingRepository = bookingRepository;
        this.bookingSnackRepository = bookingSnackRepository;
        this.snackRepository = snackRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.sessionRepository = sessionRepository;
        this.sessionService = sessionService;
        this.stripeSecretKey = stripeSecretKey;
    }

    /** Without a Stripe key, orders are booked without charging a card so the app can be demoed. */
    public boolean isDemoMode() {
        return stripeSecretKey == null || stripeSecretKey.isBlank();
    }

    public static double seatPrice(String seat) {
        return SeatLayout.isPremium(seat) ? PREMIUM_SEAT_PRICE : REGULAR_SEAT_PRICE;
    }

    @Transactional(readOnly = true)
    public Quote quote(User user, OrderRequest order) {
        List<String> seats = SeatLayout.validate(order.seats());
        return price(user, seats, loadSnacks(order.snacks()), order.applyRedemption());
    }

    /**
     * Charges the order and records the booking. The showtime row is locked for the duration of the
     * transaction so two customers cannot buy the same seat.
     */
    @Transactional
    public Booking checkout(User user, OrderRequest order) {
        List<String> seats = SeatLayout.validate(order.seats());
        Session session = sessionRepository.findByIdForUpdate(order.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("Showtime not found"));
        Movie movie = session.getMovie();
        if (movie == null || !movie.getId().equals(order.movieId())) {
            throw new IllegalArgumentException("Showtime does not belong to this movie");
        }
        if (!session.getSessionTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("This showtime has already started");
        }
        Set<String> occupied = sessionService.getOccupiedSeats(session.getId());
        List<String> taken = seats.stream().filter(occupied::contains).toList();
        if (!taken.isEmpty()) {
            throw new IllegalStateException("Sorry, these seats were just booked: " + String.join(", ", taken));
        }

        Map<Snack, Integer> snacks = loadSnacks(order.snacks());
        snacks.forEach((snack, quantity) -> {
            if (snack.getQuantity() < quantity) {
                throw new IllegalStateException("Only " + snack.getQuantity() + " " + snack.getName() + " left");
            }
        });
        Quote quote = price(user, seats, snacks, order.applyRedemption());

        charge(quote.total(), order.paymentMethodId(), user);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setMovieId(movie.getId());
        booking.setSessionId(session.getId());
        booking.setSeats(SeatLayout.format(seats));
        booking.setNumberOfTickets(seats.size());
        booking.setTotalPrice(quote.total());
        booking.setBookingTime(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        int snackItems = 0;
        for (Map.Entry<Snack, Integer> entry : snacks.entrySet()) {
            Snack snack = entry.getKey();
            int quantity = entry.getValue();
            snack.setQuantity(snack.getQuantity() - quantity);
            snack.setSnackssold(snack.getSnackssold() + quantity);
            snackRepository.save(snack);

            BookingSnack line = new BookingSnack();
            line.setBooking(booking);
            line.setSnack(snack);
            line.setQuantity(quantity);
            bookingSnackRepository.save(line);
            snackItems += quantity;
        }

        movie.setTicketssold(movie.getTicketssold() + seats.size());
        movieRepository.save(movie);
        session.setAvailableSeats(Math.max(0, session.getAvailableSeats() - seats.size()));
        sessionRepository.save(session);

        int points = user.getPoints() - (quote.discount() > 0 ? LoyaltyService.REDEMPTION_COST : 0);
        user.setPoints(points + LoyaltyService.pointsEarned(seats.size(), snackItems));
        userRepository.save(user);

        logger.info("Booking {} created for {}: seats {}, total PKR {}", booking.getId(), user.getUsername(),
                booking.getSeats(), quote.total());
        return booking;
    }

    private Quote price(User user, List<String> seats, Map<Snack, Integer> snacks, boolean applyRedemption) {
        double ticketTotal = seats.stream().mapToDouble(PaymentService::seatPrice).sum();
        double snackTotal = snacks.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().getPrice() * entry.getValue())
                .sum();
        double discount = 0;
        if (applyRedemption) {
            if (user.getPoints() < LoyaltyService.REDEMPTION_COST) {
                throw new IllegalArgumentException(
                        "You need " + LoyaltyService.REDEMPTION_COST + " points to redeem a free ticket");
            }
            // The reward covers the cheapest ticket in the order
            discount = seats.stream().mapToDouble(PaymentService::seatPrice).min().orElse(0);
        }
        List<Quote.Line> lines = new ArrayList<>();
        snacks.forEach((snack, quantity) -> lines.add(
                new Quote.Line(snack.getName(), quantity, snack.getPrice() * quantity)));
        return new Quote(seats, ticketTotal, lines, snackTotal, discount, ticketTotal + snackTotal - discount);
    }

    private Map<Snack, Integer> loadSnacks(Map<Long, Integer> requested) {
        Map<Snack, Integer> snacks = new LinkedHashMap<>();
        if (requested == null) {
            return snacks;
        }
        requested.forEach((id, quantity) -> {
            if (quantity == null || quantity <= 0) {
                return;
            }
            Snack snack = snackRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Snack not found: " + id));
            snacks.put(snack, quantity);
        });
        return snacks;
    }

    private void charge(double amount, String paymentMethodId, User user) {
        if (amount <= 0) {
            return;
        }
        if (isDemoMode()) {
            logger.warn("Stripe is not configured; booking for {} recorded without charging PKR {}",
                    user.getUsername(), amount);
            return;
        }
        if (paymentMethodId == null || paymentMethodId.isBlank()) {
            throw new IllegalArgumentException("Card details are required");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("amount", Math.round(amount * 100));
        params.put("currency", CURRENCY);
        params.put("payment_method", paymentMethodId);
        params.put("payment_method_types", List.of("card"));
        params.put("confirm", true);
        try {
            PaymentIntent intent = PaymentIntent.create(params,
                    RequestOptions.builder().setApiKey(stripeSecretKey).build());
            if (!"succeeded".equals(intent.getStatus())) {
                throw new IllegalStateException("Payment was not completed (status: " + intent.getStatus() + ")");
            }
        } catch (StripeException e) {
            throw new IllegalStateException("Payment failed: " + e.getMessage(), e);
        }
    }

    /** What the customer picked. Seats are labels such as "C7"; snacks map snack id to quantity. */
    public record OrderRequest(Long movieId, Long sessionId, List<String> seats, Map<Long, Integer> snacks,
            boolean applyRedemption, String paymentMethodId) {
    }

    public record Quote(List<String> seats, double ticketTotal, List<Line> snacks, double snackTotal,
            double discount, double total) {

        public record Line(String name, int quantity, double amount) {
        }
    }
}
