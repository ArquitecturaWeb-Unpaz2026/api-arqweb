# ADR-009 — Comunicación frontend-backend: API REST

---

## Contexto

Necesitábamos definir el mecanismo de comunicación entre el frontend (Vue 3 SPA) y el backend (Spring Boot). Las APIs son fundamentales en el diseño e implementación de backends escalables, ya que vinculan diferentes servicios y partes de un mismo sistema.

REST (Representational State Transfer) es un estilo arquitectónico para diseñar servicios web que se basa en el uso de los recursos del sistema mediante peticiones HTTP. Fue introducido por Roy Fielding en su tesis doctoral en el año 2000.

### Alternativas evaluadas

| Protocolo | Enfoque | Mejor para | Complejidad |
|---|---|---|---|
| **REST** | Recursos + verbos HTTP + JSON | CRUD, APIs públicas, ecosistemas heterogéneos | Baja-media |
| **GraphQL** | Consultas flexibles, schema tipado | Apps con datos anidados y múltiples consumidores con necesidades distintas | Media-alta |
| **gRPC** | RPC binario sobre HTTP/2 con Protocol Buffers | Comunicación inter-servicios de alto rendimiento | Alta |
| **WebSocket** | Canal bidireccional persistente | Chat, notificaciones en tiempo real, streaming | Media |

---

## Decisión

Adoptamos **API REST** siguiendo las restricciones y buenas prácticas de diseño vistas en la cátedra.

### Cumplimiento de las restricciones REST

| Restricción | Implementación en nuestro proyecto |
|---|---|
| **Cliente-servidor** | Frontend (`client-arqweb/`) y backend (`api-arqweb/`) están completamente separados. El cliente solicita información y el servidor la proporciona. |
| **Sin estado (Stateless)** | Cada petición incluye el token JWT en el header `Authorization`. El servidor no almacena sesiones (`SecurityConfig.java`: `SessionCreationPolicy.STATELESS`). |
| **Cacheable** | Las respuestas pueden cachearse en el navegador. Los assets estáticos del frontend se cachean via Nginx. |
| **Interfaz uniforme** | Recursos identificados por URIs (`/api/tareas`, `/api/usuarios`), operaciones expresadas por verbos HTTP (GET, POST, PUT, DELETE). |
| **Sistemas en capas** | Caddy (reverse proxy + TLS) → Nginx (frontend) / Spring Boot (backend) → MySQL. Cada capa se comunica solo con la adyacente. |

### Diseño de endpoints

```
Autenticación (público):
POST   /api/auth/registro            → Registrar usuario (AuthController.java)
POST   /api/auth/login               → Obtener JWT (AuthController.java)

Tareas (autenticado):
GET    /api/tareas                    → Listar tareas activas (TareaController.java)
GET    /api/tareas/{id}               → Obtener detalle de una tarea
POST   /api/tareas                    → Crear nueva tarea (201 Created + Location header)
PUT    /api/tareas/{id}               → Actualizar tarea existente
DELETE /api/tareas/{id}               → Soft delete de tarea (solo ADMIN, 204 No Content)

Usuarios (solo ADMIN):
GET    /api/usuarios                  → Listar usuarios activos (UsuarioController.java)
POST   /api/usuarios                  → Crear nuevo usuario (201 Created)
PUT    /api/usuarios/{id}             → Actualizar usuario
DELETE /api/usuarios/{id}             → Soft delete de usuario (204 No Content)
```

**Reglas de diseño aplicadas:**
- Sustantivos en plural para recursos (`/tareas`, `/usuarios`, no `/tarea`).
- Verbos HTTP expresan la operación (no se usan nombres CRUD en la URI). Por ejemplo, `DELETE /users/1234` es correcto; `GET /deleteUser/1234` es un anti-patrón.
- Separador `/` para jerarquía; sin barra diagonal al final.
- Letras minúsculas en las URIs.
- Sin extensiones de archivo en las URIs.

### Códigos de estado HTTP utilizados

| Código | Significado | Uso en nuestro proyecto |
|---|---|---|
| `200 OK` | Lectura/edición correcta | GET (listar/detalle), PUT (actualización) |
| `201 Created` | Recurso creado | POST (crear tarea, registrar/crear usuario) |
| `204 No Content` | Operación exitosa sin cuerpo | DELETE (soft delete) |
| `400 Bad Request` | Validación fallida | Campos obligatorios faltantes, email duplicado |
| `401 Unauthorized` | Token inválido o ausente | Acceso sin JWT a ruta protegida |
| `403 Forbidden` | Autenticado pero sin permisos | TECNICO intenta acceder a `/api/usuarios` o eliminar tarea |
| `404 Not Found` | Recurso inexistente | Tarea/usuario no encontrado |

### Idempotencia

La idempotencia nos dice que la ejecución repetida de una petición con los mismos parámetros sobre un mismo recurso tendrá el mismo efecto:

