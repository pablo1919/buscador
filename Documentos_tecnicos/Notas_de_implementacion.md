# Notas de implementación

Registro de decisiones y cambios de implementación que complementan los
documentos de requerimientos y de proyecto. Cada nota describe el **contexto**,
las **decisiones tomadas**, los **archivos afectados** y lo que queda
**pendiente de verificar** en el entorno de ejecución.

---

## Nota 1 — Clasificación de errores de búsqueda mediante `/salud` con caché

- **Rama:** `feature/salud-clasificacion-errores`
- **Estado:** implementado en la rama; **pendiente de verificación** en el
  entorno de ejecución (Tomcat 8.0 + Innova + gateway de búsqueda).

### Contexto y objetivo

Cuando una búsqueda falla, la interfaz muestra hoy un mensaje genérico de error
(ver RF1.4 en `REUQERIMIENTOS_DE_APLICACION.md`). Se buscó enriquecer ese
mensaje distinguiendo si la causa probable es que el **servicio de búsqueda está
degradado**, usando el endpoint `/salud` (ver `openapi-busqueda.json`, tag
*Sistema*: 200 operativo, 503 alguna dependencia caída), sin exponer información
técnica al usuario y sin multiplicar llamadas al backend.

### Decisión de diseño (Opción B: caché del estado)

Se evalúo consultar `/salud` en cada error (Opción A) frente a cachear su
resultado (Opción B). Se eligió **Opción B** con las siguientes características:

- `/salud` se consulta **únicamente en el camino de error** (nunca en búsquedas
  exitosas).
- El resultado se cachea **en memoria, a nivel de servlet** (una instancia por
  JVM, compartida entre peticiones).
- **TTL configurable** (ver más abajo). Dentro de la ventana, los errores
  siguientes reutilizan el último estado conocido.
- Ante **concurrencia**, comportamiento **stale**: mientras un hilo realiza el
  sondeo, los demás **no esperan**; usan el último estado conocido y, si la
  caché aún está fría, obtienen `DESCONOCIDO`. Esto evita encolar peticiones de
  usuario detrás de un `/salud` lento y evita el efecto *thundering herd*.
- La sincronización usa **doble verificación** (`double-checked locking`) con
  campos `volatile` y un único sondeo efectivo por ventana de TTL.

**Contexto de infraestructura considerado:** 4 nodos; reciclado sólo en
despliegue (rolling de a 2); balanceo con afinidad. Implicaciones:

- Cada nodo mantiene **su propia caché** (no hay caché compartida/distribuida;
  no se introdujo ninguna dependencia nueva para esto).
- Con backend degradado, el número de sondeos por ventana de TTL está acotado
  por el número de nodos con tráfico (~hasta 4 por TTL, y ~2 por ola de
  reciclado). Es un costo aceptable para un uso de diagnóstico.
- No se asume consistencia del estado entre nodos (no es necesaria para el fin).

### Alcance

- Se aplica **sólo al `catch (ApiException)`** del `BuscarServlet` (error de
  integración con el servicio de búsqueda). Los errores internos inesperados
  (`catch (Exception)`) no se clasifican con `/salud`, porque el estado de las
  dependencias no aporta información sobre ellos.
- El camino feliz de la búsqueda queda **sin cambios**.

### Enfoque de clasificación

- La clasificación se basa **únicamente en el código HTTP documentado**: `200`
  → operativo, `503` → degradado, otros códigos o fallo del sondeo →
  desconocido.
- **No** se parsea el cuerpo de `/salud`: el contrato sólo ejemplifica campos
  (`estado`, `opensearch`, `ollama`, `dynamodb`) y el modelo del cliente declara
  además `worker` y `colaPendientes`, que **no** están confirmados en el
  OpenAPI. Depender de ellos sería un supuesto, por lo que se evita.

### Atributo de configuración nuevo

| Atributo del portal | Unidad | Valor por defecto | Comportamiento ante valor ausente o inválido |
|---|---|---|---|
| `bps.servicio.busqueda.ia.salud.ttl.millis` | milisegundos | `60000` | Se registra en log y se usa el valor por defecto. Nunca interrumpe el arranque. |

- Se lee desde la configuración del portal (`PortalContext.getConfigAttribute`),
  siguiendo el mismo mecanismo ya usado para el endpoint
  (`bps.servicio.busqueda.ia.endpoint`).
- Un valor no numérico, vacío o `<= 0` cae en el valor por defecto.

### Cumplimiento de RF1.4 (no exponer información técnica)

- Al usuario **no** se le muestra el estado de las dependencias, ni códigos HTTP,
  ni trazas.
- Cuando la causa probable es un servicio degradado, se muestra un mensaje
  alternativo en lenguaje de usuario; en el resto de casos, el mensaje genérico
  ya existente.
