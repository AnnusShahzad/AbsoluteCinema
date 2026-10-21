package com.absolutecinema.repository;

import com.absolutecinema.entity.PasswordResetToken;
import com.absolutecinema.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findFirstByUserOrderByIdDesc(User user);

    void deleteByUser(User user);
}
