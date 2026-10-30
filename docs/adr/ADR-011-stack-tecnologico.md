# ADR-011 — Stack tecnológico: Vue 3 + Spring Boot + MySQL + Docker

---

## Contexto

Necesitábamos definir el conjunto completo de tecnologías para nuestra aplicación de gestión de tareas interna. La elección del stack no es una suma de decisiones aisladas: las tecnologías deben integrarse bien entre sí, cubrir todos los requerimientos funcionales y ser manejables por nuestro equipo.

### Requerimientos del sistema

| Área | Necesidad concreta |
|---|---|
| **Interfaz de usuario** | SPA interactiva con vistas de login, lista de tareas, detalle de tarea, gestión de miembros. Filtros, cambio de estado, asignación. |
| **API** | REST stateless con CRUD de tareas y usuarios, autenticación JWT, documentación Swagger. |
| **Persistencia** | Base de datos relacional con integridad referencial, transacciones ACID, soft delete. |
| **Seguridad** | Autenticación con contraseñas hasheadas, autorización por roles (ADMIN / TECNICO), token stateless. |
| **Despliegue** | Containerizado, reproducible, con HTTPS y acceso VPN a la base de datos para el equipo. |
| **Equipo** | Pequeño, con conocimientos variados. El stack debe ser aprendible y bien documentado. |

---

## Decisión

Adoptamos el siguiente stack tecnológico:

### Stack completo

