package com.empresa.taskmanager.model;

import jakarta.persistence.*;

/**
 * Catálogo de estados ya creado en MySQL. Cada tarea apunta a una fila por idestado.
 */
@Entity
@Table(name = "estados")
public class Estado {

    /** Clave primaria generada por MySQL. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre único del estado, tal como está definido en la columna. */
    @Column(nullable = false, length = 50, unique = true)
    private String nombre;

    /** Constructor sin argumentos requerido por JPA. */
    public Estado() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
