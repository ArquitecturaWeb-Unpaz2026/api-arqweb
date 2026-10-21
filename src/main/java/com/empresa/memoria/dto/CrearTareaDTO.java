package com.empresa.memoria.dto;

import com.empresa.memoria.enums.Prioridad;

public record CrearTareaDTO(String titulo, String descripcion, Prioridad prioridad) { }
