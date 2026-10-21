package com.absolutecinema.service;

import com.absolutecinema.entity.Booking;
import com.absolutecinema.entity.Movie;
import com.absolutecinema.entity.Session;
import com.absolutecinema.entity.User;
import com.absolutecinema.repository.BookingRepository;
import com.absolutecinema.repository.BookingSnackRepository;
import com.absolutecinema.repository.MovieRepository;
import com.absolutecinema.repository.SessionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSnackRepository bookingSnackRepository;
    private final MovieRepository movieRepository;
    private final SessionRepository sessionRepository;

    public BookingService(BookingRepository bookingRepository, BookingSnackRepository bookingSnackRepository,
            MovieRepository movieRepository, SessionRepository sessionRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingSnackRepository = bookingSnackRepository;
        this.movieRepository = movieRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(User user) {
        return withDetails(bookingRepository.findByUserOrderByBookingTimeDesc(user));
    }

    /** Every booking, newest first, for the admin view. */
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return withDetails(bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "bookingTime")));
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + id));
    }

    /** Deletes the booking together with its snack line items. */
    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = getBookingById(id);
        bookingSnackRepository.deleteByBooking(booking);
        bookingRepository.delete(booking);
    }

    /** Fills the transient movie and session of each booking using one query per table. */
    private List<Booking> withDetails(List<Booking> bookings) {
        Map<Long, Movie> movies = movieRepository.findAllById(
                bookings.stream().map(Booking::getMovieId).distinct().toList()).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));
        Map<Long, Session> sessions = sessionRepository.findAllById(
                bookings.stream().map(Booking::getSessionId).distinct().toList()).stream()
                .collect(Collectors.toMap(Session::getId, Function.identity()));
        bookings.forEach(booking -> {
            booking.setMovie(movies.get(booking.getMovieId()));
            booking.setSession(sessions.get(booking.getSessionId()));
        });
        return bookings;
    }
}
