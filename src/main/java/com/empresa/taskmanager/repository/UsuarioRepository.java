package com.empresa.taskmanager.repository;

import com.empresa.taskmanager.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla usuarios. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
