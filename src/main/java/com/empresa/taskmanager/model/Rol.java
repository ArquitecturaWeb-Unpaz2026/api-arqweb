package com.empresa.taskmanager.model;

import jakarta.persistence.*;

/**
 * Catálogo de roles ya creado en MySQL. Un usuario referencia una fila de esta tabla.
 */
@Entity
@Table(name = "roles")
public class Rol {

    /** Clave primaria generada por MySQL. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre único del rol, tal como está definido en la columna. */
    @Column(nullable = false, length = 50, unique = true)
    private String nombre;

    /** Constructor sin argumentos requerido por JPA. */
    public Rol() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
