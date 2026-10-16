package uy.gub.bps.busqueda.web.mapper;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import uy.gub.bps.busqueda.aws.api.dto.Filtro;
import uy.gub.bps.busqueda.aws.api.dto.Paginado;
import uy.gub.bps.busqueda.aws.api.dto.ResultadoBusquedaItem;
import uy.gub.bps.busqueda.aws.api.dto.ResultadoBusquedaResponse;

public final class BusquedaMapper {

    private BusquedaMapper() {
    }

    public static ResultadoBusquedaResponse parse(String json) throws Exception {
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse(new StringReader(json));

        ResultadoBusquedaResponse response = new ResultadoBusquedaResponse();
        response.setResultados(parseResultados(root));
        response.setPaginado(parsePaginado(root));
        return response;
    }

    private static List<ResultadoBusquedaItem> parseResultados(JSONObject root) {
        List<ResultadoBusquedaItem> items = new ArrayList<ResultadoBusquedaItem>();
        JSONArray resultados = (JSONArray) root.get("resultados");

        if (resultados == null) {
            return items;
        }

        for (Object obj : resultados) {
            JSONObject itemJson = (JSONObject) obj;
            ResultadoBusquedaItem item = new ResultadoBusquedaItem();

            String tipo = (String) itemJson.get("tipo");
            item.setId((String) itemJson.get("id"));
            item.setTipo(tipo);
            item.setEtiqueta(obtenerLabelResultado(tipo));
            item.setTitulo((String) itemJson.get("titulo"));
            item.setUrl((String) itemJson.get("url"));
            item.setUrlCanonica((String) itemJson.get("url_canonica"));
            item.setResumen((String) itemJson.get("resumen"));
            item.setFechaActualizacion((String) itemJson.get("fechaActualizacion"));
            item.setFragmento((String) itemJson.get("fragmento"));
            items.add(item);
        }

        return items;
    }

    private static Paginado parsePaginado(JSONObject root) {
        JSONObject json = (JSONObject) root.get("paginado");

        if (json == null) {
            return null;
        }

        Paginado paginado = new Paginado();
        paginado.setPagina(toInteger(json.get("pagina")));
        paginado.setPorPagina(toInteger(json.get("por_pagina")));
        paginado.setTotal(toInteger(json.get("total")));
        paginado.setTotalPaginas(toInteger(json.get("total_paginas")));
        paginado.setMaxResultados(toInteger(json.get("max_resultados")));
        paginado.setCursor((String) json.get("cursor"));
        parsePorTipo(json, paginado);

        return paginado;
    }

    private static void parsePorTipo(JSONObject paginadoJson, Paginado paginado) {
        JSONObject porTipoJson = (JSONObject) paginadoJson.get("por_tipo");

        if (porTipoJson == null) {
            return;
        }

        Map<String, Integer> porTipo = new HashMap<String, Integer>();
        List<Filtro> filtros = new ArrayList<Filtro>();

        for (Object keyObj : porTipoJson.keySet()) {
            String codigo = String.valueOf(keyObj);
            Integer cantidad = toInteger(porTipoJson.get(codigo));
            porTipo.put(codigo, cantidad);

            Filtro filtro = new Filtro();
            filtro.setCodigo(codigo);
            filtro.setCantidad(cantidad);
            filtro.setLabel(obtenerLabelFiltro(codigo));
            filtros.add(filtro);
        }

        paginado.setPorTipo(porTipo);
        paginado.setFiltros(filtros);
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return ((Long) value).intValue();
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        return Integer.valueOf(String.valueOf(value));
    }

    /** Labels singulares utilizados en cada resultado. */
    private static String obtenerLabelResultado(String codigo) {
        if (codigo == null) return null;

        switch (codigo) {
            case "tramite": return "Trámite";
            case "preguntaFrecuente": return "Pregunta frecuente";
            case "noticia": return "Noticia";
            case "download": return "Archivo descargable";
            case "htmlfragmenter": return "Información";
            case "folder": return "Información";
            case "link": return "Información";
            case "text": return "Información";
            case "servicioenlinea": return "Servicio en línea";
            case "superservicio": return "Superservicio";
            case "supertramite": return "Supertrámite";
            case "publicidad": return "Publicidad";
            default: return codigo;
        }
    }

    /** Labels plurales utilizados en el panel de filtros. */
    private static String obtenerLabelFiltro(String codigo) {
        if (codigo == null) return null;

        switch (codigo) {
            case "tramite": return "Trámites";
            case "preguntaFrecuente": return "Preguntas frecuentes";
            case "noticia": return "Noticias";
            case "download": return "Archivos descargables";
            case "htmlfragmenter": return "Información";
            case "folder": return "Información";
            case "link": return "Información";
            case "text": return "Información";
            case "servicioenlinea": return "Servicios en línea";
            case "superservicio": return "Superservicios";
            case "supertramite": return "Supertrámites";
            case "publicidad": return "Publicidad";
            default: return codigo;
        }
    }
}
