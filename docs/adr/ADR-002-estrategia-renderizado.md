# ADR-002 — Estrategia de renderizado: Client-Side Rendering (CSR/SPA)

---

## Contexto

Necesitábamos definir la estrategia de renderizado para nuestra aplicación de gestión de tareas interna. El renderizado es una decisión arquitectónica clave porque impacta directamente en el rendimiento, la escalabilidad y la experiencia del usuario. La decisión determina **dónde** ocurre la transformación de datos en interfaz visual: en el servidor, en el cliente, o en una combinación.

### Estrategias evaluadas

| Técnica | Descripción | Cuándo usarla | Métricas clave |
|---|---|---|---|
| **CSR** | El servidor envía HTML mínimo + JS. El navegador construye todo el DOM en cliente. | Apps altamente interactivas (dashboards, apps internas) donde SEO no es crítico. | FCP y LCP más lentos en páginas grandes. TTI puede demorarse. |
| **SSR** | El servidor renderiza HTML en cada request y lo entrega ya listo. | Sitios con mucho contenido dinámico que requieren SEO (e-commerce, noticias). | FCP y LCP rápidos. Puede aumentar TTFB. TBT moderado. |
| **SSG** | HTML generado en build time, archivos estáticos servidos desde CDN. | Documentación, blogs, landing pages con contenido que cambia poco. | TTFB y FCP muy rápidos y constantes. TBT e INP bajos. |

También vimos técnicas avanzadas como hidratación parcial (islas, frameworks como Astro), hidratación progresiva (React 18/Nuxt 3.16+) y renderizado trismórfico, pero aplican a escenarios de mayor complejidad.

### Características de nuestro proyecto

- **App interna:** Solo acceden usuarios autenticados de la organización, no el público general.
- **SEO irrelevante:** Al requerir login, los buscadores no indexan el contenido.
- **Alta interactividad:** CRUD de tareas, filtros por estado, cambios de estado, asignación de miembros, vista lista/grilla.
- **Contenido dinámico:** Los datos cambian constantemente (tareas se crean, asignan, resuelven, cancelan).

---

## Decisión

Adoptamos **Client-Side Rendering (CSR)** implementado como una **Single Page Application (SPA)** con Vue 3.

### Funcionamiento en nuestro proyecto

El ciclo de vida de una SPA sigue estos pasos:

1. **Carga del HTML:** El navegador solicita el HTML. En nuestro caso, Nginx sirve `client-arqweb/index.html`, un archivo mayormente vacío con `<div id="app"></div>` y las etiquetas `<script>` y `<link>` que referencian los bundles generados por Vite.

2. **Carga de JS/CSS:** El navegador descarga y analiza los archivos JavaScript y CSS. En este punto la pantalla aún está en blanco: el `<body>` solo contiene `<div id="app">`.

3. **Montaje de la aplicación:** El framework Vue encuentra los componentes y monta la aplicación. En nuestro proyecto esto ocurre en `client-arqweb/src/main.ts`, que crea la instancia Vue, registra Pinia y Vue Router, y monta en `#app`. A partir de ahí, `App.vue` renderiza el `<RouterView />` que carga la vista correspondiente a la ruta actual.

4. **Interacciones del usuario:** Cuando el usuario interactúa (ej: cambia el estado de una tarea), Vue maneja el evento y "parchea" solo las partes del DOM que cambiaron. Este proceso se llama **reconciliación** y minimiza los cambios en el documento.

5. **Navegación por rutas:** Al hacer clic en un enlace (ej: de `/tasks` a `/tasks/3`), Vue Router (`app/router/index.ts`) evita la recarga de la página; renderiza el componente de la nueva ruta (`TaskDetailView.vue`) y actualiza la URL. La ilusión de múltiples páginas es creada por el framework.

### Build en producción (proceso de compilación/empaquetado)

Antes de desplegar la app se debe "construir" para producción. Vite se encarga de:

- **Compilación:** Transforma los archivos `.vue` (SFC) y TypeScript en JavaScript puro.
- **Empaquetado (bundling):** Combina todos los archivos JS, CSS y recursos en bundles optimizados.
- **Minificación y tree shaking:** Elimina código muerto y caracteres innecesarios .
- **Generación del HTML:** Copia `index.html` con las referencias a los bundles generados.

El resultado es un directorio de archivos estáticos que en nuestro proyecto se sirve con Nginx (ver `client-arqweb/Dockerfile` y `client-arqweb/nginx/default.conf`).

### Por qué CSR/SPA y no otra alternativa

- **vs. SSR:** No necesitamos que el servidor genere HTML en cada request. No hay beneficio de SEO (app privada con login). SSR agregaría complejidad: necesitaríamos un meta-framework como Nuxt o un servidor Node.js para renderizar Vue. Además, SSR aumenta la carga del servidor en cada petición. SSR es ideal para sitios con contenido dinámico que requieren SEO (e-commerce, noticias); nuestro caso no aplica.
- **vs. SSG:** El contenido de tareas es 100% dinámico y cambia constantemente. SSG genera HTML en build time, por lo que sería inviable: necesitaríamos un rebuild cada vez que se crea una tarea. SSG es ideal para documentación, blogs y landing pages; nuestro caso no aplica.
- **vs. Hidratación parcial (Astro):** Astro usa "islas" donde solo se hidratan componentes interactivos y el resto queda estático. Es útil cuando se mezcla contenido estático con widgets (ej: artículos con formularios). Nuestra app es enteramente interactiva, así que no tiene sentido.

### Ventajas de CSR para nuestro proyecto

1. **Experiencia de aplicación fluida:** Una vez cargada, las transiciones entre `/tasks`, `/tasks/:id` y `/members` son instantáneas (no hay recarga de página).
2. **Menor carga en el servidor:** El backend solo sirve datos JSON via API REST; no necesita renderizar HTML.
3. **Separación clara frontend/backend:** El frontend (`client-arqweb/`) es un proyecto independiente que consume la API. Se pueden trabajar en paralelo.
4. **Modo mock para desarrollo:** Se puede desarrollar el frontend sin backend usando `VITE_USE_MOCK_API=true` (cada feature tiene su archivo `api.mock.ts`).

---

## Consecuencias

### Positivas
- UX fluida tipo aplicación de escritorio: navegación sin recargas, feedback inmediato.
- Backend desacoplado: Spring Boot solo expone API REST, sin responsabilidades de renderizado.
- Desarrollo paralelo: frontend y backend pueden evolucionar de forma independiente.
- Caching efectivo: los assets estáticos (JS, CSS) se cachean en el navegador; solo los datos JSON viajan en cada interacción. En producción, Nginx sirve los assets con cache headers.

### Negativas
- Tiempo de carga inicial mayor que SSR/SSG (mitigado por Vite que aplica code splitting, tree shaking y minificación automáticamente en el build de producción).
- Todo el peso de la UI recae en el navegador del cliente (aceptable en PCs de oficina).
- Se requiere un proxy en desarrollo (`vite.config.ts` proxea `/api` a `http://localhost:8081`) y en producción (`nginx/default.conf` proxea `/api` al contenedor del backend).

### Trade-off aceptado
Priorizamos **interactividad y experiencia de usuario** sobre velocidad de primera carga, lo cual es la decisión correcta para una app interna donde los usuarios acceden repetidamente y el SEO es irrelevante.
