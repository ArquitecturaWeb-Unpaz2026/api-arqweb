package com.empresa.taskmanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Credenciales de entrada al login. */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