| Capa | Tecnología | Versión | Rol |
|---|---|---|---|
| **Frontend** | Vue 3 (Composition API) | 3.5.42 | Framework UI, componentes reactivos, SPA |
| | TypeScript | 6.0.0 | Tipado estático del frontend |
| | Vite | 8.2.2 | Build tool, dev server con HMR, bundling |
| | Pinia | 4.1.0 | State management (sesión del usuario) |
| | Vue Router | 5.4.0 | Enrutamiento SPA con guards de autenticación/rol |
| | CSS custom (tokens) | — | Sistema de diseño propio con variables CSS |
| **Backend** | Spring Boot | 3.3.5 | Framework web, IoC, configuración externalizada |
| | Java | 17 | Lenguaje del backend |
| | Spring MVC | — | Controladores REST, serialización JSON (Jackson) |
| | Spring Security | — | Filtros de seguridad, autorización por rol |
| | Spring Data JPA + Hibernate | — | ORM, repositorios, Unit of Work, transacciones |
| | JJWT | 0.12.6 | Generación y validación de tokens JWT |
| | Jakarta Validation | — | Validación declarativa de DTOs |
| | SpringDoc OpenAPI | 2.6.0 | Documentación Swagger auto-generada |
| **Base de datos** | MySQL | 8.4 | Motor relacional ACID (InnoDB) |
| **Servidor web** | Nginx | alpine | Sirve el frontend compilado, proxea `/api` al backend |
| **Reverse proxy** | Caddy | — | TLS automático (Let's Encrypt), routing por dominio |
| **VPN** | WireGuard | — | Acceso seguro del equipo a la base de datos |
| **Contenedores** | Docker Compose | — | Orquestación de los 4 servicios |
| **Build backend** | Maven | 3.9.9 | Compilación, testing y empaquetado del JAR |
| **Build frontend** | npm + Vite | — | Instalación de dependencias, build de producción |

### Archivos de configuración del stack

| Archivo | Define |
|---|---|
| `client-arqweb/package.json` | Dependencias y scripts del frontend |
| `client-arqweb/vite.config.ts` | Plugins Vue, alias `@/`, proxy de `/api` en desarrollo |
| `client-arqweb/tsconfig.app.json` | Configuración TypeScript: strict null checks, path mapping |
| `client-arqweb/Dockerfile` | Build multi-stage: Node (compilación) → Nginx (serving) |
| `client-arqweb/nginx/default.conf` | Serving de archivos estáticos + proxy `/api` → backend |
| `api-arqweb/pom.xml` | Dependencias Maven del backend |
| `api-arqweb/src/main/resources/application.yml` | Puerto, datasource, JPA, JWT (todo externalizable por env vars) |
| `Deploy/api.Dockerfile` | Build multi-stage: Maven (compilación) → JRE (runtime) |
| `Deploy/docker-compose.yml` | 4 servicios: WireGuard, MySQL, frontend (Nginx), API (Spring Boot) |
| `Deploy/Caddyfile` | Reverse proxy: dominios → contenedores internos |
| `Deploy/seed.sql` | Esquema de la base de datos y datos iniciales |

### Cómo se integran las piezas

```
Usuario (navegador)
    │
    │  HTTPS
    ▼
[ Caddy ]  ← TLS automático
    │
    ├── arqweb.aulas-unpaz.cloud ──→ [ Nginx ]
    │                                    │
    │                                    ├── Archivos estáticos (Vue build)
    │                                    └── /api/* ──→ proxy
    │                                                    │
    └── api.arqweb.aulas-unpaz.cloud ───────────────────→ [ Spring Boot :8081 ]
                                                              │
                                                              │  JDBC + JPA/Hibernate
                                                              ▼
                                                         [ MySQL :3306 ]
```

**Flujo de una operación típica (crear tarea):**

1. El usuario completa el formulario en `TaskCreateDialog.vue` y hace clic en "Crear".
2. El composable `useTasks.ts` llama a `features/tasks/api.ts` → `api.post('/tareas', body)`.
3. `shared/api/client.ts` agrega el header `Authorization: Bearer <JWT>` y envía el POST.
4. Nginx proxea `/api/tareas` al contenedor de Spring Boot.
5. `TareaController.java` recibe el request, valida con `@Valid` y delega a `TareaService`.
6. `TareaServiceImpl.java` valida estado y usuario contra la BD, aplica reglas de autorización por rol, persiste con `TareaRepository.save()` dentro de una `@Transactional`.
7. Hibernate genera el INSERT SQL y MySQL ejecuta la transacción ACID.
8. El controller responde `201 Created` con la tarea creada y un header `Location`.
9. El frontend recibe la respuesta, `mappers.ts` convierte `ApiTarea` → `Task`, y la UI se actualiza reactivamente.

### Por qué este stack y no otro

**Frontend — Vue 3 vs React vs Angular:**
Vue ofrece un ecosistema cohesivo (Router + Pinia oficiales) sin necesidad de ensamblar librerías de terceros. Los SFCs encapsulan template, lógica y estilos en un archivo. React requeriría elegir e integrar router, state manager y convenciones por separado. Angular sería demasiado pesado para 4-5 vistas.

**Backend — Spring Boot vs Express vs NestJS:**
Spring Boot integra ORM (JPA), seguridad (Spring Security), validación (Jakarta) y documentación (SpringDoc) out-of-the-box. Express requeriría configurar cada pieza manualmente. NestJS es viable pero menos maduro en transacciones y ORM que Spring con Hibernate.

**Base de datos — MySQL vs PostgreSQL vs MongoDB:**
Nuestros datos son estructurados con relaciones fuertes (tarea → usuario → rol, tarea → estado). Necesitamos ACID para integridad transaccional. MySQL es suficiente para nuestras consultas simples (derived queries de JPA). MongoDB no aplica por la naturaleza relacional de los datos.

**Lenguaje frontend — TypeScript vs JavaScript:**
TypeScript nos da tipado estático que previene errores en tiempo de desarrollo. Los tipos compartidos (`Task`, `User`, `TaskStatus`) documentan la estructura de datos implícitamente. El costo de configuración es mínimo con Vite + `vue-tsc`.

**Build — Vite vs Webpack vs Maven:**
Vite ofrece HMR instantáneo en desarrollo y un build de producción optimizado (tree shaking, code splitting, minificación) sin configuración manual. Maven es el estándar para proyectos Spring Boot. Ambos producen artefactos desplegables en contenedores Docker multi-stage.

**Despliegue — Docker Compose vs Kubernetes vs PaaS:**
Docker Compose define toda la infraestructura como código en un solo archivo. Kubernetes sería sobreingeniería para 4 servicios. PaaS limitaría nuestro control sobre redes y VPN.

### Compatibilidad entre componentes

| Integración | Mecanismo |
|---|---|
| Vue ↔ Spring Boot | API REST con JSON. Proxy de Vite en dev, proxy de Nginx en prod. |
| Spring Boot ↔ MySQL | JDBC via `mysql-connector-j`. ORM via JPA/Hibernate. Esquema pre-existente (`ddl-auto: none`). |
| Frontend auth ↔ Backend auth | JWT: el backend firma con JJWT, el frontend decodifica con `shared/api/jwt.ts` y envía en header `Authorization`. |
| Frontend permisos ↔ Backend permisos | Mismas reglas replicadas: `permissions.ts` en frontend, `SecurityConfig.java` + `TareaServiceImpl.java` en backend (defensa en profundidad). |
| Tipos frontend ↔ DTOs backend | `mappers.ts` convierte la forma de datos del backend (`ApiTarea`) al modelo del frontend (`Task`), desacoplando ambos. |

---

## Consecuencias

### Positivas
- Stack maduro y bien documentado en cada capa: cada tecnología tiene amplia comunidad y recursos.
- Las piezas se integran sin fricción: Spring Boot + JPA + MySQL es una combinación probada; Vue + Vite + Pinia es el stack oficial recomendado por Vue.
- Desarrollo paralelo: el frontend y el backend son proyectos independientes conectados por una API REST.
- Mock API permite desarrollar el frontend sin backend (`VITE_USE_MOCK_API=true`).
- Toda la infraestructura está definida como código (Docker Compose, Dockerfiles, Caddyfile).
- Configuración externalizada: secretos y URLs se inyectan por variables de entorno, sin hardcodear nada.

### Negativas
- Dos lenguajes (TypeScript + Java) requieren conocimientos en ambos ecosistemas.
- El stack completo tiene muchas piezas móviles (Vue, Vite, Pinia, Router, Spring Boot, JPA, Security, JWT, MySQL, Nginx, Caddy, Docker, WireGuard).
- Mayor consumo de recursos que un stack ligero (ej: Express + SQLite) por el overhead de JVM + Hibernate.
- Curva de aprendizaje inicial para quien no conoce Spring Boot o Vue con Composition API.

### Trade-off aceptado
Priorizamos **integración probada, herramientas out-of-the-box y separación clara de responsabilidades** sobre simplicidad de setup y menor cantidad de tecnologías.