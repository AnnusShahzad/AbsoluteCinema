package com.absolutecinema.service;

import com.absolutecinema.entity.Booking;
import com.absolutecinema.entity.Movie;
import com.absolutecinema.entity.Session;
import com.absolutecinema.repository.BookingRepository;
import com.absolutecinema.repository.BookingSnackRepository;
import com.absolutecinema.repository.MovieRepository;
import com.absolutecinema.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final BookingSnackRepository bookingSnackRepository = mock(BookingSnackRepository.class);
    private final MovieRepository movieRepository = mock(MovieRepository.class);
    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final BookingService bookingService =
            new BookingService(bookingRepository, bookingSnackRepository, movieRepository, sessionRepository);

    @Test
    void getAllBookingsAttachesMoviesAndSessionsWithOneLookupEach() {
        Booking first = booking(1L, 10L, 100L);
        Booking second = booking(2L, 10L, 101L);
        when(bookingRepository.findAll(any(Sort.class))).thenReturn(List.of(first, second));
        Movie movie = new Movie();
        movie.setId(10L);
        when(movieRepository.findAllById(anyList())).thenReturn(List.of(movie));
        when(sessionRepository.findAllById(anyList())).thenReturn(List.of(session(100L), session(101L)));

        List<Booking> bookings = bookingService.getAllBookings();

        assertThat(bookings).extracting(Booking::getMovie).containsOnly(movie);
        assertThat(bookings).extracting(b -> b.getSession().getId()).containsExactly(100L, 101L);
        verify(movieRepository, times(1)).findAllById(anyList());
        verify(sessionRepository, times(1)).findAllById(anyList());
    }

    @Test
    void cancelBookingRemovesSnackLineItemsBeforeTheBooking() {
        Booking booking = booking(5L, 10L, 100L);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        bookingService.cancelBooking(5L);

        InOrder order = inOrder(bookingSnackRepository, bookingRepository);
        order.verify(bookingSnackRepository).deleteByBooking(booking);
        order.verify(bookingRepository).delete(booking);
    }

    @Test
    void cancelBookingFailsForUnknownId() {
        when(bookingRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(42L)).isInstanceOf(NoSuchElementException.class);
        verify(bookingRepository, never()).delete(any());
    }

    private static Booking booking(Long id, Long movieId, Long sessionId) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setMovieId(movieId);
        booking.setSessionId(sessionId);
        return booking;
    }

    private static Session session(Long id) {
        Session session = new Session();
        session.setId(id);
        return session;
    }
}
