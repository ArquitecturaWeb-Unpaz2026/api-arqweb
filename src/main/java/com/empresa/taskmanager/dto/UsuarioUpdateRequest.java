package com.empresa.taskmanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edición de un usuario vigente. La contraseña es opcional: vacía conserva la actual. */
public record UsuarioUpdateRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 120) String email,
        @Size(max = 72) String password,
        @NotNull Long idRol
) {
}
