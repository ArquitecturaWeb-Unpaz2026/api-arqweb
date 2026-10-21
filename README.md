# Backend base de tareas

Versión inicial del backend principal. Contiene el modelo `Tarea`, su repositorio JPA, el servicio CRUD y el controlador REST. Las clases tienen comentarios explicativos y [ARQUITECTURA_WEB.md](ARQUITECTURA_WEB.md) relaciona las decisiones con los contenidos de la materia.

## Ejecutar

Requiere Java 17 o superior y PostgreSQL. Desde esta carpeta, configurar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` si la base no está en los valores por defecto, y ejecutar:

```powershell
mvn spring-boot:run
```

Por defecto escucha en el puerto `8081` para no interferir con la API principal.

## Rutas

- `GET /api/tareas`: listar.
- `GET /api/tareas/{id}`: consultar una.
- `POST /api/tareas`: crear.
- `PUT /api/tareas/{id}`: editar título, descripción y prioridad.
- `DELETE /api/tareas/{id}`: eliminar.

Esta versión base no incluye todavía autenticación, roles, asignación ni historial.
