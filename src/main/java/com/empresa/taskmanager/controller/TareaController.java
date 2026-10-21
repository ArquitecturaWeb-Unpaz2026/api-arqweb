package com.empresa.taskmanager.controller;

import com.empresa.taskmanager.model.Tarea;
import com.empresa.taskmanager.service.interfaces.TareaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Adaptador HTTP de la API REST. Convierte solicitudes y respuestas JSON en
 * llamadas al servicio; así la capa web no contiene reglas de persistencia.
 */
@RestController
@RequestMapping("/api/tareas")
public class TareaController {

    /** Contrato de negocio inyectado por Spring (inyección de dependencias). */
    private final TareaService tareaService;

    /** Constructor usado por Spring para conectar el controlador con el servicio. */
    public TareaController(TareaService tareaService) {
        this.tareaService = tareaService;
    }

    /** GET /api/tareas: lectura REST de la colección, respondida con HTTP 200. */
    @GetMapping
    public ResponseEntity<List<Tarea>> listar() {
        return ResponseEntity.ok(tareaService.listar());
    }

    /** GET /api/tareas/{id}: el ID viaja como variable de ruta del recurso. */
    @GetMapping("/{id}")
    public ResponseEntity<Tarea> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tareaService.buscarPorId(id));
    }

    /**
     * POST /api/tareas: crea el recurso desde JSON validado y devuelve 201
     * Created más la cabecera Location con la URL del nuevo recurso.
     */
    @PostMapping
    public ResponseEntity<Tarea> crear(@Valid @RequestBody Tarea tarea) {
        Tarea creada = tareaService.crear(tarea);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.getId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    /** PUT /api/tareas/{id}: reemplaza los campos editables del recurso indicado. */
    @PutMapping("/{id}")
    public ResponseEntity<Tarea> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Tarea tarea) {
        return ResponseEntity.ok(tareaService.actualizar(id, tarea));
    }

    /** DELETE /api/tareas/{id}: elimina el recurso y responde 204 No Content. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tareaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
