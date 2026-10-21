package com.absolutecinema.repository;

import com.absolutecinema.entity.Booking;
import com.absolutecinema.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserOrderByBookingTimeDesc(User user);

    List<Booking> findBySessionId(Long sessionId);
}