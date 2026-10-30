# ADR-005 — Framework frontend: Vue 3 con Composition API y TypeScript

---

## Contexto

Necesitábamos seleccionar el framework frontend para construir la interfaz de usuario de nuestra aplicación de gestión de tareas interna. Los frameworks frontend modernos facilitan el desarrollo de apps web interactivas mediante componentes reutilizables que encapsulan lógica e interfaz, reactividad del estado (la UI se actualiza automáticamente al cambiar datos) y enrutamiento del lado del cliente (SPA).

Antes de los frameworks, los desarrolladores tenían que manipular el DOM directamente y gestionar el estado de forma manual, lo cual resultaba en código difícil de mantener. Trabajar constantemente con el DOM es laborioso; la esencia del framework es automatizar esas tareas para que nos concentremos en la lógica.

### Comparación de frameworks evaluados

| Framework | Filosofía | Renderizado soportado | Fortalezas | Debilidades |
|---|---|---|---|---|
| **React** | Librería UI minimalista, ecosistema enorme | CSR nativo. Con Next.js → SSR, SSG, streaming | Enorme comunidad, flexibilidad | No es opinado: hay que elegir router, state manager, etc. |
| **Vue** | Framework progresivo, fácil adopción | CSR nativo. Con Nuxt → SSR, SSG, hidratación progresiva | SFCs, reactividad nativa, API de composición, curva suave | Comunidad menor que React |
| **Angular** | Framework completo (rutas, DI, RxJS) | CSR por defecto, SSR con Angular Universal | Todo integrado, ideal para equipos grandes | Curva de aprendizaje alta, verboso |
| **Svelte** | Compilador que genera JS optimizado | CSR nativo, SSR/SSG con SvelteKit | Rendimiento excelente, menos boilerplate | Ecosistema más pequeño, menos maduro |
| **Astro** | "Framework de islas", centrado en performance | SSG por defecto, hidratación parcial | Excelente para contenido estático + widgets | No ideal para apps enteramente interactivas |

---

## Decisión

Adoptamos **Vue 3.5 con Composition API y TypeScript** como framework frontend.

### Stack técnico completo (según `client-arqweb/package.json`)

| Componente | Tecnología | Versión |
|---|---|---|
| Framework UI | Vue 3 (Composition API) | 3.5.42 |
| Build tool | Vite | 8.2.2 |
| State management | Pinia | 4.1.0 |
| Router | Vue Router | 5.4.0 |
| Lenguaje | TypeScript | 6.0.0 |
| Estilos | CSS custom (design tokens) | — |

### Estructura del proyecto (`client-arqweb/src/`)

```
src/
├── main.ts                          → Entry point: crea Vue, registra Pinia y Router, monta en #app
├── App.vue                          → Componente raíz: solo renderiza <RouterView />
│
├── app/router/
│   ├── index.ts                     → Crea el router con history mode
│   ├── routes.ts                    → Definición de rutas (/login, /tasks, /tasks/:id, /members)
│   ├── guards.ts                    → Guards: requiresAuth, guestOnly, validación de rol
│   └── meta.d.ts                    → Tipos TypeScript para route meta
│
├── layouts/
│   ├── AppLayout.vue                → Shell principal: sidebar, header, panel de sesión
│   └── AuthLayout.vue               → Layout de login con fondo institucional
│
├── features/
│   ├── auth/
│   │   ├── types.ts                 → Tipos: User, Role, LoginCredentials, AuthSession
│   │   ├── api.ts                   → Login real (llama /auth/login, decodifica JWT)
│   │   ├── api.mock.ts              → Login mock (admin@demo.com, member@demo.com)
│   │   ├── stores/session.ts        → Pinia store: bootstrap, login, logout, isAuthenticated
│   │   └── views/LoginView.vue      → Formulario de login con validación y redirect
│   │
│   ├── tasks/
│   │   ├── types.ts                 → Tipos: Task, TaskStatus, TaskInput
│   │   ├── api.ts                   → CRUD real con autorización por rol en cliente
│   │   ├── api.mock.ts              → CRUD mock con datos de ejemplo
│   │   ├── mappers.ts               → Convierte ApiTarea (backend) → Task (frontend)
│   │   ├── composables/useTasks.ts  → Composable: estado reactivo de la lista de tareas
│   │   ├── components/
│   │   │   ├── TaskCard.vue         → Tarjeta de tarea (grid/list, badge de estado, overdue)
│   │   │   ├── TaskCreateDialog.vue → Modal de creación con validación
│   │   │   └── TaskForm.vue         → Formulario alternativo (no modal)
│   │   └── views/
│   │       ├── TaskListView.vue     → Lista con filtros, vista lista/grilla, crear/eliminar
│   │       └── TaskDetailView.vue   → Detalle editable según rol, auto-save
│   │
│   └── members/
│       ├── types.ts                 → Tipos: Member, MemberInput
│       ├── api.ts                   → CRUD de usuarios (solo admin)
│       ├── api.mock.ts              → Mock con 3 usuarios de ejemplo
│       ├── components/
│       │   └── MemberDialog.vue     → Modal para crear/editar miembro
│       └── views/
│           └── MembersView.vue      → Tabla con avatar, acciones editar/eliminar
│
├── shared/
│   ├── api/
│   │   ├── client.ts                → Wrapper sobre fetch(): auto-agrega JWT, maneja errores
│   │   ├── config.ts                → useMockApi(), apiBaseUrl() desde variables de entorno
│   │   ├── jwt.ts                   → Decodifica JWT (base64), valida expiración
│   │   └── types.ts                 → ApiError, ApiErrorBody, Paginated<T>
│   ├── auth/
│   │   ├── permissions.ts           → Sistema de permisos: ROLE_PERMISSIONS, can(), canAccessTask()
│   │   ├── mapRole.ts               → Normaliza roles del backend (ADMIN → admin, TECNICO → member)
│   │   └── roleLabels.ts            → Labels de display (admin → "Administrador")
│   ├── tasks/
│   │   └── statusLabels.ts          → Mapeo de estados: IDs ↔ nombres, labels, isClosedStatus()
│   ├── datetime/
│   │   └── formatDateTime.ts        → Formato dd/mm/yyyy hh:mm, detección de overdue
│   └── components/
│       └── AppButton.vue            → Botón reutilizable (primary, secondary, danger)
│
└── assets/
    ├── main.css                     → Import principal
    ├── base.css                     → Reset de estilos
    ├── tokens.css                   → Design tokens (colores, espaciado, sombras)
    └── ui.css                       → Clases compartidas (badges, formularios, tablas)
```

