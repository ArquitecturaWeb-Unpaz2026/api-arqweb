package com.empresa.taskmanager.service;

import com.empresa.taskmanager.dto.LoginRequest;
import com.empresa.taskmanager.dto.RegistroRequest;
import com.empresa.taskmanager.dto.TokenResponse;
import com.empresa.taskmanager.exception.ResourceNotFoundException;
import com.empresa.taskmanager.model.Rol;
import com.empresa.taskmanager.model.Usuario;
import com.empresa.taskmanager.repository.RolRepository;
import com.empresa.taskmanager.repository.UsuarioRepository;
import com.empresa.taskmanager.security.JwtService;
import jakarta.persistence.EntityManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro y login contra la tabla usuarios. La contraseña se persiste con BCrypt. */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EntityManager entityManager;

    public AuthService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            EntityManager entityManager) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.entityManager = entityManager;
    }

    /** Crea el usuario con la contraseña cifrada y un rol que ya exista en la tabla roles. */
    @Transactional
    public Usuario registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        Rol rol = rolRepository.findById(request.idRol())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el rol con id " + request.idRol()));
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(rol);
        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        entityManager.refresh(guardado);
        return guardado;
    }

    /** Comprueba email y contraseña y devuelve un JWT. */
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario"));
        return new TokenResponse(
                jwtService.generar(usuario),
                "Bearer",
                jwtService.getExpirationSeconds(),
                usuario.getEmail(),
                usuario.getRol().getNombre());
    }
}
