package com.empresa.taskmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Manejo transversal de errores web. Traduce un error de dominio a HTTP 404
 * sin repetir lógica de respuesta en cada endpoint del controlador.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Formato JSON pequeño y estable para clientes HTTP.
     * @param timestamp instante en que el backend detectó el error
     * @param status código HTTP enviado en la respuesta
     * @param error nombre legible del estado HTTP
     * @param detalle explicación breve útil para quien consume la API
     */
    public record ErrorResponse(Instant timestamp, int status, String error, String detalle) {
    }

    /** Convierte ResourceNotFoundException en una respuesta REST 404 Not Found. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(ResourceNotFoundException exception) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), exception.getMessage()));
    }

    /** No revela si falló el email o la contraseña. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> credencialesInvalidas(BadCredentialsException exception) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), "Email o contraseña incorrectos"));
    }

    /** Un integrante no puede reasignar, borrar ni editar tareas ajenas. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> prohibido(AccessDeniedException exception) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), "No tiene permiso para esta operación"));
    }

    /** Convierte un dato de entrada incompleto en una respuesta REST 400 Bad Request. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> solicitudInvalida(IllegalArgumentException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), exception.getMessage()));
    }
}
