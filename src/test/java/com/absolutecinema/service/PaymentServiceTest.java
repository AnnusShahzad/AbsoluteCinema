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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final BookingSnackRepository bookingSnackRepository = mock(BookingSnackRepository.class);
    private final SnackRepository snackRepository = mock(SnackRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MovieRepository movieRepository = mock(MovieRepository.class);
    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final SessionService sessionService = mock(SessionService.class);

    // No Stripe key: demo mode, so no card is charged
    private final PaymentService paymentService = new PaymentService(bookingRepository, bookingSnackRepository,
            snackRepository, userRepository, movieRepository, sessionRepository, sessionService, "");

    private final Movie movie = new Movie();
    private final Session session = new Session();
    private final Snack popcorn = new Snack();
    private final User customer = new User();

    @BeforeEach
    void setUp() {
        movie.setId(1L);
        movie.setTicketssold(10);
        session.setId(11L);
        session.setMovie(movie);
        session.setSessionTime(LocalDateTime.now().plusDays(1));
        session.setAvailableSeats(96);
        popcorn.setId(5L);
        popcorn.setName("Popcorn");
        popcorn.setPrice(450);
        popcorn.setQuantity(10);
        customer.setUsername("demo");
        customer.setPoints(300);

        when(sessionRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(session));
        when(snackRepository.findById(5L)).thenReturn(Optional.of(popcorn));
        when(sessionService.getOccupiedSeats(11L)).thenReturn(Set.of("C1"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(99L);
            return booking;
        });
    }

    @Test
    void quotePricesPremiumSeatsAndSnacksOnTheServer() {
        PaymentService.Quote quote = paymentService.quote(customer,
                order(List.of("A1", "C2"), Map.of(5L, 2), false));

        assertThat(quote.ticketTotal()).isEqualTo(PaymentService.PREMIUM_SEAT_PRICE + PaymentService.REGULAR_SEAT_PRICE);
        assertThat(quote.snackTotal()).isEqualTo(900);
        assertThat(quote.discount()).isZero();
        assertThat(quote.total()).isEqualTo(1500 + 1200 + 900);
    }

    @Test
    void redemptionTakesOffTheCheapestTicket() {
        PaymentService.Quote quote = paymentService.quote(customer, order(List.of("A1", "C2"), Map.of(), true));

        assertThat(quote.discount()).isEqualTo(PaymentService.REGULAR_SEAT_PRICE);
        assertThat(quote.total()).isEqualTo(PaymentService.PREMIUM_SEAT_PRICE);
    }

    @Test
    void redemptionNeedsEnoughPoints() {
        customer.setPoints(LoyaltyService.REDEMPTION_COST - 1);

        assertThatThrownBy(() -> paymentService.quote(customer, order(List.of("C2"), Map.of(), true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkoutRecordsTheBookingAndUpdatesCounters() {
        Booking booking = paymentService.checkout(customer, order(List.of("C2", "C3"), Map.of(5L, 1), true));

        assertThat(booking.getSeats()).isEqualTo("C2,C3");
        assertThat(booking.getNumberOfTickets()).isEqualTo(2);
        assertThat(booking.getTotalPrice()).isEqualTo(1200 + 450);
        assertThat(movie.getTicketssold()).isEqualTo(12);
        assertThat(session.getAvailableSeats()).isEqualTo(94);
        assertThat(popcorn.getQuantity()).isEqualTo(9);
        assertThat(popcorn.getSnackssold()).isEqualTo(1);
        // 300 - 250 redeemed + 2 tickets * 50 + 1 snack * 20
        assertThat(customer.getPoints()).isEqualTo(170);

        ArgumentCaptor<BookingSnack> line = ArgumentCaptor.forClass(BookingSnack.class);
        verify(bookingSnackRepository).save(line.capture());
        assertThat(line.getValue().getQuantity()).isEqualTo(1);
    }

    @Test
    void checkoutRejectsSeatsThatAreAlreadyTaken() {
        assertThatThrownBy(() -> paymentService.checkout(customer, order(List.of("C1", "C2"), Map.of(), false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("C1");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void checkoutRejectsPastShowtimesAndMismatchedMovies() {
        session.setSessionTime(LocalDateTime.now().minusMinutes(1));
        assertThatThrownBy(() -> paymentService.checkout(customer, order(List.of("C2"), Map.of(), false)))
                .hasMessageContaining("already started");

        session.setSessionTime(LocalDateTime.now().plusDays(1));
        PaymentService.OrderRequest otherMovie = new PaymentService.OrderRequest(2L, 11L, List.of("C2"), Map.of(),
                false, null);
        assertThatThrownBy(() -> paymentService.checkout(customer, otherMovie))
                .hasMessageContaining("does not belong");
    }

    @Test
    void checkoutRejectsOrdersThatExceedSnackStock() {
        assertThatThrownBy(() -> paymentService.checkout(customer, order(List.of("C2"), Map.of(5L, 11), false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only 10 Popcorn left");
    }

    private static PaymentService.OrderRequest order(List<String> seats, Map<Long, Integer> snacks, boolean redeem) {
        return new PaymentService.OrderRequest(1L, 11L, seats, snacks, redeem, null);
    }
}
