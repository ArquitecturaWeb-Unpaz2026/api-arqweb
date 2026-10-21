package com.empresa.taskmanager.model;

/** Estados posibles del recurso Tarea. Enum evita guardar valores arbitrarios. */
public enum EstadoTarea {
    /** Estado inicial que asigna el servicio al crear una tarea. */
    PENDIENTE,
    /** Estado previsto cuando posteriormente se incorpore la asignación. */
    ASIGNADA,
    /** Estado previsto cuando el técnico comienza el trabajo. */
    EN_PROGRESO,
    /** Estado final previsto para una tarea terminada. */
    RESUELTA,
    /** Estado final previsto para una tarea cancelada. */
    CANCELADA
}
