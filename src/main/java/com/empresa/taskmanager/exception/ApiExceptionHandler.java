package com.empresa.taskmanager.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> manejarError(ResponseStatusException excepcion) {
        HttpStatusCode estado = excepcion.getStatusCode();
        Map<String, Object> cuerpo = Map.of(
                "fecha", LocalDateTime.now(),
                "status", estado.value(),
                "error", estado.toString(),
                "mensaje", excepcion.getReason() == null ? "Solicitud inválida" : excepcion.getReason()
        );
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
