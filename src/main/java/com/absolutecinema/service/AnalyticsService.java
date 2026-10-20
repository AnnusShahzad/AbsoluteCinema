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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yy");

    private final BookingRepository bookingRepository;
    private final BookingSnackRepository bookingSnackRepository;
    private final FeedbackRepository feedbackRepository;
    private final MovieRepository movieRepository;
    private final SnackRepository snackRepository;
    private final LoyaltyService loyaltyService;

    public AnalyticsService(BookingRepository bookingRepository,
            BookingSnackRepository bookingSnackRepository,
            FeedbackRepository feedbackRepository,
            MovieRepository movieRepository,
            SnackRepository snackRepository,
            LoyaltyService loyaltyService) {
        this.bookingRepository = bookingRepository;
        this.bookingSnackRepository = bookingSnackRepository;
        this.feedbackRepository = feedbackRepository;
        this.movieRepository = movieRepository;
        this.snackRepository = snackRepository;
        this.loyaltyService = loyaltyService;
    }

    /** Model attributes for the admin dashboard. */
    public Map<String, Object> getDashboardData() {
        List<Movie> movies = movieRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();

        List<Feedback> recentFeedbacks = feedbackRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(7)
                .toList();
        Map<Long, String> movieTitles = movieRepository.findAllById(
                recentFeedbacks.stream().map(Feedback::getMovieId).distinct().toList()).stream()
                .collect(Collectors.toMap(Movie::getId, Movie::getTitle));

        Map<String, Object> data = new HashMap<>();
        data.put("totalTicketsSold", movies.stream().mapToInt(Movie::getTicketssold).sum());
        data.put("totalSnacksSold", snackRepository.findAll().stream().mapToInt(Snack::getSnackssold).sum());
        data.put("totalBookings", bookings.size());
        data.put("totalLoyaltyMembers", loyaltyService.getMembers().size());
        data.put("monthlyTicketsSold", bookings.stream().mapToInt(Booking::getNumberOfTickets).sum());
        data.put("monthlySnacksSold", bookingSnackRepository.findAll().stream().mapToInt(BookingSnack::getQuantity).sum());
        data.put("topMovies", topMovies(movies, 5));
        data.put("recentFeedbacks", recentFeedbacks);
        data.put("movieTitles", movieTitles);
        return data;
    }

    /** Sales report for the analytics page, with revenue for the last {@code months} months. */
    public AnalyticsReport getReport(int months) {
        List<Movie> movies = movieRepository.findAll();
        List<Snack> snacks = snackRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();

        Map<Long, Double> snackRevenueByBooking = bookingSnackRepository.findAll().stream()
                .collect(Collectors.groupingBy(bs -> bs.getBooking().getId(),
                        Collectors.summingDouble(bs -> bs.getQuantity() * bs.getSnack().getPrice())));

        YearMonth current = YearMonth.now();
        Map<YearMonth, double[]> revenueByMonth = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            revenueByMonth.put(current.minusMonths(i), new double[2]);
        }

        double ticketRevenue = 0;
        double snackRevenue = 0;
        for (Booking booking : bookings) {
            // Snack spend is capped at the amount paid, since loyalty discounts reduce the booking total
            double snackPart = Math.min(snackRevenueByBooking.getOrDefault(booking.getId(), 0.0),
                    booking.getTotalPrice());
            double ticketPart = booking.getTotalPrice() - snackPart;
            ticketRevenue += ticketPart;
            snackRevenue += snackPart;

            double[] month = revenueByMonth.get(YearMonth.from(booking.getBookingTime()));
            if (month != null) {
                month[0] += ticketPart;
                month[1] += snackPart;
            }
        }

        List<MonthlyRevenue> monthlyRevenue = new ArrayList<>();
        revenueByMonth.forEach((month, revenue) -> monthlyRevenue.add(
                new MonthlyRevenue(month.format(MONTH_LABEL), revenue[0], revenue[1])));

        Map<String, Integer> ticketsByGenre = movies.stream()
                .collect(Collectors.groupingBy(Movie::getGenre, Collectors.summingInt(Movie::getTicketssold)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        double averageRating = feedbackRepository.findAll().stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0);

        return new AnalyticsReport(
                movies.stream().mapToInt(Movie::getTicketssold).sum(),
                snacks.stream().mapToInt(Snack::getSnackssold).sum(),
                ticketRevenue,
                snackRevenue,
                bookings.size(),
                averageRating,
                monthlyRevenue,
                topMovies(movies, 5),
                snacks.stream().sorted(Comparator.comparingInt(Snack::getSnackssold).reversed()).limit(5).toList(),
                ticketsByGenre);
    }

    private static List<Movie> topMovies(List<Movie> movies, int limit) {
        return movies.stream()
                .sorted(Comparator.comparingInt(Movie::getTicketssold).reversed())
                .limit(limit)
                .toList();
    }

    public record MonthlyRevenue(String label, double ticketRevenue, double snackRevenue) {
    }

    public record AnalyticsReport(
            long ticketsSold,
            long snacksSold,
            double ticketRevenue,
            double snackRevenue,
            long bookings,
            double averageRating,
            List<MonthlyRevenue> monthlyRevenue,
            List<Movie> topMovies,
            List<Snack> topSnacks,
            Map<String, Integer> ticketsByGenre) {

        public double totalRevenue() {
            return ticketRevenue + snackRevenue;
        }
    }
}
