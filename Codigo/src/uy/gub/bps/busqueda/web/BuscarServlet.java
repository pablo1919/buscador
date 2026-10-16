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
import uy.gub.bps.busqueda.aws.api.model.busqueda.BusquedaQuery;
import uy.gub.bps.busqueda.aws.api.model.common.ApiResponse;
import uy.gub.bps.busqueda.web.mapper.BusquedaMapper;

public class BuscarServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String CACHE_CURSORES = "CACHE_CURSORES";
    private static final int RESULTADOS_POR_PAGINA = 10;
    private static final int MAX_RESULTADOS = 50;
    private static final String AMBITO = "general";
    private static String ENDPOINT_GATEWAY = "";

    private ClienteApiAws client;

    @Override
    public void init() throws ServletException {

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	try {
			PortalContext portalContext = new PortalContext(request, response);
			if (ENDPOINT_GATEWAY.equalsIgnoreCase("")){
				String endpoint = portalContext.getConfigAttribute("bps.servicio.busqueda.ia.endpoint");
				if (endpoint != null && !endpoint.equalsIgnoreCase("")){
					ENDPOINT_GATEWAY = endpoint;
					System.out.println("Seteo ENDPOINT_GATEWAY = "+ ENDPOINT_GATEWAY);
					client = new ClienteApiAws(new ApiConfiguration(ENDPOINT_GATEWAY));
				}
			}
		} catch (InnovaServerNotRunning e1) {
			e1.printStackTrace();
		} catch (InnovaApplicationNotFoundException e1) {
			e1.printStackTrace();
		} catch (TooManySessionException e1) {
			e1.printStackTrace();
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
            ApiResponse apiResponse = client.busqueda().buscar(query);
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

        } catch (Exception e) {
            request.setAttribute("errorBusqueda", Boolean.TRUE);
            request.setAttribute("pagina", Integer.valueOf(paginaSolicitada));
            log("Error al ejecutar la búsqueda", e);
        }

        forward(request, response);
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
