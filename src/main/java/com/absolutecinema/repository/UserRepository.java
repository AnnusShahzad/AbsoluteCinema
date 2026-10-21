package com.absolutecinema.repository;

import com.absolutecinema.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByEmailIgnoreCase(String email);

    User findByUsername(String username);

    List<User> findByAccountTypeIgnoreCaseOrderByPointsDesc(String accountType);

    @Query("SELECT MAX(u.id) FROM User u")
    Optional<Long> findMaxId();
}