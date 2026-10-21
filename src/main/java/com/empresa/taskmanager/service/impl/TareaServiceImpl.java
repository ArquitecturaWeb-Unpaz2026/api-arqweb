package com.empresa.taskmanager.service.impl;

import com.empresa.taskmanager.exception.ResourceNotFoundException;
import com.empresa.taskmanager.model.EstadoTarea;
import com.empresa.taskmanager.model.Tarea;
import com.empresa.taskmanager.repository.TareaRepository;
import com.empresa.taskmanager.service.interfaces.TareaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación de reglas básicas del caso de uso CRUD.
 * Spring registra la clase como servicio y la transacción agrupa cada operación
 * con la base para mantener consistencia durante la persistencia.
 */
@Service
@Transactional
public class TareaServiceImpl implements TareaService {

    /** Repositorio inyectado por constructor (Inversión de Control de Spring). */
    private final TareaRepository tareaRepository;

    /** Spring entrega la implementación JPA del repositorio al crear el servicio. */
    public TareaServiceImpl(TareaRepository tareaRepository) {
        this.tareaRepository = tareaRepository;
    }

    /** Consulta todos los registros mediante la abstracción Spring Data JPA. */
    @Override
    @Transactional(readOnly = true)
    public List<Tarea> listar() {
        return tareaRepository.findAll();
    }

    /** Consulta por ID y transforma la ausencia del registro en un error de dominio. */
    @Override
    @Transactional(readOnly = true)
    public Tarea buscarPorId(Long id) {
        return tareaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la tarea con id " + id));
    }

    /** Aplica el caso de uso de alta y fija el estado inicial en el servidor. */
    @Override
    public Tarea crear(Tarea tarea) {
        // Evita que el cliente elija una clave existente o fuerce otro estado inicial.
        tarea.setId(null);
        tarea.setEstado(EstadoTarea.PENDIENTE);
        // save delega INSERT/UPDATE a JPA según el identificador de la entidad.
        return tareaRepository.save(tarea);
    }

    /** Copia solo los campos editables para proteger ID, estado y fechas del cliente. */
    @Override
    public Tarea actualizar(Long id, Tarea datos) {
        Tarea existente = buscarPorId(id);
        existente.setTitulo(datos.getTitulo());
        existente.setDescripcion(datos.getDescripcion());
        existente.setPrioridad(datos.getPrioridad());
        return tareaRepository.save(existente);
    }

    /** Confirma existencia antes de borrar para devolver 404 si el ID no existe. */
    @Override
    public void eliminar(Long id) {
        Tarea existente = buscarPorId(id);
        tareaRepository.delete(existente);
    }
}
