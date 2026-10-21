package com.empresa.taskmanager.repository;

import com.empresa.taskmanager.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Capa de acceso a datos. JpaRepository aporta consultas CRUD estándar y
 * delega la persistencia a Spring Data JPA/Hibernate.
 */
public interface TareaRepository extends JpaRepository<Tarea, Long> {

    /** Tareas vigentes: la columna fechaeliminacion sigue en NULL. */
    List<Tarea> findByFechaEliminacionIsNull();

    /** Una tarea vigente. Si está dada de baja, se trata como inexistente. */
    Optional<Tarea> findByIdAndFechaEliminacionIsNull(Long id);
}
