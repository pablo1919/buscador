package uy.gub.bps.busqueda.aws.api.service;

import uy.gub.bps.busqueda.aws.api.client.BaseRestClient;
import uy.gub.bps.busqueda.aws.api.exception.ApiException;
import uy.gub.bps.busqueda.aws.api.model.common.ApiResponse;

public class SistemaService {
	private final BaseRestClient client;

	public SistemaService(BaseRestClient c) {
		client = c;
	}

	public ApiResponse salud() throws ApiException {
		return client.get("/salud");
	}

	public ApiResponse openApi() throws ApiException {
		return client.get("/openapi.json");
	}
}
