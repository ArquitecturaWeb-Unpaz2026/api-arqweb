package com.empresa.taskmanager.model;

import com.empresa.taskmanager.enums.EstadoTarea;
import com.empresa.taskmanager.enums.Prioridad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Tarea {
    private Long id;
    private String titulo;
    private String descripcion;
    private Prioridad prioridad;
    private EstadoTarea estado;
    private Long administradorCreadorId;
    private Long tecnicoAsignadoId;
    private Long grupoAsignadoId;
    private String trabajoRealizado;
    private String resolucion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private final List<HistorialTarea> historial = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public EstadoTarea getEstado() { return estado; }
    public void setEstado(EstadoTarea estado) { this.estado = estado; }
    public Long getAdministradorCreadorId() { return administradorCreadorId; }
    public void setAdministradorCreadorId(Long id) { this.administradorCreadorId = id; }
    public Long getTecnicoAsignadoId() { return tecnicoAsignadoId; }
    public void setTecnicoAsignadoId(Long id) { this.tecnicoAsignadoId = id; }
    public Long getGrupoAsignadoId() { return grupoAsignadoId; }
    public void setGrupoAsignadoId(Long id) { this.grupoAsignadoId = id; }
    public String getTrabajoRealizado() { return trabajoRealizado; }
    public void setTrabajoRealizado(String trabajoRealizado) { this.trabajoRealizado = trabajoRealizado; }
    public String getResolucion() { return resolucion; }
    public void setResolucion(String resolucion) { this.resolucion = resolucion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    public List<HistorialTarea> getHistorial() { return List.copyOf(historial); }
    public void agregarHistorial(HistorialTarea entrada) { historial.add(entrada); }
}
