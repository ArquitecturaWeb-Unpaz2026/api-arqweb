# Architecture Decision Records (ADR)

Registro de decisiones arquitectónicas del equipo para la **App de Gestión de Tareas Interna**.

Cada ADR documenta una decisión importante.

---

## Índice

| ID | Decisión |
|---|---|
| [ADR-001](ADR-001-estilo-arquitectonico.md) | Estilo arquitectónico: Monolito en N-Capas |
| [ADR-002](ADR-002-estrategia-renderizado.md) | Estrategia de renderizado: CSR / SPA |
| [ADR-003](ADR-003-base-de-datos.md) | Base de datos: MySQL 8.4 (SQL relacional, ACID) |
| [ADR-004](ADR-004-framework-backend.md) | Framework backend: Spring Boot 3.3 (Java 17) |
| [ADR-005](ADR-005-framework-frontend.md) | Framework frontend: Vue 3 + TypeScript + Pinia |
| [ADR-006](ADR-006-orm-persistencia.md) | ORM y persistencia: Spring Data JPA + Hibernate |
| [ADR-007](ADR-007-autenticacion-jwt.md) | Autenticación y autorización: JWT + Spring Security + BCrypt |
| [ADR-008](ADR-008-soft-delete.md) | Estrategia de eliminación: Soft Delete (tareas y usuarios) |
| [ADR-009](ADR-009-comunicacion-api-rest.md) | Comunicación: API REST con OpenAPI/Swagger |
| [ADR-010](ADR-010-despliegue-docker.md) | Despliegue: Docker Compose + Caddy + Nginx + WireGuard |
| [ADR-011](ADR-011-stack-tecnologico.md) | Stack tecnológico: Vue 3 + Spring Boot + MySQL + Docker |

---

## Escenario

**App de gestión de tareas interna** para un equipo reducido. Permite crear, asignar y gestionar tareas con roles diferenciados (administrador y técnico), autenticación JWT, autorización granular por rol y despliegue containerizado.

## Stack tecnológico

- **Frontend:** Vue 3 + Vite + Pinia + Vue Router + TypeScript (CSR/SPA, servido por Nginx)
- **Backend:** Spring Boot 3.3 + Java 17 + Spring Security + JPA/Hibernate
- **Base de datos:** MySQL 8.4 (ACID, InnoDB)
- **Despliegue:** Docker Compose + Caddy (reverse proxy + TLS) + Nginx (frontend) + WireGuard (VPN)

