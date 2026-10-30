package com.empresa.taskmanager.service;

import com.empresa.taskmanager.dto.RegistroRequest;
import com.empresa.taskmanager.dto.UsuarioUpdateRequest;
import com.empresa.taskmanager.exception.ResourceNotFoundException;
import com.empresa.taskmanager.model.Rol;
import com.empresa.taskmanager.model.Usuario;
import com.empresa.taskmanager.repository.RolRepository;
import com.empresa.taskmanager.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Altas, edición y baja lógica de usuarios. La fila no se borra de MySQL. */
@Service
@Transactional
public class UsuarioService {

    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 72;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    /** Usuarios que todavía no tienen fechaeliminacion. */
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findByFechaEliminacionIsNull();
    }

    /** Alta con contraseña cifrada y un rol que ya exista. */
    public Usuario crear(RegistroRequest request) {
        return authService.registrar(request);
    }

    /** Cambia nombre, correo y rol. La contraseña solo se reemplaza si viene una nueva. */
    public Usuario actualizar(Long id, UsuarioUpdateRequest datos) {
        Usuario existente = vigente(id);
        String email = datos.email().trim();
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        existente.setNombre(datos.nombre().trim());
        existente.setEmail(email);
        existente.setRol(rolExistente(datos.idRol()));
        String password = datos.password();
        if (password != null && !password.isBlank()) {
            if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
                throw new IllegalArgumentException("La contraseña debe tener entre 8 y 72 caracteres");
            }
            existente.setPassword(passwordEncoder.encode(password));
        }
        return usuarioRepository.save(existente);
    }

    /**
     * Baja lógica: completa fechaeliminacion. No permite que el actor
     * se dé de baja a sí mismo, porque perdería el acceso de administrador.
     */
    public void eliminar(Long id, String emailActor) {
        Usuario existente = vigente(id);
        if (existente.getEmail().equalsIgnoreCase(emailActor)) {
            throw new IllegalArgumentException("No puede darse de baja a sí mismo");
        }
        existente.setFechaEliminacion(Instant.now());
        usuarioRepository.save(existente);
    }

    private Usuario vigente(Long id) {
        return usuarioRepository.findByIdAndFechaEliminacionIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario con id " + id));
    }

    private Rol rolExistente(Long idRol) {
        return rolRepository.findById(idRol)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el rol con id " + idRol));
    }
}
