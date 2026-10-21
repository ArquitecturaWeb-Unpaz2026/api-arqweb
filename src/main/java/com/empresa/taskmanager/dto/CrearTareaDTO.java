package com.empresa.taskmanager.dto;

import com.empresa.taskmanager.enums.Prioridad;

public record CrearTareaDTO(String titulo, String descripcion, Prioridad prioridad) { }
