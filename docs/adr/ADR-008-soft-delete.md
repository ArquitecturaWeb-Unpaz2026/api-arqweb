# ADR-008 — Estrategia de eliminación: Soft Delete (borrado lógico)

---

## Contexto

Necesitábamos definir cómo se manejan las eliminaciones en el sistema. Las dos entidades principales que se eliminan son **tareas** y **usuarios**. Existen dos enfoques principales:

| Enfoque | Descripción | Dato en BD |
|---|---|---|
| **Hard Delete** | Se elimina físicamente la fila de la tabla. `DELETE FROM tareas WHERE id = ?` | El registro desaparece permanentemente |
| **Soft Delete** | Se marca la fila como eliminada sin borrarla. `UPDATE tareas SET fechaeliminacion = NOW() WHERE id = ?` | El registro permanece pero con una marca temporal |

### Consideraciones del dominio

- Las tareas tienen relaciones con usuarios (asignado, creador) y estados. Eliminar físicamente una tarea podría dejar huérfanas referencias en el sistema.
- Los usuarios tienen relaciones con tareas (como asignado y como creador). Eliminar físicamente un usuario rompería la integridad referencial de todas sus tareas.
- Puede ser necesario auditar qué tareas y usuarios existieron, quién los creó y cuándo se eliminaron.
- La eliminación accidental debería poder revertirse.
- Las consultas normales no deben mostrar registros eliminados.

---

## Decisión

Adoptamos **Soft Delete** mediante la columna `fechaeliminacion` en las tablas `tareas` y `usuarios`. Un registro se considera eliminado cuando `fechaeliminacion IS NOT NULL`.

### Implementación en tareas

**Modelo JPA** (`api-arqweb/src/main/java/com/empresa/taskmanager/model/Tarea.java`):
```java
@Entity
@Table(name = "tareas")
public class Tarea {
    // ... otros campos
    private LocalDateTime fechaeliminacion;  // null = activa, not null = eliminada
}
```

**Repositorio** (`api-arqweb/src/main/java/com/empresa/taskmanager/repository/TareaRepository.java`):
```java
public interface TareaRepository extends JpaRepository<Tarea, Long> {
    List<Tarea> findByFechaEliminacionIsNull();                    // Solo tareas activas
    Optional<Tarea> findByIdAndFechaEliminacionIsNull(Long id);    // Tarea activa por ID
}
```

**Servicio** (`api-arqweb/src/main/java/com/empresa/taskmanager/service/impl/TareaServiceImpl.java`):
```java
@Transactional
public void eliminar(Long id, Authentication auth) {
    Tarea tarea = tareaRepo.findByIdAndFechaEliminacionIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));
    tarea.setFechaeliminacion(LocalDateTime.now());
    tareaRepo.save(tarea);
}
```

**Endpoint REST** (`controller/TareaController.java`):
```
DELETE /api/tareas/{id}  →  204 No Content  (solo ADMIN, según SecurityConfig)
```

### Implementación en usuarios

La columna `fechaeliminacion` fue agregada a la tabla `usuarios` mediante la migración `api-arqweb/sql/usuarios-fechaeliminacion.sql`.

**Modelo JPA** (`api-arqweb/src/main/java/com/empresa/taskmanager/model/Usuario.java`):
```java
@Entity
@Table(name = "usuarios")
public class Usuario {
    // ... otros campos
    private LocalDateTime fechaeliminacion;  // null = activo, not null = eliminado
}
```

**Repositorio** (`api-arqweb/src/main/java/com/empresa/taskmanager/repository/UsuarioRepository.java`):
```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findByFechaEliminacionIsNull();                    // Solo usuarios activos
    Optional<Usuario> findByIdAndFechaEliminacionIsNull(Long id);    // Usuario activo por ID
    // ...
}
```

**Servicio** (`api-arqweb/src/main/java/com/empresa/taskmanager/service/UsuarioService.java`):
- Marca al usuario como eliminado con `fechaeliminacion = now()`.
- Impide que un administrador se elimine a sí mismo (protección contra quedarse sin admins).

**Servicio de seguridad** (`security/UsuarioDetailsService.java`):
- Solo carga usuarios activos (`findByEmail` + verificación de `fechaeliminacion`), por lo que un usuario eliminado no puede autenticarse.

**Endpoint REST** (`controller/UsuarioController.java`):
```
DELETE /api/usuarios/{id}  →  204 No Content  (solo ADMIN)
```

**Frontend** (`features/members/views/MembersView.vue`):
- El botón de eliminar está deshabilitado para el usuario logueado (no se puede eliminar a sí mismo).
- La lista muestra solo usuarios activos.

### Por qué Soft Delete y no Hard Delete

| Criterio | Hard Delete | Soft Delete |
|---|---|---|
| Auditoría | Se pierde el registro | Se preserva quién creó, cuándo y cuándo se eliminó |
| Reversibilidad | Irreversible (salvo backup) | Reversible: basta con poner `fechaeliminacion = NULL` |
| Integridad referencial | Puede romper FK si hay tablas dependientes | No rompe FK: la fila sigue existiendo |
| Complejidad de queries | Simple (`SELECT * FROM tareas`) | Requiere filtro (`WHERE fechaeliminacion IS NULL`) |
| Espacio en disco | Se libera | Se acumula (aceptable para volumen bajo) |

### Ventajas concretas para nuestro proyecto

1. **Auditoría:** Se sabe exactamente cuándo se eliminó cada tarea o usuario (`fechaeliminacion` registra el timestamp).
2. **Seguridad ante errores:** Si un administrador elimina un usuario o tarea por error, se puede restaurar directamente en la BD.
3. **Integridad de datos:** No se rompen relaciones. Un usuario eliminado sigue referenciado por las tareas que creó o tenía asignadas, pero ya no aparece en las listas ni puede autenticarse.
4. **Implementación consistente:** El mismo patrón (derived queries con `findByFechaEliminacionIsNull`) se aplica tanto a tareas como a usuarios.

---

## Consecuencias

### Positivas
- Trazabilidad completa: se preserva el historial de registros eliminados.
- Posibilidad de implementar "papelera" o restauración en el futuro.
- No se afectan reportes históricos ni relaciones con otros registros.
- Los endpoints mantienen la semántica REST estándar (DELETE → 204).
- Un usuario eliminado no puede autenticarse (UsuarioDetailsService filtra por activos).

### Negativas
- Las queries deben incluir siempre el filtro `WHERE fechaeliminacion IS NULL`. Si se olvida, se muestran registros eliminados (mitigado por el uso de métodos de repositorio específicos como `findByFechaEliminacionIsNull`).
- Las tablas crecen indefinidamente (mitigable con políticas de purgado periódico en el futuro).
- La validación de unicidad de email en `UsuarioRepository` (`existsByEmail`) debe considerar si el email pertenece a un usuario eliminado o activo.

### Trade-off aceptado
Priorizamos **preservación de datos, auditoría y reversibilidad** sobre simplicidad de queries y eficiencia de almacenamiento, lo cual es adecuado para un sistema de gestión donde los datos tienen valor histórico.