### Por qué Vue 3 y no otra alternativa

- **vs. React:** React es una librería, no un framework. Para lograr una arquitectura comparable habría que elegir e integrar múltiples librerías (React Router, Zustand/Redux, etc.). Vue ofrece un ecosistema cohesivo con Vue Router y Pinia como soluciones oficiales. Además, los **Single File Components (SFCs)** de Vue encapsulan HTML, CSS y JS en un solo archivo `.vue`, lo cual es más intuitivo que JSX para un equipo que viene del desarrollo web tradicional.
- **vs. Angular:** Angular es un framework completo pero pesado y verboso. Para nuestra app con 4-5 vistas, Angular introduce demasiada ceremonia (módulos, servicios inyectables, RxJS obligatorio). Vue con Composition API logra la misma organización con mucho menos código.
- **vs. Svelte:** Svelte genera código optimizado y es muy ergonómico, pero su ecosistema es significativamente menor. La cantidad de recursos de aprendizaje de Vue lo hacen más adecuado para nuestro equipo.
- **vs. Astro:** Astro usa hidratación parcial con "islas" interactivas. Para una app enteramente interactiva como un gestor de tareas, Astro no es la herramienta adecuada.

### Ventajas concretas de Vue 3 en nuestro proyecto

1. **Composition API y composables:** Organizamos la lógica por funcionalidad. Por ejemplo, `useTasks.ts` encapsula el estado reactivo de la lista de tareas (carga, creación, actualización, eliminación) y se reutiliza en las vistas.
2. **TypeScript nativo:** Los tipos (`Task`, `User`, `TaskStatus`) dan seguridad en tiempo de desarrollo. El archivo `meta.d.ts` tipea las rutas.
3. **Pinia (state management):** `session.ts` maneja la sesión del usuario como un store reactivo: `bootstrap()`, `login()`, `logout()`, `isAuthenticated`. Es más simple que Vuex, con soporte nativo de TypeScript.
4. **Vite (build tool):** HMR instantáneo en desarrollo; build optimizado con tree shaking, minificación y code splitting en producción.
5. **Vue Router con guards:** `guards.ts` protege rutas por autenticación y rol de forma declarativa. Valida la sesión en el primer acceso (`bootstrap`).
6. **Mock API por feature:** Cada feature tiene `api.mock.ts` para desarrollo sin backend (`VITE_USE_MOCK_API=true`).
7. **Mappers:** `mappers.ts` convierte las respuestas del backend (`ApiTarea`) al formato del frontend (`Task`), desacoplando las representaciones.
8. **Sistema de permisos:** `permissions.ts` define permisos granulares (`task.view`, `task.create`, `task.edit`, `task.status`, `task.delete`, `task.assign`, `member.view`, `member.manage`) y funciones como `can(role, permission)`, `canAccessTask()`, `canMutateTask()`.

---

## Consecuencias

### Positivas
- Desarrollo ágil con HMR de Vite (cambios reflejados al instante sin recargar).
- Componentes encapsulados (SFC) que agrupan template, lógica y estilos.
- Reactividad automática: la UI se actualiza al cambiar el estado sin manipulación manual del DOM (reconciliación del virtual DOM).
- Mock API integrado para desarrollo offline.
- TypeScript previene errores comunes y mejora la documentación implícita del código.
- Sistema de permisos reutilizable que replica las reglas del backend (defensa en profundidad).

### Negativas
- Comunidad menor que React: menos componentes de terceros.
- Curva de aprendizaje de TypeScript + Composition API para quien viene de JavaScript vanilla.
- Sin SSR nativo (necesitaría Nuxt), pero no lo necesitamos para nuestro proyecto (ver ADR-002).

### Trade-off aceptado
Priorizamos **cohesión del ecosistema, facilidad de aprendizaje y ergonomía de desarrollo** sobre el tamaño de la comunidad, lo cual es adecuado para nuestro equipo que necesita productividad sin la complejidad de ensamblar múltiples librerías.
