# ADR-001 — Estilo arquitectónico: Monolito en N-Capas

---

## Contexto

Necesitábamos definir el estilo arquitectónico para nuestra aplicación de gestión de tareas interna. El sistema será utilizado por un equipo reducido dentro de la organización, con una cantidad baja-media de usuarios concurrentes. La lógica de negocio es moderada (CRUD de tareas y usuarios, asignación, estados, roles con autorización granular) y nuestro equipo de desarrollo es pequeño.

No existe una arquitectura universalmente correcta; la elección siempre es contextual y todo es un trade-off. Los criterios que evaluamos fueron:

| Criterio | Nuestro caso |
|---|---|
| Cantidad de usuarios | Baja-media (equipo interno) |
| Complejidad de la lógica de negocio | Media (CRUD + autorización por rol) |
| Escalabilidad esperada | Baja (app interna, no crece exponencialmente) |
| Tiempo de entrega | Corto-medio |
| Equipo de desarrollo | Pequeño |
| Mantenibilidad | Importante (el proyecto debe poder evolucionar) |

### Alternativas evaluadas

| Criterio | Monolito (2 niveles) | N-Capas | Monolito Modular | SOA | Microservicios |
|---|---|---|---|---|---|
| Cantidad de usuarios | Pocos | Medios/Altos | Medios/Altos | Altos | Masivos |
| Complejidad de la lógica | Baja | Media | Media/Alta | Alta | Muy alta |
| Tiempo de entrega | Rápido | Medio | Medio | Largo | Largo, alta inversión |
| Escalabilidad | Limitada | Moderada | Moderada | Alta | Muy alta |
| Equipo de desarrollo | Pequeño | Mediano | Mediano | Grande | Muy grande y especializado |
| Mantenibilidad a largo plazo | Difícil | Buena | Muy buena | Buena | Muy buena |
| Despliegue | Sencillo (1 bloque) | Puede ser distribuido o no | 1 bloque modular | Distribuido | Altamente distribuido |

---

## Decisión

Adoptamos una **arquitectura monolítica en N-Capas (Layered Architecture)** tanto para el backend como para el frontend, con separación lógica clara por responsabilidades.

### Capas del backend (Spring Boot)

La estructura en capas (presentación → negocio → acceso a datos) se implementa en nuestro proyecto dentro del paquete `com.empresa.taskmanager` (`api-arqweb/src/main/java/com/empresa/taskmanager/`):

| Capa | Responsabilidad | Archivos del proyecto |
|---|---|---|
| Presentación / API REST | Recibir solicitudes HTTP, validar entrada, responder JSON | `controller/AuthController.java`, `controller/TareaController.java`, `controller/UsuarioController.java` |
| Negocio / Aplicación | Lógica de casos de uso, reglas de negocio, transacciones | `service/AuthService.java`, `service/UsuarioService.java`, `service/interfaces/TareaService.java`, `service/impl/TareaServiceImpl.java` |
| Acceso a datos | Abstracción de operaciones contra la base de datos | `repository/TareaRepository.java`, `repository/UsuarioRepository.java`, `repository/RolRepository.java`, `repository/EstadoRepository.java` |
| Dominio / Persistencia | Entidades JPA mapeadas a tablas MySQL | `model/Tarea.java`, `model/Usuario.java`, `model/Rol.java`, `model/Estado.java` |
| DTOs | Objetos de transferencia de datos (entrada/salida) | `dto/LoginRequest.java`, `dto/RegistroRequest.java`, `dto/TokenResponse.java`, `dto/UsuarioUpdateRequest.java` |
| Transversal: Seguridad | JWT filter, configuración Spring Security | `security/JwtService.java`, `security/JwtAuthFilter.java`, `security/SecurityConfig.java`, `security/UsuarioDetailsService.java`, `security/OpenApiConfig.java` |
| Transversal: Errores | Manejo global de excepciones | `exception/ApiExceptionHandler.java`, `exception/ResourceNotFoundException.java` |

### Capas del frontend (Vue 3)

La capa de presentación del frontend (`client-arqweb/src/`) se organiza por features con separación interna de responsabilidades:

| Capa | Responsabilidad | Archivos del proyecto |
|---|---|---|
| Vistas / Presentación | Componentes de interfaz de usuario | `features/auth/views/LoginView.vue`, `features/tasks/views/TaskListView.vue`, `features/tasks/views/TaskDetailView.vue`, `features/members/views/MembersView.vue` |
| Componentes reutilizables | Elementos de UI compartidos | `features/tasks/components/TaskCard.vue`, `features/tasks/components/TaskCreateDialog.vue`, `features/members/components/MemberDialog.vue`, `shared/components/AppButton.vue` |
| Estado / Lógica de cliente | Stores reactivos, sesión de usuario | `features/auth/stores/session.ts` (Pinia) |
| Composables | Lógica reactiva reutilizable | `features/tasks/composables/useTasks.ts` |
| Comunicación / API | Cliente HTTP, mappers, mock API | `shared/api/client.ts`, `features/tasks/api.ts`, `features/tasks/api.mock.ts`, `features/tasks/mappers.ts`, `features/auth/api.ts`, `features/members/api.ts` |
| Permisos | Control de acceso por rol en frontend | `shared/auth/permissions.ts`, `shared/auth/mapRole.ts`, `shared/auth/roleLabels.ts` |
| Enrutamiento | Navegación SPA, guards de autorización | `app/router/index.ts`, `app/router/routes.ts`, `app/router/guards.ts` |
| Layouts | Estructura visual de las páginas | `layouts/AppLayout.vue`, `layouts/AuthLayout.vue` |

### Por qué N-Capas y no otra alternativa

- **vs. Monolito simple (2 niveles):** Un monolito sin capas mezclaría la lógica de acceso a datos con la lógica de negocio y la presentación, dificultando el testing y la mantenibilidad. Con capas, cada nivel se puede comprender y probar de forma aislada. En nuestro proyecto, el `TareaController` no conoce cómo se persiste la tarea, solo delega al contrato `TareaService`.
- **vs. Monolito Modular:** Para nuestra app con un dominio acotado (tareas, usuarios, estados, roles), la separación por capas horizontales es suficiente. Un monolito modular con bounded contexts sería sobreingeniería para este alcance.
- **vs. SOA / Microservicios:** Nuestro equipo pequeño con un dominio simple no justifica la complejidad operativa de servicios distribuidos. SOA y microservicios introducen desafíos de red, infraestructura y consistencia de datos que no aportan valor en nuestro contexto. El gran trade-off de microservicios es escalabilidad masiva a costa de altísima complejidad, algo que no necesitamos.

### Ventajas de la arquitectura en capas aplicadas a nuestro proyecto

1. **Separación de responsabilidades (SoC):** Cada capa es independiente. El controlador solo maneja HTTP; el servicio contiene la lógica de negocio (como las reglas de autorización por rol en `TareaServiceImpl`); el repositorio abstrae el acceso a datos.
2. **Facilidad de desarrollo:** Es un patrón conocido y bien documentado. Facilita el onboarding.
3. **Testabilidad:** Se pueden realizar pruebas unitarias en la capa de negocio sin las capas de presentación y datos. El paquete `memoria/` contiene tests (`TareaServiceTest.java`) que validan la lógica de workflow sin base de datos.
4. **Despliegue sencillo:** Un solo JAR desplegable en Docker simplifica la operación.

---

## Consecuencias

### Positivas
- Estructura clara y predecible que facilita el onboarding de nuevos desarrolladores.
- Bajo costo de infraestructura: un solo contenedor Docker para la API.
- Testabilidad mejorada gracias a la inyección de dependencias e interfaces entre capas.
- La inyección de dependencias (IoC) de Spring permite que el `TareaController` dependa de la interfaz `TareaService`, no de la implementación `TareaServiceImpl`, facilitando el reemplazo o testing.
### Negativas
- Un cambio en los requisitos (ej: agregar un nuevo campo a tarea) puede requerir modificaciones en varias capas (model, repository, service, controller, DTO). Esto es una desventaja conocida de las arquitecturas en capas.
- Escalabilidad limitada respecto a microservicios: si el sistema creciera exponencialmente, habría que reevaluar la arquitectura.
- No se puede escalar independientemente una funcionalidad específica (ej: solo el módulo de tareas).

### Trade-off aceptado
Priorizamos **simplicidad, velocidad de desarrollo y bajo costo operativo** sobre escalabilidad extrema.
