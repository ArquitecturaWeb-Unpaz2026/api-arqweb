package com.empresa.memoria.service;

import com.empresa.memoria.dto.CrearTareaDTO;
import com.empresa.memoria.enums.EstadoTarea;
import com.empresa.memoria.enums.Prioridad;
import com.empresa.memoria.model.Tarea;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class TareaServiceTest {
    private final TareaService servicio = new TareaService();

    @Test
    void asignaTareaDirectamenteAlTecnicoSeleccionado() {
        Tarea creada = servicio.crear(1L, new CrearTareaDTO("Revisar red", "Revisar switch", Prioridad.ALTA));

        Tarea asignada = servicio.asignarDirectamente(1L, creada.getId(), 101L);

        assertEquals(EstadoTarea.ASIGNADA, asignada.getEstado());
        assertEquals(101L, asignada.getTecnicoAsignadoId());
        assertNull(asignada.getGrupoAsignadoId());
    }

    @Test
    void soloElPrimerTecnicoPuedeTomarUnaTareaGrupal() {
        Tarea creada = servicio.crear(1L, new CrearTareaDTO("Revisar servidor", "Alta de equipo", Prioridad.MEDIA));
        servicio.derivarAGrupo(1L, creada.getId(), 1L);

        Tarea tomada = servicio.tomarTareaGrupal(101L, creada.getId());

        assertEquals(EstadoTarea.ASIGNADA, tomada.getEstado());
        assertEquals(101L, tomada.getTecnicoAsignadoId());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.tomarTareaGrupal(102L, creada.getId()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void transicionaYResuelveSoloUnaTareaEnProceso() {
        Tarea creada = servicio.crear(1L, new CrearTareaDTO("Revisar equipo", "No enciende", Prioridad.BAJA));
        servicio.asignarDirectamente(1L, creada.getId(), 101L);

        Tarea enProceso = servicio.cambiarEstado(101L, creada.getId(), EstadoTarea.EN_PROCESO);
        assertEquals(EstadoTarea.EN_PROCESO, enProceso.getEstado());
        servicio.registrarTrabajo(101L, creada.getId(), "Se cambió el cable de red");
        Tarea resuelta = servicio.resolver(101L, creada.getId(), "Equipo operativo");

        assertEquals(EstadoTarea.RESUELTA, resuelta.getEstado());
        assertEquals("Equipo operativo", resuelta.getResolucion());
        assertEquals("Se cambió el cable de red", resuelta.getTrabajoRealizado());
        assertTrue(resuelta.getHistorial().size() >= 4);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.cancelar(1L, creada.getId()));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void tecnicoNoPuedeModificarTareaDeOtro() {
        Tarea creada = servicio.crear(1L, new CrearTareaDTO("Revisar equipo", "No enciende", Prioridad.BAJA));
        servicio.asignarDirectamente(1L, creada.getId(), 101L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.cambiarEstado(102L, creada.getId(), EstadoTarea.EN_PROCESO));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }
}
