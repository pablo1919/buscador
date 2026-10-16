package uy.gub.bps.busqueda.aws.api.service;

import uy.gub.bps.busqueda.aws.api.client.BaseRestClient;
import uy.gub.bps.busqueda.aws.api.exception.ApiException;
import uy.gub.bps.busqueda.aws.api.model.busqueda.BusquedaQuery;
import uy.gub.bps.busqueda.aws.api.model.busqueda.PredictivaQuery;
import uy.gub.bps.busqueda.aws.api.model.common.ApiResponse;

public class BusquedaService {
	private final BaseRestClient client;

	public BusquedaService(BaseRestClient c) {
		client = c;
	}

	public ApiResponse buscar(BusquedaQuery q) throws ApiException {
		return client.get("/api/v1/buscar" + q.toQueryString());
	}

	public ApiResponse predictiva(PredictivaQuery q) throws ApiException {
		return client.get("/api/v1/predictiva" + q.toQueryString());
	}
}
