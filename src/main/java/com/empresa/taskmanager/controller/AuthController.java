package com.empresa.taskmanager.controller;

import com.empresa.taskmanager.dto.LoginRequest;
import com.empresa.taskmanager.dto.RegistroRequest;
import com.empresa.taskmanager.dto.TokenResponse;
import com.empresa.taskmanager.model.Usuario;
import com.empresa.taskmanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Login y alta de usuarios. Estas rutas no exigen token. */
@Tag(name = "Autenticación")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Registra un usuario. La contraseña se guarda cifrada y no vuelve en el JSON. */
    @Operation(summary = "Registrar un usuario")
    @PostMapping("/registro")
    public ResponseEntity<Usuario> registrar(@Valid @RequestBody RegistroRequest request) {
        Usuario creado = authService.registrar(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.getId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    /** Devuelve un JWT si el email y la contraseña coinciden con la tabla usuarios. */
    @Operation(summary = "Iniciar sesión y obtener un token")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
