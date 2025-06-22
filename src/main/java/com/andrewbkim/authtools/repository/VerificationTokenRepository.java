package com.andrewbkim.authtools.repository;

import com.andrewbkim.authtools.entity.VerificationToken;
import com.andrewbkim.authtools.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    /**
     * Finds a verification token by its token string.
     * @param token the unique token string
     * @return an Optional containing the token if found
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * Finds a verification token associated with a specific user.
     * This is the new method required by the refactored UserService.
     * @param user the user entity
     * @return an Optional containing the token if found
     */
    Optional<VerificationToken> findByUser(User user);
}
