package com.empresa.taskmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Usuario persistido en la tabla usuarios. La tarea lo usa como creador
 * y, si corresponde, como usuario asignado.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    /** Clave primaria generada por MySQL. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre visible del usuario. */
    @Column(nullable = false, length = 100)
    private String nombre;

    /** Correo único del usuario. */
    @Column(nullable = false, length = 120, unique = true)
    private String email;

    /** Credencial guardada en la tabla. No se incluye en el JSON de respuesta. */
    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    /** Rol obligatorio. La columna física es idrol. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "idrol", nullable = false)
    private Rol rol;

    /** Alta generada por el DEFAULT CURRENT_TIMESTAMP de la tabla. */
    @Column(name = "fechacreacion", insertable = false, updatable = false)
    private Instant fechaCreacion;

    /** Marca de baja lógica. NULL significa que el usuario sigue vigente. */
    @Column(name = "fechaeliminacion")
    private Instant fechaEliminacion;

    /** Constructor sin argumentos requerido por JPA. */
    public Usuario() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(Instant fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }
}
