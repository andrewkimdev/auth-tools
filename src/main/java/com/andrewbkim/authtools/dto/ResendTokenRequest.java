package com.andrewbkim.authtools.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object for resending a verification token.
 */
@Getter
@Setter
public class ResendTokenRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be a valid email address")
    private String email;
}
