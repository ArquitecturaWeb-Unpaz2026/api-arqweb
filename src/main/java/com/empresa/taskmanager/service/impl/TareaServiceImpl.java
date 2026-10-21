package com.empresa.taskmanager.service.impl;

import com.empresa.taskmanager.exception.ResourceNotFoundException;
import com.empresa.taskmanager.model.Estado;
import com.empresa.taskmanager.model.Tarea;
import com.empresa.taskmanager.model.Usuario;
import com.empresa.taskmanager.repository.EstadoRepository;
import com.empresa.taskmanager.repository.TareaRepository;
import com.empresa.taskmanager.repository.UsuarioRepository;
import com.empresa.taskmanager.service.interfaces.TareaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final EstadoRepository estadoRepository;
    private final UsuarioRepository usuarioRepository;

    /** Spring entrega las implementaciones JPA de los repositorios al crear el servicio. */
    public TareaServiceImpl(
            TareaRepository tareaRepository,
            EstadoRepository estadoRepository,
            UsuarioRepository usuarioRepository) {
        this.tareaRepository = tareaRepository;
        this.estadoRepository = estadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Consulta las tareas vigentes mediante la abstracción Spring Data JPA. */
    @Override
    @Transactional(readOnly = true)
    public List<Tarea> listar() {
        return tareaRepository.findByFechaEliminacionIsNull();
    }

    /** Consulta por ID y transforma la ausencia del registro en un error de dominio. */
    @Override
    @Transactional(readOnly = true)
    public Tarea buscarPorId(Long id) {
        return tareaRepository.findByIdAndFechaEliminacionIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la tarea con id " + id));
    }

    /** Aplica el alta usando las claves de estado y creador que ya existen en MySQL. */
    @Override
    public Tarea crear(Tarea tarea) {
        // Evita que el cliente elija una clave existente o marque la tarea como eliminada.
        tarea.setId(null);
        tarea.setFechaEliminacion(null);
        tarea.setEstado(estadoExistente(tarea.getEstado()));
        tarea.setCreador(usuarioObligatorio(tarea.getCreador(), "idcreador"));
        tarea.setUsuario(usuarioOpcional(tarea.getUsuario()));
        return tareaRepository.save(tarea);
    }

    /** Copia solo los campos editables para proteger ID, creador y fecha de alta. */
    @Override
    public Tarea actualizar(Long id, Tarea datos) {
        Tarea existente = buscarPorId(id);
        existente.setTitulo(datos.getTitulo());
        existente.setDescripcion(datos.getDescripcion());
        if (datos.getEstado() != null) {
            existente.setEstado(estadoExistente(datos.getEstado()));
        }
        existente.setUsuario(usuarioOpcional(datos.getUsuario()));
        existente.setFechaInicio(datos.getFechaInicio());
        existente.setFechaVencimiento(datos.getFechaVencimiento());
        return tareaRepository.save(existente);
    }

    /**
     * Baja lógica: completa fechaeliminacion, que es la columna prevista
     * para dejar la fila en la tabla sin borrarla.
     */
    @Override
    public void eliminar(Long id) {
        Tarea existente = buscarPorId(id);
        existente.setFechaEliminacion(Instant.now());
        tareaRepository.save(existente);
    }

    /** Resuelve idestado contra la tabla estados. */
    private Estado estadoExistente(Estado referencia) {
        if (referencia == null || referencia.getId() == null) {
            throw new IllegalArgumentException("La tarea requiere el id de un estado existente");
        }
        return estadoRepository.findById(referencia.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el estado con id " + referencia.getId()));
    }

    /** Resuelve idcreador contra la tabla usuarios. */
    private Usuario usuarioObligatorio(Usuario referencia, String columna) {
        if (referencia == null || referencia.getId() == null) {
            throw new IllegalArgumentException("La tarea requiere el id de un usuario en " + columna);
        }
        return usuarioRepository.findById(referencia.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario con id " + referencia.getId()));
    }

    /** Resuelve idusuario. Si no viene informado, la columna queda en NULL. */
    private Usuario usuarioOpcional(Usuario referencia) {
        if (referencia == null || referencia.getId() == null) {
            return null;
        }
        return usuarioRepository.findById(referencia.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario con id " + referencia.getId()));
    }
}