| Método | Idempotente | Comportamiento en nuestro proyecto |
|---|---|---|
| GET | Sí | `GET /api/tareas` siempre devuelve la misma lista (si no hay cambios) |
| PUT | Sí | `PUT /api/tareas/1` con los mismos datos produce el mismo resultado |
| DELETE | Sí | `DELETE /api/tareas/1` marca como eliminada; repetirlo da 404 (ya eliminada) |
| POST | No | `POST /api/tareas` crea una nueva tarea cada vez |

### Documentación con OpenAPI/Swagger (enfoque code-first)

Existen dos enfoques: Design-First (definición manual antes del código) y Code-First (generación automática a partir del código). Nuestro proyecto usa **Code-First** con SpringDoc (`springdoc-openapi-starter-webmvc-ui 2.6.0`):

- Las anotaciones de Spring MVC (`@GetMapping`, `@PostMapping`, etc.) generan la especificación OpenAPI automáticamente.
- `OpenApiConfig.java` configura el esquema de seguridad Bearer para que Swagger UI tenga el botón "Authorize" y se pueda probar con JWT.
- Producción: `https://api.arqweb.aulas-unpaz.cloud/swagger-ui/index.html`

Swagger UI funciona como una plataforma interactiva donde los consumidores de la API pueden navegar endpoints, ver ejemplos y ejecutar pruebas en tiempo real ("Try it out").

### Cliente HTTP en el frontend (`client-arqweb/src/shared/api/client.ts`)

```typescript
api.get<T>(path, options)       // GET
api.post<T>(path, body, opts)   // POST
api.put<T>(path, body, opts)    // PUT
api.delete<T>(path, options)    // DELETE
```

Características:
- Wrapper sobre `fetch()` nativo del navegador.
- Agrega automáticamente `Content-Type: application/json`.
- Agrega `Authorization: Bearer <token>` si el usuario está autenticado (token desde localStorage).
- Parsea errores y lanza `ApiError` con status y body para mostrar mensajes al usuario.
- `config.ts` permite alternar entre API real y mock (`VITE_USE_MOCK_API`).

**Proxy en desarrollo (`client-arqweb/vite.config.ts`):**
```typescript
server: {
  proxy: {
    '/api': {
      target: env.VITE_API_PROXY_TARGET ?? 'http://localhost:8081',
      changeOrigin: true,
    },
  },
}
```

**Proxy en producción (`client-arqweb/nginx/default.conf`):**
Nginx proxea las peticiones `/api` al contenedor del backend (`arqweb_api:8081`), evitando problemas de CORS.

### Por qué REST y no otra alternativa

- **vs. GraphQL:** GraphQL permite consultas flexibles donde el cliente pide exactamente los campos que necesita. Es ideal cuando hay múltiples consumidores con necesidades distintas. En nuestro caso, hay un solo consumidor (la SPA Vue) y los datos son simples (CRUD de tareas y usuarios). REST con endpoints bien definidos es más simple y directo.
- **vs. gRPC:** gRPC usa Protocol Buffers (formato binario) y HTTP/2 para comunicación de alto rendimiento entre servicios. Es ideal para microservicios que se comunican entre sí. Para comunicación browser→servidor, REST con JSON es el estándar soportado nativamente por `fetch()`.
- **vs. WebSocket:** WebSocket sería necesario si los usuarios necesitaran ver actualizaciones de tareas en tiempo real (ej: un tablero Kanban colaborativo). Para nuestro caso, las operaciones CRUD con solicitud-respuesta estándar son suficientes.

---

## Consecuencias

### Positivas
- Interfaz uniforme y predecible basada en estándares HTTP.
- Documentación interactiva auto-generada con Swagger UI.
- Cualquier cliente HTTP puede consumir la API (Postman, curl, otra app).
- Cacheable: el navegador y proxies intermedios pueden cachear respuestas GET.
- Simplicidad: el equipo no necesita aprender un query language adicional (como GraphQL).
- Los mappers del frontend (`features/tasks/mappers.ts`) desacoplan la forma de los datos de la API de la representación interna del frontend.

### Negativas
- Over-fetching: el servidor siempre devuelve todos los campos del recurso, incluso si el cliente no los necesita todos (aceptable para entidades pequeñas como Tarea y Usuario).
- Under-fetching: para obtener una tarea con su estado y usuario, el backend debe incluirlos en la respuesta (resuelto con JPA/Jackson que serializa las relaciones `@ManyToOne`).
- Sin notificaciones push: si otro usuario modifica una tarea, el cliente no se entera hasta que haga un nuevo GET (aceptable para el volumen de uso).

### Trade-off aceptado
Priorizamos **simplicidad, estándares HTTP y herramientas de documentación** sobre flexibilidad de consultas y comunicación en tiempo real, lo cual es la decisión correcta para un CRUD de gestión de tareas con un solo tipo de consumidor.
