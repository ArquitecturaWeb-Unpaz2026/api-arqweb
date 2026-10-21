package com.empresa.taskmanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Entidad del dominio y recurso principal de la API.
 * JPA la mapea a una tabla relacional; sus campos son los datos que la API
 * recibe y devuelve como JSON en esta primera versión.
 */
@Entity
@Table(name = "tareas")
public class Tarea {

    /** Clave primaria generada por PostgreSQL; identifica cada recurso. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Título obligatorio, validado en la entrada y limitado en la columna SQL. */
    @NotBlank
    @Size(max = 180)
    @Column(nullable = false, length = 180)
    private String titulo;

    /** Descripción obligatoria que explica el trabajo a realizar. */
    @NotBlank
    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    /** Prioridad elegida del catálogo, persistida como texto legible. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridad prioridad;

    /** Estado de dominio; el servicio inicia cada tarea en PENDIENTE. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTarea estado = EstadoTarea.PENDIENTE;

    /** Fecha UTC de alta, fijada una sola vez antes del primer INSERT. */
    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion;

    /** Fecha UTC modificada antes de cada UPDATE para auditoría básica. */
    @Column(nullable = false)
    private Instant fechaActualizacion;

    /** Constructor sin argumentos requerido por JPA al reconstruir la entidad. */
    public Tarea() {
    }

    /** Callback JPA que inicializa ambas fechas antes de insertar el registro. */
    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    /** Callback JPA que actualiza la fecha de modificación antes de guardar. */
    @PreUpdate
    void alActualizar() {
        fechaActualizacion = Instant.now();
    }

    /** Devuelve el identificador persistido de la tarea. */
    public Long getId() { return id; }
    /** Permite a JPA y al servicio asignar o limpiar el identificador. */
    public void setId(Long id) { this.id = id; }
    /** Devuelve el título para la respuesta JSON. */
    public String getTitulo() { return titulo; }
    /** Establece el título validado que llegó en la petición HTTP. */
    public void setTitulo(String titulo) { this.titulo = titulo; }
    /** Devuelve la descripción para la respuesta JSON. */
    public String getDescripcion() { return descripcion; }
    /** Establece la descripción recibida en la petición HTTP. */
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    /** Devuelve la prioridad actual. */
    public Prioridad getPrioridad() { return prioridad; }
    /** Establece la prioridad validada de la tarea. */
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    /** Devuelve el estado actual del dominio. */
    public EstadoTarea getEstado() { return estado; }
    /** Permite al servicio cambiar el estado cuando la regla de negocio lo habilite. */
    public void setEstado(EstadoTarea estado) { this.estado = estado; }
    /** Devuelve la fecha de creación, que no cambia tras el primer INSERT. */
    public Instant getFechaCreacion() { return fechaCreacion; }
    /** Devuelve la fecha de la última modificación. */
    public Instant getFechaActualizacion() { return fechaActualizacion; }
}
