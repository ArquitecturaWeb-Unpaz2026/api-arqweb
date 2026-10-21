# API de gestión de tareas técnicas

Backend REST académico con Spring Boot y Java 17. Las tareas, técnicos, administradores, grupos y el historial se guardan en listas en memoria; al reiniciar la aplicación los datos vuelven a los valores iniciales.

## Requisitos y ejecución

- Java 17 o superior
- Maven 3.8 o superior

Desde la carpeta del proyecto:

```bash
mvn spring-boot:run
```

La API escucha en `http://localhost:8080`. No tiene login ni autenticación real. Para identificar al actor de cada operación, se envía `X-Usuario-Id`: el administrador de ejemplo tiene id `1`; los técnicos son `101` (Lucía Pérez) y `102` (Mateo Gómez). Los grupos de ejemplo son `1` (Infraestructura, técnicos 101 y 102) y `2` (Aplicaciones, técnico 102).

## Endpoints

| Método | Ruta | Quién | Acción |
|---|---|---|---|
| `GET` | `/api/tecnicos` | cualquiera | Listar técnicos disponibles |
| `GET` | `/api/grupos` | cualquiera | Listar grupos y miembros |
| `POST` | `/api/tareas` | administrador | Crear tarea |
| `GET` | `/api/tareas` | administrador | Listar todas las tareas |
| `GET` | `/api/tareas/{id}` | administrador | Consultar una tarea e historial |
| `PUT` | `/api/tareas/{id}/asignacion` | administrador | Asignar directamente a un técnico |
| `PUT` | `/api/tareas/{id}/derivacion` | administrador | Derivar al grupo, sin responsable individual |
| `POST` | `/api/tareas/{id}/tomar` | técnico del grupo | Tomar una tarea grupal disponible |
| `GET` | `/api/tecnicos/{id}/tareas` | ese técnico | Listar asignaciones directas |
| `GET` | `/api/tecnicos/{id}/tareas-grupales` | ese técnico | Ver tareas de sus grupos |
| `GET` | `/api/grupos/{id}/tareas-disponibles` | miembro del grupo | Listar tareas grupales sin responsable |
| `PUT` | `/api/tareas/{id}/estado` | técnico responsable | Comenzar (`ASIGNADA` → `EN_PROCESO`) |
| `PUT` | `/api/tareas/{id}/trabajo` | técnico responsable | Registrar el trabajo realizado |
| `POST` | `/api/tareas/{id}/resolver` | técnico responsable | Guardar resolución y cerrar |
| `POST` | `/api/tareas/{id}/cancelar` | administrador | Cancelar una tarea no resuelta |

Los errores responden con `400`, `403`, `404` o `409` y un cuerpo JSON con estado y mensaje.

## Ejemplos con curl

Crear una tarea:

```bash
curl -i -X POST http://localhost:8080/api/tareas \
  -H 'Content-Type: application/json' -H 'X-Usuario-Id: 1' \
  -d '{"titulo":"Revisar servidor","descripcion":"No responde por SSH","prioridad":"ALTA"}'
```

Asignarla directamente al técnico 101 (usar el ID devuelto al crear):

```bash
curl -X PUT http://localhost:8080/api/tareas/1/asignacion \
  -H 'Content-Type: application/json' -H 'X-Usuario-Id: 1' \
  -d '{"tecnicoId":101}'
```

O derivarla al grupo 1 y permitir que un miembro la tome:

```bash
curl -X PUT http://localhost:8080/api/tareas/1/derivacion \
  -H 'Content-Type: application/json' -H 'X-Usuario-Id: 1' \
  -d '{"grupoId":1}'

curl -X POST http://localhost:8080/api/tareas/1/tomar -H 'X-Usuario-Id: 101'
```

Comenzar, registrar el trabajo y resolver:

```bash
curl -X PUT http://localhost:8080/api/tareas/1/estado \
  -H 'Content-Type: application/json' -H 'X-Usuario-Id: 101' \
  -d '{"estado":"EN_PROCESO"}'

curl -X PUT http://localhost:8080/api/tareas/1/trabajo \
  -H 'Content-Type: application/json' -H 'X-Usuario-Id: 101' \
  -d '{"descripcion":"Se reinició el servicio SSH"}'

curl -X POST http://localhost:8080/api/tareas/1/resolver \
  -H 'Content-Type: application/json' -H 'X-Usuario-Id: 101' \
  -d '{"descripcion":"Conexión SSH verificada"}'
```

Listar las tareas directas del técnico 101:

```bash
curl http://localhost:8080/api/tecnicos/101/tareas -H 'X-Usuario-Id: 101'
```

## Clases principales

- `Tarea`: datos, estado, responsable, grupo de origen, trabajo, solución e historial.
- `Administrador`, `Tecnico`, `GrupoTecnicos`: actores y pertenencia de técnicos a grupos.
- `EstadoTarea`, `Prioridad`: valores enumerados para los estados y las prioridades.
- `TareaService`: mantiene las listas y aplica las reglas de negocio y transiciones.
- `TareaController`, `CatalogoController`: reciben HTTP/JSON y delegan al servicio.
- DTOs: describen cuerpos JSON de entrada sin mezclar la solicitud con la lógica.
- `ApiExceptionHandler`: convierte errores de negocio a respuestas HTTP sencillas.

El encabezado `X-Usuario-Id` solo identifica al actor para el ejercicio: cualquiera puede cambiarlo, por lo que no reemplaza autenticación y no debe usarse como medida de seguridad en producción.

## Pruebas

```bash
mvn test
```

Las pruebas cubren asignación directa, toma exclusiva de tareas grupales, transiciones/resolución y aislamiento entre técnicos.
