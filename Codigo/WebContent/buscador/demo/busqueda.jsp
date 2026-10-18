<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="cabezal.jspf" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="uy.gub.bps.bpsutils.BPSUtils" %>
<%@ page import="uy.gub.bps.busqueda.aws.api.dto.Filtro" %>
<%@ page import="uy.gub.bps.busqueda.aws.api.dto.Paginado" %>
<%@ page import="uy.gub.bps.busqueda.aws.api.dto.ResultadoBusquedaItem" %>
<%@ page import="uy.gub.bps.busqueda.aws.api.dto.ResultadoBusquedaResponse" %>

<%!
    private String htmlEscape(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&': escaped.append("&amp;"); break;
                case '<': escaped.append("&lt;"); break;
                case '>': escaped.append("&gt;"); break;
                case '"': escaped.append("&quot;"); break;
                case '\'': escaped.append("&#39;"); break;
                default: escaped.append(c);
            }
        }
        return escaped.toString();
    }

    /**
     * El servicio de búsqueda sólo devuelve <em> como HTML controlado.
     * El resto del contenido se escapa para evitar interpretar HTML arbitrario.
     */
    private String fragmentoSeguro(String value) {
        if (value == null) {
            return "";
        }

        final String marcadorApertura = "__BPS_EM_APERTURA__";
        final String marcadorCierre = "__BPS_EM_CIERRE__";

        String protegido = value
                .replace("<em>", marcadorApertura)
                .replace("</em>", marcadorCierre);

        return htmlEscape(protegido)
                .replace(marcadorApertura, "<em>")
                .replace(marcadorCierre, "</em>");
    }

    private boolean mismosTipos(String a, String b) {
        return tipos(a).equals(tipos(b));
    }

    private Set<String> tipos(String value) {
        Set<String> result = new HashSet<String>();
        if (value == null || value.trim().isEmpty()) {
            return result;
        }
        String[] values = value.split(",");
        for (String item : values) {
            String tipo = item.trim();
            if (!tipo.isEmpty()) {
                result.add(tipo);
            }
        }
        return result;
    }
%>

<%
    String q = (String) request.getAttribute("q");
    String tipo = (String) request.getAttribute("tipo");
    Integer paginaAttr = (Integer) request.getAttribute("pagina");
    ResultadoBusquedaResponse responseObj = (ResultadoBusquedaResponse) request.getAttribute("resultado");
    Boolean errorBusqueda = (Boolean) request.getAttribute("errorBusqueda");
    q = q == null ? "" : q;
    tipo = tipo == null ? "" : tipo;
    int pagina = paginaAttr == null ? 1 : paginaAttr.intValue();
%>

