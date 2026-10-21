package com.empresa.taskmanager.repository;

import com.empresa.taskmanager.model.Estado;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla estados. */
public interface EstadoRepository extends JpaRepository<Estado, Long> {
}
