# ADR-003 — Base de datos: MySQL 8.4 (SQL relacional)

---

## Contexto

Necesitábamos seleccionar el motor de base de datos para nuestra aplicación de gestión de tareas interna. La selección de la base de datos es una decisión arquitectónica clave que impacta directamente en la eficiencia y sostenibilidad del sistema.

### Naturaleza de los datos de nuestro proyecto

- **Tareas:** Tienen título, descripción, estado, usuario asignado, creador, fechas de creación/inicio/vencimiento/eliminación. Relaciones claras con otras entidades.
- **Usuarios:** Nombre, email, contraseña hasheada, rol, fechas de creación/eliminación. Relación con tareas (como asignado y como creador) y con roles.
- **Estados:** Catálogo fijo (PENDIENTE, ASIGNADA, EN_PROCESO, RESUELTA, CANCELADA).
- **Roles:** Catálogo fijo (ADMIN, TECNICO).

Los datos son **altamente estructurados**, con **relaciones claras** entre entidades y **necesidad de consistencia transaccional** (ej: no se puede asignar una tarea a un usuario inexistente).

### SQL vs. NoSQL: criterios de evaluación

Los criterios principales para elegir entre SQL y NoSQL son:

| Criterio | Nuestro caso | Favorece |
|---|---|---|
| Estructura del dato | Estructurado, esquema rígido | SQL |
| Requisitos de consistencia | ACID (integridad de asignaciones, estados) | SQL |
| Necesidad de escalabilidad | Baja-media (app interna) | SQL |
| Complejidad de consultas | Consultas con JOINs entre tareas, usuarios, estados | SQL |
| Volumen de lecturas/escrituras | Bajo-medio | SQL |

### ACID vs. BASE

| Propiedad | ACID (SQL) | BASE (NoSQL) |
|---|---|---|
| **Atomicidad** | La transacción se ejecuta completa o no se ejecuta. Ej: si se asigna una tarea, o se completa todo o no pasa nada. | No garantizada. |
| **Consistencia** | La base siempre pasa de un estado válido a otro. Ej: un campo no puede violar constraints. | Eventualmente consistente. |
| **Aislamiento** | Las transacciones no se interfieren entre sí. Ej: si dos usuarios editan tareas al mismo tiempo, no se pisan. | Puede haber inconsistencias temporales. |
| **Durabilidad** | Una vez confirmada, la transacción se guarda permanentemente. Ej: si se corta la luz, el dato queda guardado. | Garantizada. |

Para gestión de tareas necesitamos **ACID**: si se asigna una tarea a un técnico, esa operación debe ser atómica y consistente. No podemos aceptar que una tarea quede en un estado inválido.

### Comparación de motores evaluados

| Motor | Modelo | Consistencia | Escalabilidad | Rendimiento óptimo | Casos de uso típicos |
|---|---|---|---|---|---|
| **MySQL** | Relacional | ACID (InnoDB) | Vertical / Réplicas maestro-esclavo | Lecturas rápidas, cargas ligeras | Apps web, CMS, SaaS simples |
| PostgreSQL | Relacional | ACID (fuerte) | Vertical, con replicación | Consultas complejas | ERP, sistemas financieros |
| MongoDB | Documental (JSON/BSON) | BASE (eventual) | Horizontal (sharding nativo) | Escrituras concurrentes, escalabilidad | Redes sociales, catálogos de productos |
| Redis | Clave-valor | Eventual | Horizontal (cluster) | Acceso rápido en memoria | Caché, sesiones, ranking |

---

## Decisión

Adoptamos **MySQL 8.4** como motor de base de datos relacional con motor de almacenamiento **InnoDB**.

### Esquema de la base de datos

El esquema se define en `Deploy/seed.sql` y se carga manualmente en la primera ejecución:

