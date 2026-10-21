package com.absolutecinema.repository;

import com.absolutecinema.entity.Session;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query("SELECT s FROM Session s WHERE s.movie.id = :movieId ORDER BY s.sessionTime ASC")
    List<Session> findByMovieId(@Param("movieId") Long movieId);

    @Query("SELECT s FROM Session s WHERE s.movie.id = :movieId AND s.sessionTime > :after ORDER BY s.sessionTime ASC")
    List<Session> findUpcomingByMovieId(@Param("movieId") Long movieId, @Param("after") LocalDateTime after);

    /** Locks the session row so concurrent checkouts for the same showtime are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Session s WHERE s.id = :id")
    Optional<Session> findByIdForUpdate(@Param("id") Long id);
}
