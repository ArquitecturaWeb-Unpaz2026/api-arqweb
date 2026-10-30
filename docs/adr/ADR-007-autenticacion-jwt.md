# ADR-007 — Autenticación y autorización: JWT + Spring Security + BCrypt

---

## Contexto

Nuestra aplicación de gestión de tareas interna requiere autenticación (verificar identidad del usuario) y autorización (verificar permisos según rol). La seguridad es una cuestión transversal del backend que debe abordarse incorporando mecanismos de autenticación, autorización, control de acceso, confidencialidad e integridad.

Además, la arquitectura REST establece como restricción que el protocolo sea **sin estado (stateless):** cada petición del cliente al servidor es independiente y no guarda estado entre una petición y otra. El servidor no recuerda peticiones anteriores y toda la información necesaria debe enviarse con cada solicitud.

### Alternativas evaluadas

| Mecanismo | Descripción | Stateless | Escalable | Complejidad |
|---|---|---|---|---|
| **Sesiones en servidor** | El servidor almacena la sesión en memoria o base de datos. Se envía una cookie con el session ID. | No | Requiere sticky sessions o store compartido | Baja |
| **JWT (JSON Web Token)** | Token firmado que contiene claims del usuario. El cliente lo envía en cada request. | Sí | Cada servidor puede verificar el token independientemente | Media |
| **OAuth 2.0 / OpenID Connect** | Protocolo de autorización delegada con proveedor externo (Google, Auth0). | Sí | Alta | Alta |
| **API Keys** | Clave estática asociada a un cliente. Sin concepto de usuario. | Sí | Alta | Baja |

---

## Decisión

Adoptamos **JWT (JSON Web Token)** con **Spring Security** para autenticación stateless y **BCrypt** para el hashing de contraseñas.

### Flujo de autenticación implementado

```
1. POST /api/auth/login  { email, password }
   │  → AuthController.java → AuthService.java
   │
2. Spring Security AuthenticationManager verifica credenciales
   │  └── UsuarioDetailsService.java carga usuario activo por email
   │  └── BCrypt.matches(password, hashedPassword)
   │
3. JwtService.java genera token firmado:
   │  - Subject: email del usuario
   │  - Claims: uid (ID), rol (ADMIN/TECNICO)
   │  - Expiration: 24 horas (configurable via JWT_EXPIRATION_MS)
   │  - Firma: HMAC-SHA con secreto de 32+ bytes
   │
4. Respuesta: TokenResponse { token, tipo: "Bearer", expiraEnSegundos, email, rol }
   │
5. Cliente almacena token en localStorage (key: "arqweb_access_token")
   │  → shared/api/client.ts gestiona el almacenamiento
   │
6. Cada request subsiguiente:
   │  Authorization: Bearer eyJhbGciOi...
   │  → shared/api/client.ts agrega el header automáticamente
   │
7. JwtAuthFilter.java intercepta cada request en el backend:
   │  - Extrae token del header Authorization
   │  - Valida firma y expiración via JwtService
   │  - Carga UserDetails via UsuarioDetailsService
   │  - Establece SecurityContext con authorities (ROLE_ADMIN o ROLE_TECNICO)
   │  - Token inválido → se ignora silenciosamente (401 en rutas protegidas)
```

### Seguridad de contraseñas

En `api-arqweb/src/main/java/com/empresa/taskmanager/service/AuthService.java`:

```java
// Registro: hash con BCrypt
usuario.setPassword(passwordEncoder.encode(request.password()));

// Login: verificación via AuthenticationManager
authManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
```

- Las contraseñas **nunca** se almacenan en texto plano. En `Deploy/seed.sql`, los usuarios semilla tienen password `Arqweb2026` hasheado con BCrypt.
- BCrypt incluye salt automático, resistente a rainbow tables.
- La contraseña **nunca** se retorna en las respuestas de la API: `Usuario.java` tiene `@JsonIgnore` en el campo password.

### Reglas de autorización

**Backend (`security/SecurityConfig.java`):**
```
Público:     /api/auth/**                    → Registro y login
             /v3/api-docs/**, /swagger-ui/** → Documentación OpenAPI
Protegido (solo ADMIN):
             /api/usuarios/**               → CRUD de usuarios
             DELETE /api/tareas/**           → Eliminación de tareas
Protegido (autenticado):
             Todas las demás rutas           → Requiere JWT válido
```