```
roles (id, nombre)                         -- Valores: 1=ADMIN, 2=TECNICO
usuarios (id, nombre, email, password,     -- FK → roles (idrol)
          idrol, fechacreacion,
          fechaeliminacion)                -- Soft delete (ver ADR-008)
estados (id, nombre)                       -- Valores: 1=PENDIENTE, 2=ASIGNADA, 3=EN_PROCESO, 4=RESUELTA, 5=CANCELADA
tareas (id, titulo, descripcion,           -- FK → estados (idestado)
        idestado, idusuario, idcreador,    -- FK → usuarios (idusuario = asignado, idcreador = creador)
        fechacreacion, fechainicio,
        fechavencimiento,
        fechaeliminacion)                  -- Soft delete (ver ADR-008)
```

La columna `fechaeliminacion` en `usuarios` se agregó mediante la migración `api-arqweb/sql/usuarios-fechaeliminacion.sql` para extender el soft delete también a usuarios.

### Datos semilla (`Deploy/seed.sql`)

| Tabla | Datos iniciales |
|---|---|
| **roles** | ADMIN (id=1), TECNICO (id=2) |
| **estados** | PENDIENTE (1), ASIGNADA (2), EN_PROCESO (3), RESUELTA (4), CANCELADA (5) |
| **usuarios** | Ana Admin (admin@arqweb.local, ADMIN), Lucía Pérez (lucia@arqweb.local, TECNICO), Mateo Gómez (mateo@arqweb.local, TECNICO). Password: `Arqweb2026` hasheado con BCrypt. |
| **tareas** | 5 tareas de ejemplo con distintos estados y asignaciones |

### Configuración aplicada (`api-arqweb/src/main/resources/application.yml`)

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/arqweb}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:none}       # Esquema pre-existente, Hibernate no crea ni altera tablas
    open-in-view: false                # Evita mantener sesión JPA abierta durante toda la respuesta
    properties:
      hibernate.jdbc.time_zone: UTC    # Normaliza fechas a UTC en la persistencia
```

### Por qué MySQL y no otra alternativa

- **vs. PostgreSQL:** Ambos son excelentes para nuestro caso. Elegimos MySQL por su simplicidad de configuración, menor consumo de recursos para cargas ligeras, y la amplia disponibilidad de imágenes Docker oficiales. Para una app de CRUD con consultas simples (derived queries de JPA), MySQL es más que suficiente.
- **vs. MongoDB:** Nuestros datos son estructurados con relaciones fuertes (tarea → usuario → rol, tarea → estado). MongoDB es ideal para datos semiestructurados con esquema flexible y escalabilidad horizontal. Nosotros necesitamos ACID, JOINs y foreign keys, por lo que MongoDB no aplica.
- **vs. Redis:** Redis es un almacén en memoria para caché, sesiones o ranking. No es un sistema de persistencia primaria.

### Por qué esquema pre-existente (ddl-auto: none)

Decidimos definir el esquema mediante el script SQL `Deploy/seed.sql` en lugar de dejar que Hibernate auto-genere las tablas. Esto nos da control total sobre la estructura, tipos de datos, constraints e índices. Es la práctica recomendada en producción para evitar cambios inesperados en el esquema.

---

## Consecuencias

### Positivas
- Integridad referencial garantizada por foreign keys (tarea → usuario, tarea → estado, usuario → rol).
- Transacciones ACID aseguran que las operaciones de negocio sean atómicas (ej: `@Transactional` en `TareaServiceImpl`).
- Ecosistema maduro: herramientas de administración, monitoreo, backup ampliamente disponibles.
- Compatibilidad nativa con Spring Data JPA/Hibernate.
- Normalización de zona horaria a UTC evita problemas de consistencia temporal.

### Negativas
- Escalabilidad horizontal limitada comparada con NoSQL (no aplica para nuestro volumen de datos).
- Cambios en el esquema requieren migraciones manuales (como `sql/usuarios-fechaeliminacion.sql`).
- Dependencia de un solo motor: si quisiéramos cambiar a PostgreSQL, habría que revisar queries específicas de MySQL (mínimo en nuestro caso por el uso de JPA).

### Trade-off aceptado
Priorizamos **consistencia, integridad de datos y relaciones estructuradas** sobre escalabilidad horizontal y flexibilidad de esquema, lo cual es la decisión correcta para un dominio de gestión de tareas con entidades claramente relacionadas.
