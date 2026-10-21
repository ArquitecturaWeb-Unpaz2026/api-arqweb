package com.empresa.taskmanager.repository;

import com.empresa.taskmanager.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acceso a la tabla usuarios. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busca por el correo único, que es la credencial de login. */
    Optional<Usuario> findByEmail(String email);

    /** Evita registrar dos veces el mismo correo. */
    boolean existsByEmail(String email);
}
