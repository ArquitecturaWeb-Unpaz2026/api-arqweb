# ADR-010 — Despliegue: Docker Compose con Caddy, Nginx y WireGuard

---

## Contexto

Necesitábamos definir la estrategia de despliegue para poner en producción nuestra aplicación de gestión de tareas interna. El sistema consta de cuatro componentes principales:

1. **Frontend:** SPA Vue 3 compilada a archivos estáticos HTML/JS/CSS, servida por Nginx.
2. **Backend:** API Spring Boot empaquetada como JAR ejecutable.
3. **Base de datos:** MySQL 8.4.
4. **VPN:** Acceso seguro para el equipo de desarrollo a la base de datos.

Además, se requiere HTTPS para los endpoints públicos.

### Alternativas evaluadas

| Estrategia | Descripción | Complejidad | Costo |
|---|---|---|---|
| **Hosting tradicional** | Servidor con Apache/Nginx, instalación manual de Java, MySQL | Alta (configuración manual) | Bajo |
| **Docker Compose** | Contenedores orquestados con definición declarativa | Media | Bajo |
| **Kubernetes** | Orquestación de contenedores a escala | Muy alta | Alto |
| **PaaS (Heroku, Railway)** | Plataforma gestionada, deploy por push | Baja | Medio-alto |
| **Serverless (AWS Lambda)** | Funciones sin servidor | Media-alta | Variable |

---

## Decisión

Adoptamos **Docker Compose** para orquestar todos los servicios en un VPS (Hostinger), con **Caddy** como reverse proxy externo, **Nginx** para servir el frontend y **WireGuard** como VPN.

### Arquitectura de despliegue

```
Internet
    │
    ▼
[ Caddy (reverse proxy + TLS automático) ]  ← red externa: web-gateway
    │
    ├── arqweb.aulas-unpaz.cloud        → arqweb_web (Nginx :80) → archivos estáticos Vue
    │                                      └── /api → proxy a arqweb_api:8081
    └── api.arqweb.aulas-unpaz.cloud    → arqweb_api (Spring Boot :8081)
                                             │
                                             ▼
                                    [ arqweb_db (MySQL :3306) ]  ← red interna: arqweb-network
                                             │
                                             ▲
                                    [ arqweb_wireguard (VPN :51830/UDP) ]
                                             │
                                    Equipo de desarrollo (acceso remoto a BD)
```

### Servicios en Docker Compose (`Deploy/docker-compose.yml`)

| Servicio | Imagen/Build | Puerto | Red | Función |
|---|---|---|---|---|
| `arqweb_wireguard` | linuxserver/wireguard | 51830 UDP | arqweb-network | VPN para acceso seguro del equipo a la BD |
| `arqweb_db` | mysql:8.4 | 3306 (interno) | arqweb-network (172.60.0.99) | Base de datos MySQL |
| `arqweb_web` | Build desde `client-arqweb/Dockerfile` | 80 (interno) | arqweb-network + web-gateway | Frontend Vue (Nginx) + proxy `/api` |
| `arqweb_api` | Build desde `Deploy/api.Dockerfile` | 8081 (interno) | arqweb-network + web-gateway | API REST Spring Boot |

### Build del frontend (`client-arqweb/Dockerfile`)

```dockerfile
# Etapa 1: Build con Node
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build              # Vite compila, minifica, tree-shaking

# Etapa 2: Servir con Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
```

El `nginx/default.conf` configura:
- Servir archivos estáticos del frontend desde `/usr/share/nginx/html`.
- Proxy de `/api` al contenedor del backend (`arqweb_api:8081`).
- `try_files $uri /index.html` para que Vue Router maneje todas las rutas (SPA).

### Build del backend (`Deploy/api.Dockerfile`)

```dockerfile
# Etapa 1: Compilación
FROM maven:3.9-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package          # Compila Java, ejecuta tests, empaqueta JAR

# Etapa 2: Runtime
FROM eclipse-temurin:17-jre
COPY --from=build target/*.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
```

Ventajas del multi-stage build:
- La imagen final solo contiene el JRE y el JAR, no Maven ni el código fuente.
- Imagen más liviana y segura (menor superficie de ataque).

### Redes Docker

- **arqweb-network (172.60.0.0/24):** Red interna que conecta los 4 servicios. Aislada del exterior.
- **web-gateway (externa):** Red compartida con Caddy para exponer frontend y API a internet via HTTPS.

