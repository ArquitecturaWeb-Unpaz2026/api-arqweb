package com.empresa.memoria.service;

import com.empresa.memoria.dto.*;
import com.empresa.memoria.enums.EstadoTarea;
import com.empresa.memoria.enums.Prioridad;
import com.empresa.memoria.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TareaService {
    private final List<Tarea> tareas = new ArrayList<>();
    private final List<Administrador> administradores = new ArrayList<>();
    private final List<Tecnico> tecnicos = new ArrayList<>();
    private final List<GrupoTecnicos> grupos = new ArrayList<>();
    private long siguienteId = 1;

    public TareaService() {
        administradores.add(new Administrador(1L, "Admin Demo"));
        tecnicos.add(new Tecnico(101L, "Lucía Pérez"));
        tecnicos.add(new Tecnico(102L, "Mateo Gómez"));
        grupos.add(new GrupoTecnicos(1L, "Infraestructura", List.of(101L, 102L)));
        grupos.add(new GrupoTecnicos(2L, "Aplicaciones", List.of(102L)));
    }

    public List<Tecnico> listarTecnicos() { return List.copyOf(tecnicos); }
    public List<GrupoTecnicos> listarGrupos() { return List.copyOf(grupos); }

    public synchronized Tarea crear(Long administradorId, CrearTareaDTO datos) {
        verificarAdministrador(administradorId);
        if (datos == null || textoVacio(datos.titulo()) || textoVacio(datos.descripcion()) || datos.prioridad() == null) {
            throw error(HttpStatus.BAD_REQUEST, "La tarea requiere título, descripción y prioridad");
        }

        Tarea tarea = new Tarea();
        tarea.setId(siguienteId++);
        tarea.setTitulo(datos.titulo().trim());
        tarea.setDescripcion(datos.descripcion().trim());
        tarea.setPrioridad(datos.prioridad());
        tarea.setEstado(EstadoTarea.PENDIENTE);
        tarea.setAdministradorCreadorId(administradorId);
        tarea.setFechaCreacion(LocalDateTime.now());
        actualizar(tarea, "Tarea creada", administradorId, "Estado inicial: PENDIENTE");
        tareas.add(tarea);
        return tarea;
    }

    public List<Tarea> listarTodas(Long administradorId) {
        verificarAdministrador(administradorId);
        return List.copyOf(tareas);
    }

    public Tarea buscarParaAdministrador(Long administradorId, Long tareaId) {
        verificarAdministrador(administradorId);
        return buscarTarea(tareaId);
    }

    public Tarea asignarDirectamente(Long administradorId, Long tareaId, Long tecnicoId) {
        verificarAdministrador(administradorId);
        Tarea tarea = buscarTarea(tareaId);
        verificarEditable(tarea);
        buscarTecnico(tecnicoId);

        tarea.setTecnicoAsignadoId(tecnicoId);
        tarea.setGrupoAsignadoId(null);
        tarea.setEstado(EstadoTarea.ASIGNADA);
        actualizar(tarea, "Asignación directa", administradorId, "Técnico asignado: " + nombreTecnico(tecnicoId));
        return tarea;
    }

    public Tarea derivarAGrupo(Long administradorId, Long tareaId, Long grupoId) {
        verificarAdministrador(administradorId);
        Tarea tarea = buscarTarea(tareaId);
        verificarEditable(tarea);
        GrupoTecnicos grupo = buscarGrupo(grupoId);

        tarea.setTecnicoAsignadoId(null);
        tarea.setGrupoAsignadoId(grupo.getId());
        tarea.setEstado(EstadoTarea.PENDIENTE);
        actualizar(tarea, "Derivación a grupo", administradorId, "Disponible para el grupo: " + grupo.getNombre());
        return tarea;
    }

    public List<Tarea> listarTareasDirectas(Long tecnicoId) {
        verificarTecnico(tecnicoId);
        return tareas.stream()
                .filter(t -> tecnicoId.equals(t.getTecnicoAsignadoId()) && t.getGrupoAsignadoId() == null)
                .toList();
    }

    public List<Tarea> listarTareasDeGrupos(Long tecnicoId) {
        verificarTecnico(tecnicoId);
        List<Long> gruposDelTecnico = grupos.stream()
                .filter(g -> g.contieneTecnico(tecnicoId))
                .map(GrupoTecnicos::getId)
                .toList();
        return tareas.stream().filter(t -> gruposDelTecnico.contains(t.getGrupoAsignadoId())).toList();
    }

    public List<Tarea> listarDisponiblesDeGrupo(Long tecnicoId, Long grupoId) {
        verificarTecnico(tecnicoId);
        GrupoTecnicos grupo = buscarGrupo(grupoId);
        verificarMiembro(grupo, tecnicoId);
        return tareas.stream()
                .filter(t -> grupoId.equals(t.getGrupoAsignadoId()))
                .filter(t -> t.getTecnicoAsignadoId() == null && t.getEstado() == EstadoTarea.PENDIENTE)
                .toList();
    }

    public synchronized Tarea tomarTareaGrupal(Long tecnicoId, Long tareaId) {
        Tecnico tecnico = verificarTecnico(tecnicoId);
        Tarea tarea = buscarTarea(tareaId);
        if (tarea.getGrupoAsignadoId() == null) {
            throw error(HttpStatus.BAD_REQUEST, "La tarea no fue derivada a un grupo");
        }
        GrupoTecnicos grupo = buscarGrupo(tarea.getGrupoAsignadoId());
        verificarMiembro(grupo, tecnicoId);
        if (tarea.getTecnicoAsignadoId() != null || tarea.getEstado() != EstadoTarea.PENDIENTE) {
            throw error(HttpStatus.CONFLICT, "La tarea grupal ya fue tomada o no está disponible");
        }

        tarea.setTecnicoAsignadoId(tecnico.getId());
        tarea.setEstado(EstadoTarea.ASIGNADA);
        actualizar(tarea, "Tarea grupal tomada", tecnicoId, "Responsable: " + tecnico.getNombre());
        return tarea;
    }

    public Tarea cambiarEstado(Long tecnicoId, Long tareaId, EstadoTarea nuevoEstado) {
        verificarTecnico(tecnicoId);
        Tarea tarea = buscarTarea(tareaId);
        verificarResponsable(tarea, tecnicoId);
        if (nuevoEstado != EstadoTarea.EN_PROCESO || tarea.getEstado() != EstadoTarea.ASIGNADA) {
            throw error(HttpStatus.BAD_REQUEST, "La única transición permitida aquí es ASIGNADA a EN_PROCESO");
        }
        tarea.setEstado(EstadoTarea.EN_PROCESO);
        actualizar(tarea, "Trabajo iniciado", tecnicoId, "Estado: EN_PROCESO");
        return tarea;
    }

    public Tarea registrarTrabajo(Long tecnicoId, Long tareaId, String descripcion) {
        verificarTecnico(tecnicoId);
        Tarea tarea = buscarTarea(tareaId);
        verificarResponsable(tarea, tecnicoId);
        if (tarea.getEstado() != EstadoTarea.EN_PROCESO) {
            throw error(HttpStatus.BAD_REQUEST, "El trabajo solo se registra cuando la tarea está EN_PROCESO");
        }
        if (textoVacio(descripcion)) {
            throw error(HttpStatus.BAD_REQUEST, "La descripción del trabajo no puede estar vacía");
        }
        tarea.setTrabajoRealizado(descripcion.trim());
        actualizar(tarea, "Trabajo actualizado", tecnicoId, descripcion.trim());
        return tarea;
    }

    public Tarea resolver(Long tecnicoId, Long tareaId, String resolucion) {
        verificarTecnico(tecnicoId);
        Tarea tarea = buscarTarea(tareaId);
        verificarResponsable(tarea, tecnicoId);
        if (tarea.getEstado() != EstadoTarea.EN_PROCESO) {
            throw error(HttpStatus.BAD_REQUEST, "Solo se puede resolver una tarea EN_PROCESO");
        }
        if (textoVacio(resolucion)) {
            throw error(HttpStatus.BAD_REQUEST, "La descripción de la resolución es obligatoria");
        }
        tarea.setResolucion(resolucion.trim());
        tarea.setEstado(EstadoTarea.RESUELTA);
        actualizar(tarea, "Tarea resuelta", tecnicoId, resolucion.trim());
        return tarea;
    }

    public Tarea cancelar(Long administradorId, Long tareaId) {
        verificarAdministrador(administradorId);
        Tarea tarea = buscarTarea(tareaId);
        if (tarea.getEstado() == EstadoTarea.RESUELTA || tarea.getEstado() == EstadoTarea.CANCELADA) {
            throw error(HttpStatus.BAD_REQUEST, "No se puede cancelar una tarea resuelta o ya cancelada");
        }
        tarea.setEstado(EstadoTarea.CANCELADA);
        actualizar(tarea, "Tarea cancelada", administradorId, "Cancelada por administración");
        return tarea;
    }

    private void actualizar(Tarea tarea, String accion, Long usuarioId, String detalle) {
        tarea.setFechaActualizacion(LocalDateTime.now());
        tarea.agregarHistorial(new HistorialTarea(LocalDateTime.now(), accion, usuarioId, detalle));
    }

    private Tarea buscarTarea(Long tareaId) {
        return tareas.stream().filter(t -> t.getId().equals(tareaId)).findFirst()
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe la tarea " + tareaId));
    }

    private GrupoTecnicos buscarGrupo(Long grupoId) {
        return grupos.stream().filter(g -> g.getId().equals(grupoId)).findFirst()
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe el grupo " + grupoId));
    }

    private Tecnico verificarTecnico(Long tecnicoId) {
        return tecnicos.stream().filter(t -> t.getId().equals(tecnicoId)).findFirst()
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "El usuario no es un técnico registrado"));
    }

    private Tecnico buscarTecnico(Long tecnicoId) {
        return tecnicos.stream().filter(t -> t.getId().equals(tecnicoId)).findFirst()
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe el técnico " + tecnicoId));
    }

    private void verificarAdministrador(Long administradorId) {
        boolean existe = administradores.stream().anyMatch(a -> a.getId().equals(administradorId));
        if (!existe) throw error(HttpStatus.FORBIDDEN, "La operación requiere un administrador");
    }

    private void verificarMiembro(GrupoTecnicos grupo, Long tecnicoId) {
        if (!grupo.contieneTecnico(tecnicoId)) {
            throw error(HttpStatus.FORBIDDEN, "El técnico no pertenece al grupo asignado");
        }
    }

    private void verificarResponsable(Tarea tarea, Long tecnicoId) {
        if (tarea.getTecnicoAsignadoId() == null || !tarea.getTecnicoAsignadoId().equals(tecnicoId)) {
            throw error(HttpStatus.FORBIDDEN, "El técnico no es responsable de esta tarea");
        }
    }

    private void verificarEditable(Tarea tarea) {
        if (tarea.getEstado() == EstadoTarea.EN_PROCESO || tarea.getEstado() == EstadoTarea.RESUELTA
                || tarea.getEstado() == EstadoTarea.CANCELADA) {
            throw error(HttpStatus.CONFLICT, "La tarea ya está en curso o finalizada y no puede reasignarse");
        }
    }

    private String nombreTecnico(Long tecnicoId) {
        return verificarTecnico(tecnicoId).getNombre();
    }

    private boolean textoVacio(String texto) { return texto == null || texto.isBlank(); }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
