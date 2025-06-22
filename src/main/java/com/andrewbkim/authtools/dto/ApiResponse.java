package com.andrewbkim.authtools.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * A generic, simple response object for successful API calls.
 */
@Getter
@Setter
@AllArgsConstructor
public class ApiResponse {
    private String message;
}
