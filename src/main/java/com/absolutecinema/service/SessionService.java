package com.absolutecinema.service;

import com.absolutecinema.entity.Booking;
import com.absolutecinema.entity.Session;
import com.absolutecinema.repository.BookingRepository;
import com.absolutecinema.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final BookingRepository bookingRepository;

    public SessionService(SessionRepository sessionRepository, BookingRepository bookingRepository) {
        this.sessionRepository = sessionRepository;
        this.bookingRepository = bookingRepository;
    }

    /** Showtimes for a movie that have not started yet. */
    public List<Session> getUpcomingSessions(Long movieId) {
        return sessionRepository.findUpcomingByMovieId(movieId, LocalDateTime.now());
    }

    /** Seat labels (e.g. "C7") already sold for a showtime. */
    @Transactional(readOnly = true)
    public Set<String> getOccupiedSeats(Long sessionId) {
        Set<String> occupied = new TreeSet<>();
        bookingRepository.findBySessionId(sessionId).stream()
                .map(Booking::getSeats)
                .forEach(seats -> occupied.addAll(SeatLayout.parse(seats)));
        return occupied;
    }

    public List<Session> findAll() {
        return sessionRepository.findAll();
    }

    public Optional<Session> findById(Long id) {
        return sessionRepository.findById(id);
    }

    public List<Session> findByMovieId(Long movieId) {
        return sessionRepository.findByMovieId(movieId);
    }

    public Session save(Session session) {
        return sessionRepository.save(session);
    }

    public void deleteById(Long id) {
        sessionRepository.deleteById(id);
    }
}
