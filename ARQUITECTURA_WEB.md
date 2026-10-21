# Relación con Arquitectura Web

En esta primera iteración organicé un backend en capas e implementé CRUD para el recurso `Tarea`. Apliqué contenidos de Arquitectura Web para mostrar cómo una solicitud viaja desde HTTP hasta PostgreSQL y cómo se separan las responsabilidades.

## Capas y responsabilidades

| Capa | Archivo | Aplicación del concepto |
|---|---|---|
| Presentación / API REST | `controller/TareaController.java` | Recibe métodos HTTP, rutas y JSON; responde con códigos de estado y JSON. No contiene consultas SQL. |
| Negocio / aplicación | `service/interfaces/TareaService.java`, `service/impl/TareaServiceImpl.java` | Define y ejecuta casos de uso. Decide el estado inicial, selecciona los campos que pueden editarse y trata la ausencia de una tarea. |
| Acceso a datos | `repository/TareaRepository.java` | Abstrae las operaciones contra la base. Spring Data JPA genera la implementación CRUD. |
| Dominio / persistencia | `model/Tarea.java`, `EstadoTarea.java`, `Prioridad.java` | Representa la entidad y las reglas/valores propios de las tareas. JPA relaciona la clase con la tabla. |
| Manejo transversal | `exception/ApiExceptionHandler.java` | Traduce errores de negocio a una respuesta HTTP uniforme. |

## Recorrido de una petición

Por ejemplo, al enviar `POST /api/tareas` con JSON:

1. Spring MVC encuentra el método del controlador asociado a `POST /api/tareas`.
2. Jackson convierte el JSON al objeto Java `Tarea` y Jakarta Validation revisa los campos obligatorios.
3. El controlador delega al contrato `TareaService`; no decide cómo persistir.
4. `TareaServiceImpl` fija `PENDIENTE` como estado inicial y llama a `TareaRepository`.
5. Spring Data JPA/Hibernate convierte la entidad en una operación SQL para PostgreSQL.
6. El controlador devuelve `201 Created`, el objeto creado y una cabecera `Location` con su URL.

## Conceptos aplicados

- **Arquitectura cliente-servidor:** un cliente envía una solicitud HTTP al backend; el servidor procesa la operación y responde.
- **REST y recursos:** las rutas se organizan alrededor de `/api/tareas`; los verbos `GET`, `POST`, `PUT` y `DELETE` expresan la operación sobre el recurso.
- **Códigos HTTP:** `200` para lecturas/ediciones correctas, `201` para alta, `204` para borrado sin cuerpo y `404` cuando el ID no existe.
- **Separación de responsabilidades:** controlador, servicio, repositorio y modelo tienen tareas diferentes y acotadas.
- **Inyección de dependencias e inversión de control:** Spring crea y conecta las clases; el servicio depende de una interfaz y el controlador de `TareaService`.
- **ORM:** JPA/Hibernate mapea objetos Java a filas y columnas; `JpaRepository` evita escribir el CRUD SQL básico.
- **Validación de entrada:** las restricciones de `Tarea` se activan en el controlador con `@Valid` antes de ejecutar el caso de uso.
- **Transacciones:** `@Transactional` delimita las operaciones de escritura para que el cambio se complete como una unidad.
- **Configuración externa:** puerto y acceso a base se pueden cambiar por variables de entorno, evitando codificar los valores de despliegue.
- **Serialización:** Spring MVC/Jackson convierte los objetos de respuesta a JSON para el cliente.

## Qué apliqué en esta entrega

Primero definí el recurso y sus datos (`Tarea`); después agregué el contrato de persistencia, la lógica CRUD y finalmente las rutas HTTP. Esta secuencia permite construir el backend desde el modelo hacia afuera y entender qué responsabilidad tiene cada componente. Usé inyección por constructor para que Spring conecte las capas y para que, más adelante, se puedan probar o reemplazar implementaciones sin acoplar el controlador a la base de datos.

## Decisiones de alcance

Esta etapa no implementa login, JWT, roles, técnicos, asignaciones ni historial. El CRUD está preparado como base para agregarlos después sin mezclar esas funciones con la primera implementación. Para un sistema expuesto en producción también se incorporarán DTOs, autorización por rol, paginación, migraciones versionadas y reglas de transición de estados.
