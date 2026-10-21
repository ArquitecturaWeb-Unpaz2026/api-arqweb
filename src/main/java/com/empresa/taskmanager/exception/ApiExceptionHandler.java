package com.empresa.taskmanager.exception;

import org.springframework.http.HttpStatus;
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
}
