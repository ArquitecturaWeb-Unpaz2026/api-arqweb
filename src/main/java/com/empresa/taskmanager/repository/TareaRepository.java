package com.empresa.taskmanager.repository;

import com.empresa.taskmanager.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Capa de acceso a datos. JpaRepository aporta consultas CRUD estándar y
 * delega la persistencia a Spring Data JPA/Hibernate.
 */
public interface TareaRepository extends JpaRepository<Tarea, Long> {
    // No hacen falta consultas propias para el CRUD inicial.
    // Tarea es la entidad y Long es el tipo de su clave primaria.
}
