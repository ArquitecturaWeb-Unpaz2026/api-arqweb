package com.empresa.taskmanager.repository;

import com.empresa.taskmanager.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla roles. */
public interface RolRepository extends JpaRepository<Rol, Long> {
}
