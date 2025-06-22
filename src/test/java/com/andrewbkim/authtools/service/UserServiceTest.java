package com.andrewbkim.authtools.service;

import com.andrewbkim.authtools.dto.RegistrationRequest;
import com.andrewbkim.authtools.entity.User;
import com.andrewbkim.authtools.entity.VerificationToken;
import com.andrewbkim.authtools.exception.RegistrationException;
import com.andrewbkim.authtools.repository.UserRepository;
import com.andrewbkim.authtools.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the UserService class.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks // Creates an instance of UserService and injects the mocks into it
    private UserService userService;

    private RegistrationRequest registrationRequest;
    private User user;

    @BeforeEach
    void setUp() {
        // Common setup for tests
        registrationRequest = new RegistrationRequest();
        registrationRequest.setEmail("test@example.com");
        registrationRequest.setPassword("password123");

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setEnabled(false);
    }

    @Test
    @DisplayName("Should successfully register a new user with a unique email")
    void registerUser_whenEmailIsNew_shouldSucceed() {
        // Given: The email is not already in use
        when(userRepository.findByEmail(registrationRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registrationRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When: registerUser is called
        userService.registerUser(registrationRequest);

        // Then: A new user should be saved with an encoded password
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertFalse(savedUser.isEnabled());

        // Then: A verification token should be created and saved
        verify(tokenRepository).save(any(VerificationToken.class));

        // Then: A verification email should be sent
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should throw CONFLICT exception when registering with an existing email")
    void registerUser_whenEmailExists_shouldThrowException() {
        // Given: The email is already in use
        when(userRepository.findByEmail(registrationRequest.getEmail())).thenReturn(Optional.of(user));

        // When & Then: Calling registerUser should throw a RegistrationException
        RegistrationException exception = assertThrows(RegistrationException.class, () ->
            userService.registerUser(registrationRequest)
        );

        assertEquals("Email already in use", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        // Then: No interactions with other mocks should occur
        verify(passwordEncoder, never()).encode(anyString());
        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should enable user when a valid confirmation token is provided")
    void confirmToken_whenTokenIsValid_shouldEnableUser() {
        // Given: A valid, unexpired token exists
        VerificationToken token = new VerificationToken("valid-token", user);
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        // When: confirmToken is called
        userService.confirmToken("valid-token");

        // Then: The user's enabled status should be true
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isEnabled());

        // Then: The token should be deleted
        verify(tokenRepository).delete(token);
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST exception for an invalid confirmation token")
    void confirmToken_whenTokenIsInvalid_shouldThrowException() {
        // Given: The token does not exist
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // When & Then: An exception should be thrown
        RegistrationException exception = assertThrows(RegistrationException.class, () ->
            userService.confirmToken("invalid-token")
        );
        assertEquals("Invalid verification token", exception.getMessage());
    }

    @Test
    @DisplayName("Should successfully resend verification token for an unverified user")
    void confirmToken_whenTokenIsExpired_shouldThrowException() {
        // Given: A token exists but it has expired
        VerificationToken token = new VerificationToken("expired-token", user);
        token.setExpiryDate(LocalDateTime.now().minusHours(1)); // Set expiry in the past
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        // When & Then: An exception should be thrown
        RegistrationException exception = assertThrows(RegistrationException.class, () ->
            userService.confirmToken("expired-token")
        );
        assertEquals("Verification token has expired", exception.getMessage());
    }

    @Test
    @DisplayName("Should successfully resend verification token for an unverified user")
    void resendVerificationToken_whenUserExistsAndNotVerified_shouldSucceed() {
        // Given: An unverified user exists
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        // Assume no old token exists for simplicity in this test
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());

        // When: resendVerificationToken is called
        userService.resendVerificationToken("test@example.com");

        // Then: A new token should be saved and an email should be sent
        verify(tokenRepository).save(any(VerificationToken.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST exception when resending token for an already verified user")
    void resendVerificationToken_whenUserIsAlreadyVerified_shouldThrowException() {
        // Given: An already verified user exists
        user.setEnabled(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When & Then: An exception should be thrown
        RegistrationException exception = assertThrows(RegistrationException.class, () ->
            userService.resendVerificationToken("test@example.com")
        );
        assertEquals("Account is already verified", exception.getMessage());
    }

}
