# ADR-004 — Framework backend: Spring Boot 3.3 (Java 17)

---

## Contexto

Necesitábamos seleccionar el framework para implementar el backend de nuestra aplicación de gestión de tareas interna. El backend debe exponer una API REST, conectarse a MySQL, manejar autenticación/autorización por roles y documentar sus endpoints.

Los frameworks de desarrollo web del lado del servidor proporcionan herramientas que simplifican tareas comunes como el enrutamiento de URLs, la interacción con bases de datos, el soporte de sesiones y autorización de usuarios, el formateo de salida (JSON, XML) y la mejora de la seguridad contra ataques web.

### Comparación de frameworks evaluados

| Framework | Cuándo usarlo | Ventajas | Limitaciones |
|---|---|---|---|
| **Express (Node.js)** | Prototipos rápidos, microservicios livianos, APIs REST sencillas | Ligero, minimalista, enorme ecosistema NPM, rápido desarrollo | Requiere disciplina para mantener estructura limpia; poco "opinado" |
| **NestJS (Node.js)** | Estructura sólida con arquitectura modular (similar a Angular o Spring) | DI, decoradores, testing integrado, escalable | Curva de aprendizaje mayor; overhead inicial |
| **Django (Python)** | Monolito web con ORM integrado y panel admin | "Baterías incluidas", seguridad nativa, productividad | Menos flexible para microservicios; acoplamiento fuerte |
| **FastAPI (Python)** | Alto rendimiento y tipado fuerte, APIs REST/GraphQL | Async I/O, OpenAPI automático, validación con Pydantic | Ecosistema más nuevo; menor madurez que Django |
| **Spring Boot (Java)** | Apps empresariales grandes, con escalabilidad, transacciones y seguridad | Estandarizado, DI, soporte transaccional, microservicios con Spring Cloud | Verboso; requiere experiencia previa |

### Requisitos de nuestro proyecto

1. **API REST completa:** CRUD de tareas y usuarios, autenticación, documentación.
2. **Seguridad robusta:** JWT + BCrypt, filtros de seguridad, autorización por rol (ADMIN vs TECNICO).
3. **ORM maduro:** Mapeo objeto-relacional con JPA/Hibernate para MySQL.
4. **Validación:** Validación declarativa de DTOs en la entrada.
5. **Documentación automática:** OpenAPI/Swagger integrado.
6. **Transacciones:** Control transaccional en operaciones de escritura.

---

## Decisión

Adoptamos **Spring Boot 3.3.5 con Java 17** como framework backend.

### Stack técnico completo (según `api-arqweb/pom.xml`)

| Componente | Tecnología | Dependencia Maven |
|---|---|---|
| Framework web | Spring MVC | `spring-boot-starter-web` |
| ORM | JPA + Hibernate | `spring-boot-starter-data-jpa` |
| Seguridad | Spring Security + JWT | `spring-boot-starter-security` + `jjwt 0.12.6` |
| Validación | Jakarta Bean Validation | `spring-boot-starter-validation` |
| Documentación API | SpringDoc OpenAPI | `springdoc-openapi-starter-webmvc-ui 2.6.0` |
| Base de datos | MySQL 8.4 | `mysql-connector-j` |
| Testing | JUnit + Spring Test | `spring-boot-starter-test` |

### Organización del código (`api-arqweb/src/main/java/com/empresa/taskmanager/`)

```
taskmanager/
├── TaskManagerBaseApplication.java      → Entry point (@SpringBootApplication)
├── controller/
│   ├── AuthController.java              → POST /api/auth/registro, POST /api/auth/login
│   ├── TareaController.java             → CRUD /api/tareas (GET, POST, PUT, DELETE)
│   └── UsuarioController.java           → CRUD /api/usuarios (GET, POST, PUT, DELETE) — solo ADMIN
├── service/
│   ├── AuthService.java                 → Registro con BCrypt, login con JWT
│   ├── UsuarioService.java              → CRUD de usuarios con soft delete
│   ├── interfaces/
│   │   └── TareaService.java            → Contrato de operaciones sobre tareas
│   └── impl/
│       └── TareaServiceImpl.java        → Implementación con lógica de autorización por rol
├── repository/
│   ├── TareaRepository.java             → Derived queries con filtro de soft delete
│   ├── UsuarioRepository.java           → findByEmail, existsByEmail, filtros soft delete
│   ├── RolRepository.java               → CRUD del catálogo de roles
│   └── EstadoRepository.java            → CRUD del catálogo de estados
├── model/
│   ├── Tarea.java                       → @Entity mapeada a tabla `tareas`
│   ├── Usuario.java                     → @Entity mapeada a tabla `usuarios`
│   ├── Rol.java                         → @Entity mapeada a tabla `roles`
│   └── Estado.java                      → @Entity mapeada a tabla `estados`
├── dto/
│   ├── LoginRequest.java                → Record: email, password (validación Jakarta)
│   ├── RegistroRequest.java             → Record: nombre, email, password (8-72 chars), idRol
│   ├── TokenResponse.java               → Record: token, tipo, expiraEnSegundos, email, rol
│   └── UsuarioUpdateRequest.java        → Record: nombre, email, password (opcional), idRol
├── security/
│   ├── JwtService.java                  → Genera y valida tokens JWT (HMAC-SHA)
│   ├── JwtAuthFilter.java               → Filtro que intercepta cada request y valida JWT
│   ├── SecurityConfig.java              → Reglas de autorización por ruta y rol
│   ├── UsuarioDetailsService.java       → Carga usuario activo por email para Spring Security
│   └── OpenApiConfig.java               → Configura Swagger con botón "Authorize" para Bearer
└── exception/
    ├── ApiExceptionHandler.java          → @RestControllerAdvice: 400, 401, 403, 404 uniformes
    └── ResourceNotFoundException.java    → Excepción custom para recursos no encontrados
```

