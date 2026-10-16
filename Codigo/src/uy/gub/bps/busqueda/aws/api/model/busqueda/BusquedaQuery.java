package uy.gub.bps.busqueda.aws.api.model.busqueda;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import uy.gub.bps.busqueda.aws.api.util.UrlUtils;

public final class BusquedaQuery {
	private final String query, searchScope;
	private Integer pagina, porPagina, maxResultados;
	private String tipo, cursor;
	private boolean verbose;

	public BusquedaQuery(String query, String searchScope) {
		if (query == null || query.trim().isEmpty())
			throw new IllegalArgumentException("query es obligatorio");
		if (searchScope == null || searchScope.trim().isEmpty())
			throw new IllegalArgumentException("searchScope es obligatorio");
		this.query = query;
		this.searchScope = searchScope;
	}

	public BusquedaQuery pagina(Integer v) {
		pagina = v;
		return this;
	}

	public BusquedaQuery porPagina(Integer v) {
		porPagina = v;
		return this;
	}

	public BusquedaQuery maxResultados(Integer v) {
		maxResultados = v;
		return this;
	}

	public BusquedaQuery tipo(String v) {
		tipo = v;
		return this;
	}

	public BusquedaQuery cursor(String v) {
		cursor = v;
		return this;
	}

	public BusquedaQuery verbose(boolean v) {
		verbose = v;
		return this;
	}

	public String toQueryString() {
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put("query", urlEncode(query));
		p.put("searchScope", urlEncode(searchScope));
		p.put("pagina", pagina);
		p.put("por_pagina", porPagina);
		p.put("max_resultados", maxResultados);
		p.put("tipo", urlEncode(tipo));
		p.put("cursor", urlEncode(cursor));
		p.put("verbose", verbose);
		return UrlUtils.query(p);
	}
	
    private static String urlEncode(String value) {
        try {
        	if (value != null){
        		value = URLEncoder.encode(value, "UTF-8");
        	}
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 no está disponible", e);
        }
		return value;
    } 	
}
