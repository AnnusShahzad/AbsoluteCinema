package com.absolutecinema.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "number_of_tickets", nullable = false)
    private Integer numberOfTickets;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;

    @Transient
    private Movie movie;

    @Transient
    private Session session;
}