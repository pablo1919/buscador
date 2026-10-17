package uy.gub.bps.busqueda.web;

import innova.portal.PortalContext;
import innova.server.InnovaApplicationNotFoundException;
import innova.server.InnovaServerNotRunning;
import innova.server.TooManySessionException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import uy.gub.bps.busqueda.aws.api.ClienteApiAws;
import uy.gub.bps.busqueda.aws.api.config.ApiConfiguration;
import uy.gub.bps.busqueda.aws.api.dto.Paginado;
import uy.gub.bps.busqueda.aws.api.dto.ResultadoBusquedaResponse;
import uy.gub.bps.busqueda.aws.api.exception.ApiException;
import uy.gub.bps.busqueda.aws.api.model.busqueda.BusquedaQuery;
import uy.gub.bps.busqueda.aws.api.model.common.ApiResponse;
import uy.gub.bps.busqueda.web.mapper.BusquedaMapper;
import uy.gub.bps.busqueda.web.salud.CacheEstadoSalud;
import uy.gub.bps.busqueda.web.salud.ClasificadorSalud;
import uy.gub.bps.busqueda.web.salud.EstadoSalud;

public class BuscarServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String CACHE_CURSORES = "CACHE_CURSORES";
    private static final int RESULTADOS_POR_PAGINA = 10;
    private static final int MAX_RESULTADOS = 50;
    private static final String AMBITO = "general";
    private static final String ATTR_INICIO_RENDER_NS = "INICIO_RENDER_NS";
    private static final String ATTR_ERROR_SERVICIO = "errorServicioNoDisponible";
    private static final String ATTR_CODIGO_INCIDENCIA = "codigoIncidencia";
    private static final String PREFIJO_INCIDENCIA = "BPS-BUS";
    private static final String CONFIG_ENDPOINT = "bps.servicio.busqueda.ia.endpoint";
    private static final String CONFIG_TTL_SALUD_MS = "bps.servicio.busqueda.ia.salud.ttl.millis";
    private static String ENDPOINT_GATEWAY = "";

    private ClienteApiAws client;
    private CacheEstadoSalud cacheSalud;

    @Override
    public void init() throws ServletException {

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	try {
			PortalContext portalContext = new PortalContext(request, response);
			if (ENDPOINT_GATEWAY.equalsIgnoreCase("")){
				String endpoint = portalContext.getConfigAttribute(CONFIG_ENDPOINT);
				if (endpoint != null && !endpoint.equalsIgnoreCase("")){
					ENDPOINT_GATEWAY = endpoint;
					System.out.println("Seteo ENDPOINT_GATEWAY = "+ ENDPOINT_GATEWAY);
					client = new ClienteApiAws(new ApiConfiguration(ENDPOINT_GATEWAY));
					long ttlSaludMs = resolverTtlSaludMs(portalContext);
					cacheSalud = new CacheEstadoSalud(new ClasificadorSalud(client), ttlSaludMs);
				}
			}
		} catch (InnovaServerNotRunning e1) {
			log("Portal no disponible: InnovaServerNotRunning al resolver el endpoint de búsqueda", e1);
		} catch (InnovaApplicationNotFoundException e1) {
			log("Configuración no disponible: InnovaApplicationNotFoundException al resolver el endpoint de búsqueda", e1);
		} catch (TooManySessionException e1) {
			log("Sesiones excedidas: TooManySessionException al resolver el endpoint de búsqueda", e1);
		}

        String q = trim(request.getParameter("q"));
//        System.out.println("[BuscarServlet] q recibido=[" + q + "]");
//        for (int i = 0; i < q.length(); i++) {
//            System.out.println("  char[" + i + "]=" + q.charAt(i) + " codigo=" + (int) q.charAt(i));
//        }

        String tipo = trim(request.getParameter("tipo"));
        int paginaSolicitada = parsePagina(request.getParameter("pagina"));

        System.out.println("\n[BuscarServlet] === PETICIÓN === q=[" + q
                + "] tipo=[" + tipo
                + "] paginaSolicitada=" + paginaSolicitada);

        request.setAttribute("q", q);
        request.setAttribute("tipo", tipo);

        if (q.isEmpty()) {
            request.setAttribute("pagina", Integer.valueOf(1));
            request.setAttribute(ATTR_INICIO_RENDER_NS, Long.valueOf(System.nanoTime()));
            forward(request, response);
            return;
        }

        try {
            HttpSession session = request.getSession();
            @SuppressWarnings("unchecked")
            Map<String, String> cacheCursores = (Map<String, String>) session.getAttribute(CACHE_CURSORES);

            if (cacheCursores == null) {
                cacheCursores = new HashMap<String, String>();
                session.setAttribute(CACHE_CURSORES, cacheCursores);
            }

            String contextoBusqueda = q + "|" + tipo;
            String cacheKey = contextoBusqueda + "|" + paginaSolicitada;
            String cursor = null;
            int pagina = paginaSolicitada;

            if (paginaSolicitada > 1) {
                cursor = cacheCursores.get(cacheKey);

                // Un cursor sólo es válido dentro de la sesión y del contexto de búsqueda/filtro que lo originó.
                // Si no existe, no se puede reconstruir una página intermedia de forma segura.
                if (cursor == null || cursor.trim().isEmpty()) {
                    pagina = 1;
                    cursor = null;
                    cacheCursores.remove(contextoBusqueda + "|1");
                }
            }

            BusquedaQuery query = new BusquedaQuery(q, AMBITO)
                    .porPagina(Integer.valueOf(RESULTADOS_POR_PAGINA))
                    .maxResultados(Integer.valueOf(MAX_RESULTADOS));

            System.out.println("[BuscarServlet] ESTADO ANTES DE AWS:"
                    + " \npaginaSolicitada=" + paginaSolicitada
                    + " \npaginaEfectiva=" + pagina
                    + " \ncacheKey=" + cacheKey
                    + " \ncursorLeidoDeSesion=" + cursor
                    + " \nclavesEnSesion=" + cacheCursores.keySet());

            if (pagina == 1) {
                query.pagina(Integer.valueOf(1));
            } else {
                // Para páginas posteriores el cursor es la autoridad de navegación.
            	query.pagina(pagina);
                query.cursor(cursor);
            }

            if (!tipo.isEmpty()) {
                query.tipo(tipo);
            }

            query.verbose(false);

            long inicioAwsNs = System.nanoTime();
            ApiResponse apiResponse = client.busqueda().buscar(query);
            long duracionAwsMs = (System.nanoTime() - inicioAwsNs) / 1000000L;
            log("[medicion] tiempo_respuesta_aws_ms=" + duracionAwsMs
                    + " q=[" + q + "] tipo=[" + tipo + "] pagina=" + pagina);

            ResultadoBusquedaResponse resultado = BusquedaMapper.parse(apiResponse.getBody());

            //System.out.println("[BuscarServlet] BODY CRUDO DE AWS:");
            System.out.println(apiResponse.getBody());

            System.out.println("[BuscarServlet] RESPUESTA AWS:"
                    + " cursorDevuelto=" + (resultado.getPaginado() == null ? "null"
                                            : resultado.getPaginado().getCursor())
                    + " paginaDevuelta=" + (resultado.getPaginado() == null ? "null"
                                            : resultado.getPaginado().getPagina())
                    + " totalPaginas=" + (resultado.getPaginado() == null ? "null"
                                          : resultado.getPaginado().getTotalPaginas()));

            request.setAttribute("resultado", resultado);
            request.setAttribute("pagina", Integer.valueOf(pagina));
            guardarCursorSiguiente(cacheCursores, contextoBusqueda, pagina, resultado);

        } catch (ApiException e) {
            // Error de integración con el servicio de búsqueda (HTTP >= 400 o fallo de red).
            // No se expone detalle técnico al usuario; queda registrado para diagnóstico.
            request.setAttribute("errorBusqueda", Boolean.TRUE);
            request.setAttribute("pagina", Integer.valueOf(paginaSolicitada));
            clasificarYRegistrarError(request, e, q, tipo, paginaSolicitada);
        } catch (Exception e) {
            request.setAttribute("errorBusqueda", Boolean.TRUE);
            request.setAttribute("pagina", Integer.valueOf(paginaSolicitada));
            log("[BuscarServlet] Error inesperado al ejecutar la búsqueda"
                    + " q=[" + q + "] tipo=[" + tipo + "] pagina=" + paginaSolicitada, e);
        }

        request.setAttribute(ATTR_INICIO_RENDER_NS, Long.valueOf(System.nanoTime()));
        forward(request, response);
    }

    /**
     * Clasifica el error de integración consultando (con caché) el estado del
     * servicio mediante /salud, y registra la información para diagnóstico.
     * Al usuario se le expone únicamente un código de incidencia opaco: nunca
     * el estado técnico de las dependencias (RF1.4).
     */
    private void clasificarYRegistrarError(HttpServletRequest request,
                                           ApiException e,
                                           String q,
                                           String tipo,
                                           int paginaSolicitada) {
        EstadoSalud estadoServicio = EstadoSalud.DESCONOCIDO;
        if (cacheSalud != null) {
            estadoServicio = cacheSalud.obtener();
        }

        String codigoIncidencia = generarCodigoIncidencia();
        request.setAttribute(ATTR_CODIGO_INCIDENCIA, codigoIncidencia);
        request.setAttribute(ATTR_ERROR_SERVICIO,
                Boolean.valueOf(estadoServicio == EstadoSalud.DEGRADADO));

        log("[BuscarServlet] Error de integración con el servicio de búsqueda"
                + " (HTTP " + e.getStatusCode() + ")"
                + " estadoServicio=" + estadoServicio
                + " codigoIncidencia=" + codigoIncidencia
                + " q=[" + q + "] tipo=[" + tipo + "] pagina=" + paginaSolicitada, e);
    }

    /**
     * Código opaco de correlación para atención de incidentes. No contiene
     * información técnica ni del usuario: sólo un identificador con marca de
     * tiempo para que el usuario lo mencione a soporte y pueda cruzarse con
     * los logs del servidor.
     */
    private static String generarCodigoIncidencia() {
        return PREFIJO_INCIDENCIA + "-" + System.currentTimeMillis();
    }

    /**
     * Resuelve el TTL (en milisegundos) de la caché de /salud desde la
     * configuración del portal. Si el atributo no está definido o no es un
     * entero positivo válido, se registra y se usa el valor por defecto.
     * Nunca lanza: una mala configuración no debe impedir el arranque.
     */
    private long resolverTtlSaludMs(PortalContext portalContext) {
        try {
            String valor = portalContext.getConfigAttribute(CONFIG_TTL_SALUD_MS);
            if (valor == null || valor.trim().isEmpty()) {
                return CacheEstadoSalud.TTL_MS_POR_DEFECTO;
            }
            long ttlMs = Long.parseLong(valor.trim());
            if (ttlMs > 0) {
                return ttlMs;
            }
            log("[BuscarServlet] TTL de /salud inválido (=" + valor + "); se usa el valor por defecto "
                    + CacheEstadoSalud.TTL_MS_POR_DEFECTO + " ms");
        } catch (NumberFormatException e) {
            log("[BuscarServlet] TTL de /salud no numérico; se usa el valor por defecto "
                    + CacheEstadoSalud.TTL_MS_POR_DEFECTO + " ms", e);
        } catch (Exception e) {
            log("[BuscarServlet] No se pudo leer el TTL de /salud; se usa el valor por defecto "
                    + CacheEstadoSalud.TTL_MS_POR_DEFECTO + " ms", e);
        }
        return CacheEstadoSalud.TTL_MS_POR_DEFECTO;
    }

    private void guardarCursorSiguiente(Map<String, String> cacheCursores,
                                        String contextoBusqueda,
                                        int paginaActual,
                                        ResultadoBusquedaResponse resultado) {
        if (resultado == null || resultado.getPaginado() == null) {
            return;
        }

        Paginado paginado = resultado.getPaginado();
        String cursorSiguiente = paginado.getCursor();
        String claveSiguiente = contextoBusqueda + "|" + (paginaActual + 1);

        if (cursorSiguiente != null && !cursorSiguiente.trim().isEmpty()) {
            cacheCursores.put(claveSiguiente, cursorSiguiente);
        } else {
            cacheCursores.remove(claveSiguiente);
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/buscador/demo/busqueda.jsp").forward(request, response);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static int parsePagina(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 1;
        }

        try {
            int pagina = Integer.parseInt(value.trim());
            return pagina > 0 ? pagina : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
