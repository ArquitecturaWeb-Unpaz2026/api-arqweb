package com.empresa.taskmanager.exception;

/** Excepción de aplicación para representar que no se encontró un recurso. */
public class ResourceNotFoundException extends RuntimeException {
    /** Conserva un mensaje que el manejador puede devolver en la respuesta HTTP. */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
