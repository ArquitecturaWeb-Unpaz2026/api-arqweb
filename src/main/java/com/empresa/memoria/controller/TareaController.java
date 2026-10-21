package com.empresa.memoria.controller;

import com.empresa.memoria.dto.*;
import com.empresa.memoria.model.Tarea;
import com.empresa.memoria.service.TareaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tareas")
public class TareaController {
    private final TareaService servicio;

    public TareaController(TareaService servicio) { this.servicio = servicio; }

    @PostMapping
    public ResponseEntity<Tarea> crear(@RequestHeader("X-Usuario-Id") Long administradorId,
                                       @RequestBody CrearTareaDTO datos) {
        Tarea creada = servicio.crear(administradorId, datos);
        return ResponseEntity.created(URI.create("/api/tareas/" + creada.getId())).body(creada);
    }

    @GetMapping
    public List<Tarea> listarTodas(@RequestHeader("X-Usuario-Id") Long administradorId) {
        return servicio.listarTodas(administradorId);
    }

    @GetMapping("/{tareaId}")
    public Tarea buscar(@RequestHeader("X-Usuario-Id") Long administradorId,
                        @PathVariable Long tareaId) {
        return servicio.buscarParaAdministrador(administradorId, tareaId);
    }

    @PutMapping("/{tareaId}/asignacion")
    public Tarea asignar(@RequestHeader("X-Usuario-Id") Long administradorId,
                         @PathVariable Long tareaId, @RequestBody AsignarTecnicoDTO datos) {
        return servicio.asignarDirectamente(administradorId, tareaId, datos.tecnicoId());
    }

    @PutMapping("/{tareaId}/derivacion")
    public Tarea derivar(@RequestHeader("X-Usuario-Id") Long administradorId,
                         @PathVariable Long tareaId, @RequestBody DerivarGrupoDTO datos) {
        return servicio.derivarAGrupo(administradorId, tareaId, datos.grupoId());
    }

    @PostMapping("/{tareaId}/tomar")
    public Tarea tomar(@RequestHeader("X-Usuario-Id") Long tecnicoId, @PathVariable Long tareaId) {
        return servicio.tomarTareaGrupal(tecnicoId, tareaId);
    }

    @PutMapping("/{tareaId}/estado")
    public Tarea cambiarEstado(@RequestHeader("X-Usuario-Id") Long tecnicoId,
                               @PathVariable Long tareaId, @RequestBody CambiarEstadoDTO datos) {
        return servicio.cambiarEstado(tecnicoId, tareaId, datos.estado());
    }

    @PutMapping("/{tareaId}/trabajo")
    public Tarea registrarTrabajo(@RequestHeader("X-Usuario-Id") Long tecnicoId,
                                  @PathVariable Long tareaId, @RequestBody TextoTareaDTO datos) {
        return servicio.registrarTrabajo(tecnicoId, tareaId, datos.descripcion());
    }

    @PostMapping("/{tareaId}/resolver")
    public Tarea resolver(@RequestHeader("X-Usuario-Id") Long tecnicoId,
                          @PathVariable Long tareaId, @RequestBody TextoTareaDTO datos) {
        return servicio.resolver(tecnicoId, tareaId, datos.descripcion());
    }

    @PostMapping("/{tareaId}/cancelar")
    public Tarea cancelar(@RequestHeader("X-Usuario-Id") Long administradorId, @PathVariable Long tareaId) {
        return servicio.cancelar(administradorId, tareaId);
    }
}