### Por qué Spring Boot y no otra alternativa

- **vs. Express (Node.js):** Express es minimalista y poco opinado. Para nuestra app con autenticación JWT, ORM, validación y documentación Swagger, habría que agregar y configurar cada librería manualmente (Sequelize, Passport, express-validator, swagger-jsdoc). Spring Boot incluye todo esto de forma integrada y estandarizada. Además, Express no tiene soporte nativo para inyección de dependencias ni arquitectura por capas.
- **vs. NestJS (Node.js):** NestJS se inspira en Spring/Angular y ofrece DI y decoradores similares. Es una alternativa válida, pero Spring Boot tiene un ecosistema más maduro para aplicaciones empresariales, mejor soporte transaccional con JPA/Hibernate, y Spring Security es más completo.
- **vs. Django (Python):** Django es "baterías incluidas" pero está más orientado a apps con frontend renderizado en servidor (templates). Para una API REST pura que sirve JSON, Spring Boot con Spring MVC es más idiomático.
- **vs. FastAPI (Python):** FastAPI es excelente para APIs ligeras con OpenAPI automático, pero su ecosistema de seguridad y ORM es menos maduro que Spring. Para nuestra app con múltiples capas, transacciones y autenticación compleja, Spring Boot ofrece más herramientas out-of-the-box.

### Ventajas concretas de Spring Boot en nuestro proyecto

1. **Inyección de dependencias (IoC):** Spring crea y conecta las clases automáticamente. `TareaController` depende de la interfaz `TareaService`, no de `TareaServiceImpl`.
2. **@Transactional:** Delimita operaciones de escritura como una unidad atómica en `TareaServiceImpl` y `UsuarioService`.
3. **Spring Data JPA:** `JpaRepository` genera la implementación CRUD automáticamente. `TareaRepository` define queries solo con el nombre del método (`findByFechaEliminacionIsNull`).
4. **Validación declarativa:** `@Valid` + anotaciones Jakarta en los DTOs (`LoginRequest`, `RegistroRequest`) activan la validación antes de ejecutar el caso de uso.
5. **SpringDoc/Swagger:** Documentación interactiva auto-generada en `/swagger-ui/index.html` (enfoque code-first). `OpenApiConfig.java` configura el botón "Authorize" para probar con JWT.
6. **Configuración externalizada:** Puerto, base de datos y secreto JWT se configuran por variables de entorno en `application.yml`.

---

## Consecuencias

### Positivas
- Arquitectura robusta y bien definida con separación clara de responsabilidades.
- Ecosistema empresarial maduro: testing, seguridad, transacciones, documentación.
- Amplia documentación y comunidad.
- Compatibilidad directa con Docker (JAR ejecutable autocontenido, ver `Deploy/api.Dockerfile`).
- Serialización JSON automática via Jackson (incluye `@JsonIgnore` en password de `Usuario`).

### Negativas
- Mayor verbosidad que frameworks de scripting (Node.js, Python): más archivos, clases y anotaciones.
- Curva de aprendizaje inicial para quien no conoce el ecosistema Spring.
- Consumo de memoria mayor que una app Node.js/Express para funcionalidades equivalentes.
- Tiempos de compilación y arranque más lentos que frameworks interpretados.

### Trade-off aceptado
Priorizamos **robustez, ecosistema empresarial integrado y separación de responsabilidades** sobre velocidad de desarrollo inicial y simplicidad del código, lo cual es adecuado para un backend que necesita seguridad, transacciones y documentación de API.
