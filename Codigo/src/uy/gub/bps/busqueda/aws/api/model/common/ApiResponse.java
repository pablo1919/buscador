package uy.gub.bps.busqueda.aws.api.model.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ApiResponse {
	private final int statusCode;
	private final String body;
	private final Map<String, List<String>> headers;

	public ApiResponse(int s, String b, Map<String, List<String>> h) {
		statusCode = s;
		body = b;
		headers = h == null ? Collections.<String, List<String>> emptyMap()
				: Collections.unmodifiableMap(h);
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getBody() {
		return body;
	}

	public Map<String, List<String>> getHeaders() {
		return headers;
	}

	public boolean isSuccessful() {
		return statusCode >= 200 && statusCode < 300;
	}

	@Override
	public String toString() {
		return "ApiResponse [statusCode=" + statusCode + ", body=" + body
				+ ", headers=" + headers + ", getStatusCode()="
				+ getStatusCode() + ", getBody()=" + getBody()
				+ ", getHeaders()=" + getHeaders() + ", isSuccessful()="
				+ isSuccessful() + "]";
	}
	
	
}
