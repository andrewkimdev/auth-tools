package com.andrewbkim.authtools.controller;

import com.andrewbkim.authtools.service.UserService;
import com.andrewbkim.authtools.dto.RegistrationRequest;
import com.andrewbkim.authtools.dto.ResendTokenRequest;
import com.andrewbkim.authtools.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;

    @PostMapping("/")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegistrationRequest request) {
        userService.registerUser(request);
        return new ResponseEntity<>(
                new ApiResponse("Registration successful. Please check your email to verify your account"),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/confirm")
    public ResponseEntity<ApiResponse> confirm(@RequestParam("token") String token) {
        userService.confirmToken(token);
        return ResponseEntity.ok(new ApiResponse("Your account has been successfully activated"));
    }

    @PostMapping("/resend-token")
    public ResponseEntity<ApiResponse> resendToken(@Valid @RequestBody ResendTokenRequest request) {
        userService.resendVerificationToken(request.getEmail());
        return ResponseEntity.ok(new ApiResponse("A new verification email has been sent"));
    }
}