<div class="container">
    <main class="results-area" aria-labelledby="tituloResultados">
		<div id="estadoCarga"
		     class="estado-loading-overlay"
		     role="status"
		     aria-live="polite"
		     hidden=true>
		    <span class="spinner" aria-hidden="true"></span>
		    <span>Buscando...</span>
		</div>

        <% if (errorBusqueda != null && errorBusqueda.booleanValue()) { %>
            <div class="result-item estado estado-error" role="alert">
                <p>Ocurrió un error al buscar, por favor intente más tarde.</p>
            </div>
        <% } else if (q.isEmpty()) { %>
            <div class="result-item estado estado-idle">
                <h2 id="tituloResultados">Buscar contenidos</h2>
                <p>Ingrese uno o más términos para comenzar la búsqueda.</p>
            </div>
        <% } else if (responseObj != null && responseObj.getResultados() != null) { %>
            <h2 id="tituloResultados" class="visually-hidden">Resultados de búsqueda</h2>

            <% if (responseObj.getResultados().isEmpty()) { %>
                <div class="result-item estado estado-empty" role="status">
                    <p>No se encontraron resultados para la búsqueda o filtro seleccionado.</p>
                </div>
            <% } else { %>
                <% for (ResultadoBusquedaItem item : responseObj.getResultados()) { %>
                    <article class="result-item">
                        <h2 class="result-title">
                            <a href="<%= htmlEscape(item.getUrlCanonica()) %>">
                                <%= htmlEscape(item.getTitulo()) %>
                            </a>
                        </h2>
                        <div class="result-meta">
                            <strong>Tipo:</strong> <%= htmlEscape(item.getEtiqueta()) %><br />
                            <span>tipo Innova: <%=item.getTipo()%></span><br />
                            <span>id: <%= htmlEscape(item.getId()) %></span>
                        </div>

                        <% if (item.getFragmento() != null && !item.getFragmento().isEmpty()) { %>
                            <p class="result-desc">
                                <strong>Fragmento:</strong> <%= fragmentoSeguro(item.getFragmento()) %>
                            </p>
                        <% } %>

                        <% if (item.isResumenDistintoFragmento()) {
                        	System.out.println("\nid:"+ item.getId());
                        	System.out.println("resumen:\n"+item.getResumen());
                        	System.out.println("fragmento:\n"+item.getFragmentoSinHtml());%>
                            <div class="resumen-ia">
                                <strong>Resumen:</strong> <%= htmlEscape(item.getResumen()) %>
                            </div>
                        <% } %>

                        <% if (item.getFechaActualizacion() != null && !item.getFechaActualizacion().isEmpty()) { %>
                            <div class="fecha-actualizacion">
                                Última modificación: <%= htmlEscape(BPSUtils.formatearFecha(item.getFechaActualizacion())) %>
                            </div>
                        <% } %>
                    </article>
                <% } %>
            <% } %>

            <% Paginado paginado = responseObj.getPaginado(); %>
            <% if (paginado != null) {
                // La página visible la determina la navegación del servlet;
                // con cursor, paginado.pagina de la API no es la autoridad.
                int paginaActual = pagina;
                Integer totalPaginasAttr = paginado.getTotalPaginas();
                int totalPaginas = totalPaginasAttr == null ? paginaActual : totalPaginasAttr.intValue();
                boolean primera = paginaActual <= 1;
                boolean ultima = totalPaginas > 0 && paginaActual >= totalPaginas;
                boolean hayCursorSiguiente = paginado.getCursor() != null && !paginado.getCursor().trim().isEmpty();
            %>
                <nav class="pagination" aria-label="Paginación de resultados">
                    <button type="button"
                            class="pagina-control"
                            data-pagina="1"
                            <%= primera ? "disabled" : "" %>>
                        &laquo; Primera página
                    </button>

                    <button type="button"
                            class="pagina-control"
                            data-pagina="<%= Math.max(1, paginaActual - 1) %>"
                            <%= primera ? "disabled" : "" %>>
                        &lsaquo; Anterior
                    </button>

                    <span aria-current="page">
                        Página <%= paginaActual %> de <%= totalPaginas %>
                    </span>

                    <button type="button"
                            class="pagina-control"
                            data-pagina="<%= paginaActual + 1 %>"
                            <%= (ultima || !hayCursorSiguiente) ? "disabled" : "" %>>
                        Siguiente &rsaquo;
                    </button>
                </nav>
            <% } %>
        <% } %>
    </main>

    <aside class="sidebar" aria-labelledby="tituloFiltros">
        <h2 id="tituloFiltros">Filtros por tipo</h2>

        <% if (responseObj != null && responseObj.getPaginado() != null
                && responseObj.getPaginado().getFiltros() != null) {

            Map<String, Integer> conteoPorLabel = new LinkedHashMap<String, Integer>();
            Map<String, String> codigosPorLabel = new LinkedHashMap<String, String>();

            for (Filtro filtro : responseObj.getPaginado().getFiltros()) {
                String label = filtro.getLabel();
                int cantidad = filtro.getCantidad() == null ? 0 : filtro.getCantidad().intValue();
                Integer acumulado = conteoPorLabel.get(label);
                conteoPorLabel.put(label, acumulado == null ? cantidad : acumulado.intValue() + cantidad);

                String codigo = filtro.getCodigo();
                String codigos = codigosPorLabel.get(label);
                codigosPorLabel.put(label, codigos == null || codigos.isEmpty()
                        ? codigo
                        : codigos + "," + codigo);
            }
        %>
            <button type="button"
                    class="filtro-item <%= tipo.isEmpty() ? "active" : "" %>"
                    data-tipo="">
                Todos los tipos
            </button>

            <% for (Map.Entry<String, Integer> entry : conteoPorLabel.entrySet()) {
                String labelFiltro = entry.getKey();
                String codigosFiltro = codigosPorLabel.get(labelFiltro);
                boolean activo = codigosFiltro != null && mismosTipos(codigosFiltro, tipo);
            %>
                <button type="button"
                        class="filtro-item <%= activo ? "active" : "" %>"
                        data-tipo="<%= htmlEscape(codigosFiltro) %>">
                    <%= htmlEscape(labelFiltro) %> (<%= entry.getValue() %>)
                </button>
            <% } %>
        <% } else if (q.isEmpty()) { %>
            <p>Realice una búsqueda para ver los filtros.</p>
                <% } %>
        <a href="/bps/buscador/demo/busqueda.jsp">reiniciar</a>
    </aside>
</div>

<%
    // Medición del tiempo de render de la vista.
    // El servlet registra INICIO_RENDER_NS justo antes del forward; aquí se
    // calcula el tiempo transcurrido hasta el final del render de la JSP.
    Object inicioRenderNsObj = request.getAttribute("INICIO_RENDER_NS");
    if (inicioRenderNsObj instanceof Long) {
        long duracionRenderMs = (System.nanoTime() - ((Long) inicioRenderNsObj).longValue()) / 1000000L;
        pageContext.getServletContext().log(
                "[medicion] tiempo_render_resultado_ms=" + duracionRenderMs
                + " q=[" + q + "] tipo=[" + tipo + "] pagina=" + pagina);
    }
%>

<%@ include file="pie.jspf" %>
