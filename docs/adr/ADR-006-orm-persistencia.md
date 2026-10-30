# ADR-006 — ORM y estrategia de persistencia: Spring Data JPA + Hibernate

---

## Contexto

Necesitábamos definir la estrategia de comunicación entre nuestro backend (orientado a objetos, Java) y la base de datos relacional (MySQL). Este es un problema fundamental conocido como **diferencia de impedancia objeto-relacional** (object-relational impedance mismatch).

### El problema de la impedancia

Existe un desajuste fundamental entre los objetos y las bases de datos relacionales. Este desajuste lleva a concebir los datos de manera distinta:

| Aspecto | Objetos (Java) | Tablas (SQL) |
|---|---|---|
| Modelado | Clases con estado y comportamiento | Filas y columnas estáticas |
| Relaciones | Referencias entre objetos | Claves foráneas |
| Herencia | Nativa en POO | No tiene equivalente directo |
| Identidad | Referencia en memoria | Clave primaria |
| Encapsulamiento | Atributos privados + métodos | Columnas públicas |

Esto genera incompatibilidades: la herencia no tiene equivalente en el modelo relacional, las referencias deben transformarse en claves foráneas, y el encapsulamiento se rompe al exponer atributos para las columnas.

### Estrategias para reducir la impedancia

1. **Capa de acceso a datos (DAL):** Intermediaria entre lógica de negocio y base de datos. Facilita el reemplazo del mecanismo de almacenamiento sin alterar la lógica del dominio.
2. **ORM (Object-Relational Mapping):** Convierte tablas en clases, filas en instancias y columnas en atributos. Crea una "base de datos orientada a objetos virtual" sobre la base relacional.
3. **Patrones de persistencia:** Unit of Work (coordina múltiples operaciones en una transacción), Repository (abstrae el acceso a datos).

---

## Decisión

Adoptamos **Spring Data JPA con Hibernate** como ORM y estrategia de persistencia.

### Cómo Hibernate resuelve la impedancia en nuestro proyecto

**Entidades JPA (modelo → tabla)** en `api-arqweb/src/main/java/com/empresa/taskmanager/model/`:

La entidad `Tarea.java` mapea la tabla `tareas`:
```java
@Entity
@Table(name = "tareas")
public class Tarea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "idestado")
    private Estado estado;           // FK resuelta como referencia a objeto

    @ManyToOne
    @JoinColumn(name = "idusuario")
    private Usuario usuario;         // FK resuelta como referencia a objeto (asignado)

    @ManyToOne
    @JoinColumn(name = "idcreador")
    private Usuario creador;         // FK resuelta como referencia a objeto

    private LocalDateTime fechaeliminacion;  // Soft delete (ver ADR-008)
}
```

La entidad `Usuario.java` mapea la tabla `usuarios`:
```java
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "idrol")
    private Rol rol;                 // FK resuelta como referencia a objeto

    @JsonIgnore                      // Nunca se expone en la API
    private String password;

    private LocalDateTime fechaeliminacion;  // Soft delete
}
```

Hibernate se encarga de:
- Traducir `@ManyToOne` en JOINs SQL automáticamente.
- Mapear filas del resultado a instancias de `Tarea` con sus objetos relacionados (`Estado`, `Usuario`).
- Generar INSERT/UPDATE/DELETE sin escribir SQL manualmente.

**Repositorios (Spring Data JPA)** en `api-arqweb/src/main/java/com/empresa/taskmanager/repository/`:

```java
// TareaRepository.java
public interface TareaRepository extends JpaRepository<Tarea, Long> {
    List<Tarea> findByFechaEliminacionIsNull();
    Optional<Tarea> findByIdAndFechaEliminacionIsNull(Long id);
}

// UsuarioRepository.java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    List<Usuario> findByFechaEliminacionIsNull();
    Optional<Usuario> findByIdAndFechaEliminacionIsNull(Long id);
}
```

