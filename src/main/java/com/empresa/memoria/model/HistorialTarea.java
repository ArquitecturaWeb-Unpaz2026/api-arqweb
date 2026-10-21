package com.empresa.memoria.model;

import java.time.LocalDateTime;

public record HistorialTarea(LocalDateTime fecha, String accion, Long usuarioId, String detalle) { }
