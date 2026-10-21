package com.absolutecinema.repository;

import com.absolutecinema.entity.Booking;
import com.absolutecinema.entity.BookingSnack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingSnackRepository extends JpaRepository<BookingSnack, Long> {
    void deleteByBooking(Booking booking);
}