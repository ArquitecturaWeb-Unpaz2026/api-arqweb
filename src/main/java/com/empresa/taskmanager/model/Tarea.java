package com.empresa.taskmanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Entidad del dominio mapeada a la tabla tareas que ya existe en MySQL.
 * Los nombres de columna respetan el esquema creado para este desarrollo.
 */
@Entity
@Table(name = "tareas")
public class Tarea {

    /** Clave primaria generada por MySQL. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Título obligatorio, limitado a la longitud de la columna. */
    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String titulo;

    /** Descripción opcional, alineada con la columna text que admite NULL. */
    @Column(columnDefinition = "text")
    private String descripcion;

    /** Estado de la tarea. La columna física es idestado. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "idestado", nullable = false)
    private Estado estado;

    /** Usuario asignado. La columna idusuario admite NULL. */
    @ManyToOne
    @JoinColumn(name = "idusuario")
    private Usuario usuario;

    /** Usuario que dio de alta la tarea. La columna idcreador es obligatoria. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "idcreador", nullable = false)
    private Usuario creador;

    /** Alta generada por el DEFAULT CURRENT_TIMESTAMP de la tabla. */
    @Column(name = "fechacreacion", insertable = false, updatable = false)
    private Instant fechaCreacion;

    /** Inicio previsto del trabajo. */
    @Column(name = "fechainicio")
    private Instant fechaInicio;

    /** Vencimiento previsto del trabajo. */
    @Column(name = "fechavencimiento")
    private Instant fechaVencimiento;

    /** Marca de baja lógica. NULL significa que la tarea sigue vigente. */
    @Column(name = "fechaeliminacion")
    private Instant fechaEliminacion;

    /** Constructor sin argumentos requerido por JPA. */
    public Tarea() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Usuario getCreador() { return creador; }
    public void setCreador(Usuario creador) { this.creador = creador; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }
    public Instant getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(Instant fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public Instant getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(Instant fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }
}
