package uy.gub.bps.busqueda.aws.api.service;

import uy.gub.bps.busqueda.aws.api.client.BaseRestClient;
import uy.gub.bps.busqueda.aws.api.exception.ApiException;
import uy.gub.bps.busqueda.aws.api.model.common.ApiResponse;
import uy.gub.bps.busqueda.aws.api.util.UrlUtils;

public class IndexacionService {
	private final BaseRestClient client;

	public IndexacionService(BaseRestClient c) {
		client = c;
	}

	public ApiResponse indexar(String json) throws ApiException {
		return client.post("/api/v1/indexar", json);
	}

	public ApiResponse estado(String jobId) throws ApiException {
		return client.get("/api/v1/indexar/" + UrlUtils.encode(jobId));
	}

	public ApiResponse items(String jobId) throws ApiException {
		return client.get("/api/v1/indexar/" + UrlUtils.encode(jobId) + "/items");
	}

	public ApiResponse dlq() throws ApiException {
		return client.get("/api/v1/dlq");
	}
}
