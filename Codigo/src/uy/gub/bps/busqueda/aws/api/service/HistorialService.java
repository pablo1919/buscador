package uy.gub.bps.busqueda.aws.api.service;

import java.util.*;

import uy.gub.bps.busqueda.aws.api.client.BaseRestClient;
import uy.gub.bps.busqueda.aws.api.exception.ApiException;
import uy.gub.bps.busqueda.aws.api.model.common.ApiResponse;
import uy.gub.bps.busqueda.aws.api.util.UrlUtils;

public class HistorialService {
	private final BaseRestClient client;

	public HistorialService(BaseRestClient c) {
		client = c;
	}

	public ApiResponse porDia(String dia, Integer limite) throws ApiException {
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put("dia", dia);
		p.put("limite", limite);
		return client.get("/api/v1/historial" + UrlUtils.query(p));
	}

	public ApiResponse porContenido(String id, Integer limite) throws ApiException {
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put("limite", limite);
		return client.get("/api/v1/historial/" + UrlUtils.encode(id) + UrlUtils.query(p));
	}
}
