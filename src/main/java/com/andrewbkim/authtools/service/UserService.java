package com.andrewbkim.authtools.service;

import com.andrewbkim.authtools.repository.UserRepository;
import com.andrewbkim.authtools.dto.RegistrationRequest;
import com.andrewbkim.authtools.entity.VerificationToken;
import com.andrewbkim.authtools.repository.VerificationTokenRepository;
import com.andrewbkim.authtools.exception.RegistrationException;
import com.andrewbkim.authtools.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Transactional
    public void registerUser(RegistrationRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new RegistrationException("Email already in use", HttpStatus.CONFLICT);
        });

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        sendVerificationToken(user);
    }

    @Transactional
    public void confirmToken(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RegistrationException("Invalid verification token", HttpStatus.BAD_REQUEST));

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RegistrationException("Verification token has expired", HttpStatus.BAD_REQUEST);
        }

        User user = verificationToken.getUser();
        if (user.isEnabled()) {
            throw new RegistrationException("Account is already verified", HttpStatus.BAD_REQUEST);
        }

        user.setEnabled(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);
    }

    @Transactional
    public void resendVerificationToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RegistrationException("User not found with this email", HttpStatus.NOT_FOUND));

        if (user.isEnabled()) {
            throw new RegistrationException("Account is already verified", HttpStatus.BAD_REQUEST);
        }

        // Find existing token and delete it before creating a new one
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        sendVerificationToken(user);
    }

    private void sendVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        tokenRepository.save(verificationToken);

        String confirmationUrl = "http://localhost:8080/register/confirm?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Confirm your registration");
        message.setText("Thank you for registering. Please click the link below to activate your account:\n" + confirmationUrl + "\nThis link will expire in 15 minutes.");

        mailSender.send(message);
    }


}