Adicionalmente, `SecurityConfig.java` configura respuestas personalizadas para errores de seguridad:
- 401 Unauthorized: token ausente o inválido
- 403 Forbidden: usuario autenticado pero sin el rol requerido

**Lógica de negocio (`service/impl/TareaServiceImpl.java`):**

Más allá de las reglas de ruta, la autorización se aplica a nivel de lógica de negocio:
- **Admin:** Ve, edita y elimina todas las tareas. Puede cambiar todos los campos (título, descripción, estado, asignado).
- **Técnico (member):** Solo ve y edita tareas propias (creadas o asignadas). Solo puede cambiar el estado de la tarea, no otros campos.

**Frontend (`shared/auth/permissions.ts`):**

El frontend replica las mismas reglas como defensa en profundidad:
```typescript
const ROLE_PERMISSIONS = {
  admin: ['task.view', 'task.create', 'task.edit', 'task.status',
          'task.delete', 'task.assign', 'member.view', 'member.manage'],
  member: ['task.view', 'task.create', 'task.status']
}
```

Las funciones `can()`, `canAccessTask()` y `canMutateTask()` se usan en las vistas para mostrar/ocultar acciones según el rol y la propiedad de la tarea.

**Frontend (`app/router/guards.ts`):**
```
/login          → guestOnly (redirige a /tasks si ya está autenticado)
/tasks          → requiresAuth + roles: [admin, member]
/tasks/:id      → requiresAuth + roles: [admin, member]
/members        → requiresAuth + roles: [admin]
```

### Por qué JWT y no otra alternativa

- **vs. Sesiones en servidor:** Las sesiones rompen la restricción stateless de REST. El servidor necesitaría almacenar el estado de cada sesión, complicando la escalabilidad horizontal. Con JWT, cualquier instancia del backend puede verificar el token independientemente. Además, nuestro frontend es una SPA que se comunica via API REST; las cookies de sesión son más naturales para aplicaciones con renderizado en servidor (SSR).
- **vs. OAuth 2.0:** OAuth es un protocolo de autorización delegada pensado para escenarios donde un usuario autoriza a una app de terceros a acceder a sus recursos. Para nuestra app interna con su propia base de usuarios, OAuth agrega complejidad innecesaria (authorization server, tokens de refresh, flujos de redirección).
- **vs. API Keys:** Las API keys identifican a una aplicación, no a un usuario. No permiten diferenciar roles ni identidades individuales, que es un requisito de nuestro sistema.

### Configuración (`api-arqweb/src/main/resources/application.yml`)

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:dev-only-secret-cambiar-en-despliegue-32b}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}  # 24 horas
```

El secreto JWT **nunca** se hardcodea para producción; se inyecta como variable de entorno. El valor por defecto solo aplica en desarrollo local.

---

## Consecuencias

### Positivas
- API completamente stateless: cumple con la restricción REST y facilita escalabilidad horizontal.
- El token contiene la identidad y el rol del usuario, evitando consultas a la base en cada request.
- BCrypt protege las contraseñas con salt automático y factor de costo configurable.
- Defensa en profundidad: tanto el backend (SecurityConfig + TareaServiceImpl) como el frontend (permissions.ts + guards.ts) aplican las mismas reglas de autorización.
- Swagger UI con botón "Authorize" (configurado en `OpenApiConfig.java`) permite probar endpoints protegidos.

### Negativas
- Los tokens JWT no se pueden invalidar antes de su expiración (no hay logout del lado del servidor). Mitigación: expiración de 24 horas limita la ventana de riesgo.
- `localStorage` es vulnerable a XSS. Mitigación: la app no inyecta HTML dinámico no sanitizado.
- El token se envía en cada request, agregando bytes al header HTTP (aceptable para el tamaño de nuestros tokens).
- Sin refresh tokens: al expirar el token, el usuario debe re-autenticarse (aceptable para 24h en una app interna de jornada laboral).

### Trade-off aceptado
Priorizamos **statelessness, simplicidad de implementación y cumplimiento de la arquitectura REST** sobre la capacidad de invalidación de sesiones y la complejidad del manejo de refresh tokens.
