package uy.gub.bps.busqueda.aws.api.service;

import uy.gub.bps.busqueda.aws.api.client.BaseRestClient;
import uy.gub.bps.busqueda.aws.api.exception.ApiException;
import uy.gub.bps.busqueda.aws.api.model.common.ApiResponse;
import uy.gub.bps.busqueda.aws.api.util.UrlUtils;

public class AdministracionService {
	private final BaseRestClient client;

	public AdministracionService(BaseRestClient c) {
		client = c;
	}

	public ApiResponse pesos() throws ApiException {
		return client.get("/api/v1/pesos");
	}

	public ApiResponse reemplazarPesos(String json) throws ApiException {
		return client.put("/api/v1/pesos", json);
	}

	public ApiResponse glosario() throws ApiException {
		return client.get("/api/v1/glosario");
	}

	public ApiResponse guardarGlosario(String json) throws ApiException {
		return client.put("/api/v1/glosario", json);
	}

	public ApiResponse borrarGlosario(String json) throws ApiException {
		return client.delete("/api/v1/glosario", json);
	}

	public ApiResponse destacados() throws ApiException {
		return client.get("/api/v1/destacados");
	}

	public ApiResponse guardarDestacado(String id, String json)
			throws ApiException {
		return client.put("/api/v1/destacados/" + UrlUtils.encode(id), json);
	}

	public ApiResponse borrarDestacado(String id) throws ApiException {
		return client.delete("/api/v1/destacados/" + UrlUtils.encode(id), null);
	}
}
