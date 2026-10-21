package com.empresa.memoria.controller;

import com.empresa.memoria.model.GrupoTecnicos;
import com.empresa.memoria.model.Tarea;
import com.empresa.memoria.model.Tecnico;
import com.empresa.memoria.service.TareaService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogoController {
    private final TareaService servicio;

    public CatalogoController(TareaService servicio) { this.servicio = servicio; }

    @GetMapping("/tecnicos")
    public List<Tecnico> listarTecnicos() { return servicio.listarTecnicos(); }

    @GetMapping("/grupos")
    public List<GrupoTecnicos> listarGrupos() { return servicio.listarGrupos(); }

    @GetMapping("/tecnicos/{tecnicoId}/tareas")
    public List<Tarea> listarTareasDirectas(@RequestHeader("X-Usuario-Id") Long usuarioId,
                                            @PathVariable Long tecnicoId) {
        validarIdentidad(usuarioId, tecnicoId);
        return servicio.listarTareasDirectas(tecnicoId);
    }

    @GetMapping("/tecnicos/{tecnicoId}/tareas-grupales")
    public List<Tarea> listarTareasDeGrupos(@RequestHeader("X-Usuario-Id") Long usuarioId,
                                            @PathVariable Long tecnicoId) {
        validarIdentidad(usuarioId, tecnicoId);
        return servicio.listarTareasDeGrupos(tecnicoId);
    }

    @GetMapping("/grupos/{grupoId}/tareas-disponibles")
    public List<Tarea> listarDisponibles(@RequestHeader("X-Usuario-Id") Long tecnicoId,
                                         @PathVariable Long grupoId) {
        return servicio.listarDisponiblesDeGrupo(tecnicoId, grupoId);
    }

    private void validarIdentidad(Long usuarioId, Long tecnicoId) {
        if (!usuarioId.equals(tecnicoId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "El encabezado X-Usuario-Id debe corresponder al técnico consultado");
        }
    }
}
