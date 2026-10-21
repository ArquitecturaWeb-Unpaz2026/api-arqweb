package com.empresa.taskmanager.service.interfaces;

import com.empresa.taskmanager.model.Tarea;

import java.util.List;

/**
 * Contrato de la capa de negocio. El controlador depende de esta abstracción
 * y no del detalle de persistencia, aplicando separación de responsabilidades.
 */
public interface TareaService {
    /** Devuelve la colección de tareas para el endpoint GET de listado. */
    List<Tarea> listar();

    /** Busca una tarea por su clave o informa que no existe. */
    Tarea buscarPorId(Long id);

    /** Crea una tarea nueva e inicia su estado como PENDIENTE. */
    Tarea crear(Tarea tarea);

    /** Actualiza los campos editables de una tarea existente. */
    Tarea actualizar(Long id, Tarea datos);

    /** Elimina la tarea indicada si existe. */
    void eliminar(Long id);
}
