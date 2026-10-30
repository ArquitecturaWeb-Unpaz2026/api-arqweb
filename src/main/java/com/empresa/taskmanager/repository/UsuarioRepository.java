package com.empresa.taskmanager.repository;

import com.empresa.taskmanager.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Acceso a la tabla usuarios. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busca por el correo único, que es la credencial de login. */
    Optional<Usuario> findByEmail(String email);

    /** Evita registrar dos veces el mismo correo, incluso si el anterior está dado de baja. */
    boolean existsByEmail(String email);

    /** El correo pertenece a otra fila, vigente o no. */
    boolean existsByEmailAndIdNot(String email, Long id);

    /** Usuarios vigentes: fechaeliminacion sigue en NULL. */
    List<Usuario> findByFechaEliminacionIsNull();

    /** Un usuario vigente. Si está dado de baja, se trata como inexistente. */
    Optional<Usuario> findByIdAndFechaEliminacionIsNull(Long id);
}
