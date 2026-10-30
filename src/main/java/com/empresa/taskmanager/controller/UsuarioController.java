package com.empresa.taskmanager.controller;

import com.empresa.taskmanager.dto.RegistroRequest;
import com.empresa.taskmanager.dto.UsuarioUpdateRequest;
import com.empresa.taskmanager.model.Usuario;
import com.empresa.taskmanager.service.UsuarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Gestión de usuarios. SecurityConfig limita estas rutas a ROLE_ADMIN. */
@Tag(name = "Usuarios")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** GET /api/usuarios: solo los que siguen vigentes. */
    @GetMapping
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    /** POST /api/usuarios: alta con los mismos datos que el registro. */
    @PostMapping
    public ResponseEntity<Usuario> crear(@Valid @RequestBody RegistroRequest request) {
        Usuario creado = usuarioService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.getId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    /** PUT /api/usuarios/{id}: nombre, correo, rol y contraseña opcional. */
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequest datos) {
        return ResponseEntity.ok(usuarioService.actualizar(id, datos));
    }

    /** DELETE /api/usuarios/{id}: marca fechaeliminacion y responde 204. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication authentication) {
        usuarioService.eliminar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