- Se añade un **código de incidencia opaco** (p. ej. `BPS-BUS-<marca de
  tiempo>`): un identificador de correlación para atención de incidentes, **sin
  información técnica ni del usuario**, que éste puede mencionar a soporte y
  permite cruzarlo con los logs del servidor.

### Componentes nuevos

| Archivo | Rol |
|---|---|
| `Codigo/src/uy/gub/bps/busqueda/web/salud/EstadoSalud.java` | Enumeración `OPERATIVO` / `DEGRADADO` / `DESCONOCIDO`. |
| `Codigo/src/uy/gub/bps/busqueda/web/salud/CacheEstadoSalud.java` | Caché del estado con doble verificación, `volatile`, TTL parametrizable y comportamiento stale. Expone `TTL_MS_POR_DEFECTO`. |
| `Codigo/src/uy/gub/bps/busqueda/web/salud/ClasificadorSalud.java` | Sonda `/salud` vía `ClienteApiAws.sistema().salud()` y traduce el código HTTP a `EstadoSalud`. |

### Componentes modificados

| Archivo | Cambio |
|---|---|
| `Codigo/src/uy/gub/bps/busqueda/web/BuscarServlet.java` | Crea la caché al inicializar; en el `catch (ApiException)` clasifica y registra el error; resuelve el TTL desde el portal; publica los atributos `errorServicioNoDisponible` y `codigoIncidencia`. |
| `Codigo/WebContent/buscador/demo/busqueda.jsp` | Bloque de error: mensaje alternativo según clasificación y visualización del código de incidencia. |

### Observabilidad (logs)

- **Clasificación de error:** se registra `HTTP <status>`, `estadoServicio`,
  `codigoIncidencia` y el contexto de la búsqueda (`q`, `tipo`, `pagina`).
- **Configuración:** se registra cuando el TTL de `/salud` es inválido o ausente
  y se usa el valor por defecto.
- (Mediciones previas de tiempo de respuesta AWS y tiempo de render se
  mantienen; ver nota de medición más abajo.)

### Pendiente de verificar (no verificado en este entorno)

> Estas comprobaciones **deben realizarse en el entorno de ejecución**. No se
> declaran como hechas.

- **Compilación Java y traducción JSP**, que requieren Tomcat 8.0, el entorno
  Innova (`PortalContext`, `BPSUtils`) y el gateway.
- **Atributo de configuración del portal:** confirmar que
  `bps.servicio.busqueda.ia.salud.ttl.millis` existe y llega como texto parseable.
- **Comportamiento real de `/salud`** en el gateway (respuestas 200/503
  efectivas).
- **Concurrencia bajo carga:** confirmar que el número de sondeos por nodo y por
  ventana de TTL es el esperado y que ninguna petición de usuario queda
  bloqueada por un sondeo en curso.

### Riesgo residual

Bajo y acotado al **camino de error** de la búsqueda. El mayor cuidado está en
la concurrencia de la caché, mitigada por diseño (doble verificación + `volatile`
+ stale) pero **pendiente de validación** en el entorno.

---

## Nota 2 — Mediciones de tiempo (respuesta AWS y render)

- **Rama:** `feature/salud-clasificacion-errores` (originada en la iteración
  previa sobre `main`).
- **Estado:** implementado; **pendiente de verificación**.

### Cambios

- **`BuscarServlet.java`:** mide con `System.nanoTime()` el tiempo de la llamada
  al servicio de búsqueda en AWS y lo registra como
  `[medicion] tiempo_respuesta_aws_ms=...` junto con `q`, `tipo` y `pagina`.
- **`busqueda.jsp`:** el servlet publica `INICIO_RENDER_NS` justo antes del
  `forward`; la JSP calcula el tiempo transcurrido hasta el final del render y
  lo registra como `[medicion] tiempo_render_resultado_ms=...`.

### Limitación reconocida

- La medición de "render" abarca desde el inicio del `forward` hasta el final de
  la ejecución de la JSP; **no** incluye la serialización final de la respuesta
  al cliente por el contenedor. Medir hasta el último byte enviado requeriría un
  `Filter` (no implementado; evaluable como propuesta).

---

## Nota 3 — Manejo de excepciones en `BuscarServlet`

- **Rama:** `feature/salud-clasificacion-errores` (originada en la iteración
  previa sobre `main`).
- **Estado:** implementado.

### Cambios

- El bloque de inicialización reemplazó `printStackTrace()` por `log(...)` con
  contexto descriptivo.
- La ejecución de la búsqueda diferencia:
  - `catch (ApiException)`: error de integración con el servicio de búsqueda
    (HTTP >= 400 o fallo de red).
  - `catch (Exception)`: error inesperado.
- Ambos registran contexto (`q`, `tipo`, `pagina`) y **no exponen detalle técnico
  al usuario**, que sigue viendo un mensaje genérico (RF1.4).
