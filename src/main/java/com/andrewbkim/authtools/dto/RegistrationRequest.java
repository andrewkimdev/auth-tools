package com.andrewbkim.authtools.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for user registration requests.
 * Includes validation rules for the incoming data.
 */
@Getter
@Setter
public class RegistrationRequest {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be a valid email address")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password be at least 8 characters long")
    private String password;
}
