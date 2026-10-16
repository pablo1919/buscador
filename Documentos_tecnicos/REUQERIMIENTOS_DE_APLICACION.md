# Requerimiento Funcional: Búsqueda de Contenidos Institucionales

## 1. Descripción General
Construir la interfaz web de búsqueda de contenidos dentro del sitio web institucional. La solución permitirá realizar búsquedas por texto libre, recibir sugerencias en tiempo real mediante *search-as-you-type*, filtrar los resultados por tipo de contenido y navegar dinámicamente por las páginas de resultados.

---

## 2. Integración Backend y Mapeo de Datos
* **Servicio:** REST en AWS consumido a través del API Gateway corporativo.
* **Firma HMAC:** Se asume implementada en el Gateway corporativo, según la especificación del API REST.
* **Gateway de desarrollo local:** `http://localhost:3001`.
* **Mapeo de Tipos de Contenido:** Los tipos devueltos por el backend en el atributo `paginado.por_tipo` de la respuesta JSON deben agruparse, contabilizarse y mostrarse en la interfaz utilizando los siguientes alias plurales para los filtros:

| Tipo Backend (`por_tipo`) | Alias UI para filtros |
|---|---|
| `servicioenlinea` | Servicios en línea |
| `folder` | Información |
| `tramite` | Trámites |
| `text` | Información |
| `preguntaFrecuente` | Preguntas frecuentes |
| `download` | Archivos descargables |
| `noticia` | Noticias |
| `htmlfragmenter` | Información |
| `link` | Información |
| `superservicio` | Superservicios |
| `supertramite` | Supertrámites |
| `publicidad` | Publicidad |

* **Mapeo de resultados:** Cada resultado utiliza el alias singular correspondiente a su tipo.
* **URL de detalle:** La URL que debe utilizarse para navegar al contenido es `url_canonica`.

---

## 3. Criterios de Aceptación (Frontend)

### RF1 - Búsqueda Principal, Resultados y Navegación

#### RF1.1 - Formulario y Lista de Resultados
* **Formulario:** Debe contener un campo de entrada de texto y un botón de búsqueda (permite ejecución mediante la tecla `Enter`).
* **Lista de Resultados:** Presentación vertical de los ítems con los siguientes atributos:
  * Título
  * Alias singular relativo al tipo de contenido (etiqueta UI)
  * Descripción / fragmento
  * Resumen IA (si está presente en el JSON)
  * Fecha de última actualización
* **Navegación a Detalle:** El resultado debe permitir navegar a la dirección indicada en `url_canonica` del objeto JSON.

#### RF1.2 - Panel de Filtros
* **Ubicación:** Ubicado en la columna izquierda respecto al área principal de resultados.
* **Contadores:** Debe consolidar los tipos del backend según el mapeo UI y desplegar la cantidad total de resultados por cada alias plural.
* **Filtrado:** Al seleccionar un tipo de contenido, se debe realizar una nueva consulta al backend enviando el código o códigos de tipo correspondientes. No se debe filtrar únicamente el conjunto de resultados de la página ya cargada.
* **Tipos agrupados:** Cuando varios tipos backend comparten un alias (por ejemplo `folder`, `text`, `htmlfragmenter` y `link` → `Información`), el filtro debe enviar todos los códigos correspondientes, separados por coma, según el contrato del API.

#### RF1.3 - Paginación
* **Mecanismo:** Controlado mediante el atributo `cursor` proporcionado por la respuesta JSON del servicio REST.
* **Autoridad:** El `cursor` es la autoridad para recuperar una página concreta. El número de página se utiliza únicamente como representación de posición en la interfaz.
* **Navegación:**
  * Incluye botones **"Primera página"**, **"Anterior"** y **"Siguiente"**.
  * **"Primera página"** reinicia la navegación sin cursor.
  * **"Anterior"** se deshabilita cuando se está en la primera página.
  * **"Siguiente"** se deshabilita cuando el atributo `cursor` devuelto sea nulo/vacío o no existan más páginas disponibles.
  * Los cursores deben conservarse por contexto de búsqueda y filtro para permitir volver a páginas previamente consultadas.

#### RF1.4 - Estados de la Interfaz
* **Estado Inicial:** Muestra un mensaje/pantalla con texto de bienvenida orientativo.
* **Carga (Loading):** Despliega un indicador visual de carga durante la ejecución de consultas a la API.
* **Sin Resultados:** Presenta un mensaje informativo cuando la búsqueda devuelva 0 ítems.
* **Error:** Muestra únicamente un mensaje genérico de error ante fallas de conexión o del API Gateway; no debe exponerse información técnica al usuario.

---

### RF2 - Búsqueda en Tiempo Real (Search-As-You-Type / Autocompletado)

#### RF2.1 - Descripción
Permite al usuario recibir sugerencias de contenido en un menú desplegable mientras escribe en el campo de búsqueda, sin necesidad de activar el botón "Buscar" ni recargar la página.

#### RF2.2 - Comportamiento Técnico en Frontend
* **Umbral Mínimo:** La consulta de sugerencias a la API se dispara a partir de **3 o más caracteres** ingresados.
* **Control de Frecuencia (Debounce):** Aplica un *debounce* de **300 ms** desde la última pulsación de tecla antes de enviar la solicitud al API Gateway.
* **Cancelación de Peticiones (AbortController):** Si el usuario continúa escribiendo mientras hay una consulta en curso, la petición previa se aborta automáticamente para procesar solo el último término.

#### RF2.3 - Interfaz y Navegación
* **Desplegable (Dropdown):** Lista flotante posicionada inmediatamente debajo del campo de búsqueda.
* **Atributos por Sugerencia:** Cada ítem muestra su **Título** y la etiqueta correspondiente de **Tipo de contenido (Alias UI)**.
* **Interacción y Teclado:**
  * Uso de teclas `Flecha Abajo` y `Flecha Arriba` para desplazarse entre las sugerencias.
  * Presionar `Enter` sobre una sugerencia seleccionada navega directamente a su `url` o autocompleta el término.
  * Presionar `Escape` o hacer clic fuera del componente cierra el menú desplegable.
