package com.absolutecinema.service;

import com.absolutecinema.entity.Booking;
import com.absolutecinema.entity.BookingSnack;
import com.absolutecinema.entity.Feedback;
import com.absolutecinema.entity.Movie;
import com.absolutecinema.entity.Snack;
import com.absolutecinema.repository.BookingRepository;
import com.absolutecinema.repository.BookingSnackRepository;
import com.absolutecinema.repository.FeedbackRepository;
import com.absolutecinema.repository.MovieRepository;
import com.absolutecinema.repository.SnackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final BookingSnackRepository bookingSnackRepository = mock(BookingSnackRepository.class);
    private final FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
    private final MovieRepository movieRepository = mock(MovieRepository.class);
    private final SnackRepository snackRepository = mock(SnackRepository.class);
    private final LoyaltyService loyaltyService = mock(LoyaltyService.class);

    private AnalyticsService analyticsService;

    private final Movie dune = movie(1L, "Dune", "Sci-Fi", 30);
    private final Movie alien = movie(2L, "Alien", "Sci-Fi", 10);
    private final Movie up = movie(3L, "Up", "Animation", 25);
    private final Snack popcorn = snack(1L, "Popcorn", 5.0, 40);
    private final Snack cola = snack(2L, "Cola", 3.0, 70);

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(bookingRepository, bookingSnackRepository, feedbackRepository,
                movieRepository, snackRepository, loyaltyService);
        when(movieRepository.findAll()).thenReturn(List.of(dune, alien, up));
        when(snackRepository.findAll()).thenReturn(List.of(popcorn, cola));
        when(feedbackRepository.findAll()).thenReturn(List.of(feedback(5), feedback(4)));
    }

    @Test
    void reportSplitsBookingRevenueBetweenTicketsAndSnacks() {
        Booking withSnacks = booking(1L, 34.0, LocalDateTime.now());
        Booking ticketsOnly = booking(2L, 24.0, LocalDateTime.now().minusMonths(1));
        when(bookingRepository.findAll()).thenReturn(List.of(withSnacks, ticketsOnly));
        when(bookingSnackRepository.findAll()).thenReturn(List.of(
                bookingSnack(withSnacks, popcorn, 2), bookingSnack(withSnacks, cola, 1)));

        AnalyticsService.AnalyticsReport report = analyticsService.getReport(12);

        assertThat(report.snackRevenue()).isEqualTo(13.0);
        assertThat(report.ticketRevenue()).isEqualTo(21.0 + 24.0);
        assertThat(report.totalRevenue()).isEqualTo(58.0);
        assertThat(report.bookings()).isEqualTo(2);
    }

    @Test
    void snackRevenueIsCappedAtTheAmountPaid() {
        // A loyalty redemption can bring the booking total below the snack list price
        Booking discounted = booking(1L, 4.0, LocalDateTime.now());
        when(bookingRepository.findAll()).thenReturn(List.of(discounted));
        when(bookingSnackRepository.findAll()).thenReturn(List.of(bookingSnack(discounted, popcorn, 2)));

        AnalyticsService.AnalyticsReport report = analyticsService.getReport(12);

        assertThat(report.snackRevenue()).isEqualTo(4.0);
        assertThat(report.ticketRevenue()).isZero();
    }

    @Test
    void monthlyRevenueCoversTheRequestedWindowAndIgnoresOlderBookings() {
        when(bookingRepository.findAll()).thenReturn(List.of(
                booking(1L, 12.0, LocalDateTime.now()),
                booking(2L, 30.0, LocalDateTime.now().minusMonths(2)),
                booking(3L, 99.0, LocalDateTime.now().minusMonths(6))));
        when(bookingSnackRepository.findAll()).thenReturn(List.of());

        List<AnalyticsService.MonthlyRevenue> months = analyticsService.getReport(3).monthlyRevenue();

        assertThat(months).hasSize(3);
        assertThat(months).extracting(AnalyticsService.MonthlyRevenue::ticketRevenue)
                .containsExactly(30.0, 0.0, 12.0);
        // Bookings outside the window still count toward the all-time total
        assertThat(analyticsService.getReport(3).ticketRevenue()).isEqualTo(141.0);
    }

    @Test
    void reportRanksMoviesSnacksAndGenres() {
        when(bookingRepository.findAll()).thenReturn(List.of());
        when(bookingSnackRepository.findAll()).thenReturn(List.of());

        AnalyticsService.AnalyticsReport report = analyticsService.getReport(12);

        assertThat(report.ticketsSold()).isEqualTo(65);
        assertThat(report.snacksSold()).isEqualTo(110);
        assertThat(report.topMovies()).extracting(Movie::getTitle).containsExactly("Dune", "Up", "Alien");
        assertThat(report.topSnacks()).extracting(Snack::getName).containsExactly("Cola", "Popcorn");
        assertThat(report.ticketsByGenre()).containsExactly(
                org.assertj.core.api.Assertions.entry("Sci-Fi", 40),
                org.assertj.core.api.Assertions.entry("Animation", 25));
        assertThat(report.averageRating()).isCloseTo(4.5, within(0.001));
    }

    private static Movie movie(Long id, String title, String genre, int ticketsSold) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setTicketssold(ticketsSold);
        return movie;
    }

    private static Snack snack(Long id, String name, double price, int sold) {
        Snack snack = new Snack();
        snack.setId(id);
        snack.setName(name);
        snack.setPrice(price);
        snack.setSnackssold(sold);
        return snack;
    }

    private static Booking booking(Long id, double total, LocalDateTime time) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setTotalPrice(total);
        booking.setNumberOfTickets(1);
        booking.setBookingTime(time);
        return booking;
    }

    private static BookingSnack bookingSnack(Booking booking, Snack snack, int quantity) {
        BookingSnack bookingSnack = new BookingSnack();
        bookingSnack.setBooking(booking);
        bookingSnack.setSnack(snack);
        bookingSnack.setQuantity(quantity);
        return bookingSnack;
    }

    private static Feedback feedback(int rating) {
        Feedback feedback = new Feedback();
        feedback.setRating(rating);
        return feedback;
    }
}
