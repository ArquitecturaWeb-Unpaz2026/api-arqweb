# Backend base de tareas

Versión inicial del backend principal. Contiene el modelo `Tarea`, su repositorio JPA, el servicio CRUD y el controlador REST. Las clases tienen comentarios explicativos y [ARQUITECTURA_WEB.md](ARQUITECTURA_WEB.md) relaciona las decisiones con los contenidos de la materia.

## Ejecutar

Requiere Java 17 o superior y MySQL. Desde esta carpeta, configurar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` si la base no está en los valores por defecto, y ejecutar:

```powershell
mvn spring-boot:run
```

Por defecto escucha en el puerto `8081` para no interferir con la API principal.

## Rutas

- `POST /api/auth/registro`: crea un usuario. La contraseña se guarda con BCrypt y no vuelve en la respuesta.
- `POST /api/auth/login`: devuelve un JWT (`Authorization: Bearer`).
- `GET /swagger-ui.html`: documentación interactiva.
- `GET /api/tareas`: listar. Exige token.
- `GET /api/tareas/{id}`: consultar una.
- `POST /api/tareas`: crear.
- `PUT /api/tareas/{id}`: editar título, descripción, estado, usuario asignado y fechas.
- `DELETE /api/tareas/{id}`: baja lógica (`fechaeliminacion`).

La persistencia usa las tablas `roles`, `estados`, `usuarios` y `tareas`. Crear una tarea exige el id de un estado y el id de un usuario creador que ya existan. El registro exige un rol existente. Las rutas de tareas exigen el token del login.

`JWT_SECRET` tiene que medir al menos 32 bytes. Por defecto el token dura 24 horas.