Spring Data JPA genera la implementación SQL automáticamente a partir del nombre del método (**derived queries**). No escribimos SQL ni HQL manualmente. Por ejemplo, `findByFechaEliminacionIsNull()` se traduce en `SELECT ... FROM tareas WHERE fechaeliminacion IS NULL`.

**Unit of Work (patrón implementado por Hibernate):**

El patrón Unit of Work lleva un registro de todo lo que se hace durante una transacción de negocio que pueda afectar la base de datos. Cuando termina, determina todo lo que debe hacerse para modificar la base de datos. En Hibernate/JPA, el `EntityManager` actúa como Unit of Work:

- Rastrea automáticamente qué entidades fueron creadas, modificadas o eliminadas.
- Al hacer `commit`, detecta los cambios (dirty checking) y genera las sentencias SQL necesarias.
- A diferencia de Sequelize (Node.js) donde el desarrollador decide cuándo persistir cada objeto, en Hibernate el Unit of Work es automático. Se piensa en objetos, no en SQL, lo que reduce significativamente la impedancia.

**Control transaccional** en `api-arqweb/src/main/java/com/empresa/taskmanager/service/impl/TareaServiceImpl.java`:

```java
@Transactional
public Tarea crear(Tarea tarea, Authentication auth) {
    // Toda la lógica dentro de una transacción
    Estado estado = estadoRepo.findById(tarea.getEstado().getId())
        .orElseThrow(() -> new ResourceNotFoundException("Estado no encontrado"));
    // ... validaciones y asignaciones
    Tarea guardada = tareaRepo.save(tarea);
    entityManager.refresh(guardada);  // Obtener valores generados por la BD (timestamps)
    return guardada;
    // Si algo falla, se hace rollback automático
}
```

### Por qué JPA/Hibernate y no otra alternativa

- **vs. JDBC directo:** Escribir SQL manual para cada operación sería extremadamente verboso y propenso a errores. Para un CRUD con relaciones (tarea → estado, tarea → usuario), el ORM reduce drásticamente el código necesario.
- **vs. Sequelize (Node.js):** En Sequelize/Node uno decide cuándo persistir cada objeto; no hay un contexto que rastree automáticamente los cambios. Para nuestro proyecto con Java, Hibernate con Unit of Work automático es más natural y reduce la impedancia de forma más efectiva.

### Configuración aplicada (`api-arqweb/src/main/resources/application.yml`)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:none}       # Esquema controlado por SQL manual (seed.sql)
    open-in-view: false                # Evitar sesión JPA abierta en la vista (anti-pattern)
    properties:
      hibernate.jdbc.time_zone: UTC    # Normalizar fechas a UTC
```

- `ddl-auto: none`: No se auto-genera esquema; usamos `Deploy/seed.sql` para control total.
- `open-in-view: false`: Evita el anti-pattern de Open Session in View, forzando que las queries se resuelvan en la capa de servicio.

---

## Consecuencias

### Positivas
- Reducción drástica de la impedancia objeto-relacional: trabajamos con objetos Java (`Tarea`, `Usuario`), no con filas SQL.
- Derived queries eliminan la necesidad de escribir SQL para consultas simples.
- Unit of Work automático garantiza consistencia transaccional sin código manual.
- Independencia del motor SQL: si migráramos de MySQL a PostgreSQL, el código Java no cambiaría.
- `EntityManager.refresh()` permite obtener valores generados por la base (ej: timestamps con DEFAULT).

### Negativas
- Abstracción con costo: queries generadas por Hibernate pueden no ser óptimas (problema N+1 si no se configura fetch strategy correctamente).
- Curva de aprendizaje del ciclo de vida de entidades JPA (managed, detached, transient).
- Debugging más complejo: las excepciones de Hibernate pueden ser crípticas.
- `open-in-view: false` requiere que todas las relaciones lazy se resuelvan explícitamente en el servicio.

### Trade-off aceptado
Priorizamos **productividad, abstracción del acceso a datos y reducción de la impedancia** sobre control fino del SQL generado, lo cual es adecuado para una app de CRUD donde las consultas son simples y predecibles.