Esta separación asegura que la base de datos **nunca** sea accesible directamente desde internet; solo a través de la API (red interna) o la VPN (WireGuard).

### Reverse proxy externo (`Deploy/Caddyfile`)

```
arqweb.aulas-unpaz.cloud {
    reverse_proxy arqweb_web:80
}

api.arqweb.aulas-unpaz.cloud {
    reverse_proxy arqweb_api:8081
}
```

Caddy genera y renueva certificados TLS automáticamente (Let's Encrypt). El backend recibe el header `X-Forwarded-Proto: https` (configurado en `application.yml` con `forward-headers-strategy: framework`) para que Swagger UI publique URLs con https.

### Seguridad del despliegue

| Aspecto | Implementación |
|---|---|
| HTTPS | Caddy genera y renueva certificados TLS automáticamente |
| Acceso a BD | Solo via red interna Docker o VPN WireGuard |
| Secretos | Variables de entorno en `.env` (no commiteado, ver `Deploy/.env.example`) |
| Secreto JWT | Inyectado como `JWT_SECRET` (mínimo 32 bytes) |
| Contraseña BD | Inyectada como `MYSQL_ROOT_PASSWORD` |
| Frontend | Nginx sirve solo archivos estáticos; no expone código fuente |
| Backend | Solo el JAR compilado en la imagen final (multi-stage) |

### Por qué Docker Compose y no otra alternativa

- **vs. Hosting tradicional:** Instalar Java, MySQL, Node y Nginx manualmente es propenso a errores, difícil de reproducir y costoso de mantener. Docker Compose permite definir toda la infraestructura como código (Infrastructure as Code) y levantarla con un solo comando.
- **vs. Kubernetes:** Kubernetes es sobreingeniería para una app con 4 servicios y un equipo pequeño. Docker Compose resuelve la orquestación necesaria sin la complejidad de pods, services, ingress controllers y etcd.
- **vs. PaaS (Heroku, Railway):** Las plataformas PaaS simplifican el deploy pero limitan el control sobre la infraestructura (redes, VPN, reverse proxy personalizado) y pueden tener costos crecientes. Docker Compose en un VPS ofrece control total a bajo costo.
- **vs. Serverless:** La arquitectura serverless requiere descomponer la app en funciones independientes, lo cual no se alinea con nuestro monolito en capas. Además, los cold starts de Lambda con Java son significativos.

### Persistencia de datos

```yaml
volumes:
  mysql-arqweb-data:     # Volumen Docker para datos de MySQL (persistente)
```

La API es **stateless** (todo el estado está en la BD o en el JWT) y el frontend sirve archivos estáticos compilados, por lo que ninguno de los dos necesita volúmenes persistentes.

### Proceso de despliegue

```bash
# 1. Configuración inicial
cp .env.example .env
# Editar .env con MYSQL_ROOT_PASSWORD y JWT_SECRET (mínimo 32 bytes)

# 2. Levantar servicios
docker compose up -d --build

# 3. Seed de la base de datos (primera vez)
docker exec -i arqweb_db mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < seed.sql
```

---

## Consecuencias

### Positivas
- Infraestructura como código: toda la topología de servicios está versionada en `Deploy/docker-compose.yml`.
- Reproducibilidad: cualquier miembro del equipo puede levantar el entorno completo con un comando.
- Aislamiento de red: la BD solo es accesible internamente o via VPN.
- Caddy maneja HTTPS automáticamente (sin configurar certificados manualmente).
- Frontend y backend se construyen y despliegan como contenedores independientes.
- Nginx maneja el routing SPA (`try_files`) y el proxy a la API, centralizando la configuración de red del frontend.

### Negativas
- Single point of failure: todos los servicios corren en un mismo VPS. Si el servidor cae, cae todo (aceptable para una app interna no crítica).
- No hay alta disponibilidad ni balanceo de carga (no necesario para el volumen de usuarios).
- Backups de la BD deben configurarse manualmente (cron + mysqldump).
- WireGuard requiere configuración por cada nuevo miembro del equipo.

### Trade-off aceptado
Priorizamos **simplicidad operativa, bajo costo y control total sobre la infraestructura** sobre alta disponibilidad y escalabilidad automática, lo cual es adecuado para una app interna con equipo y usuarios reducidos.
