package com.empresa.taskmanager.dto;

/** Token JWT que el cliente envía luego como Authorization: Bearer. */
public record TokenResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        String email,
        String rol
) {
}
