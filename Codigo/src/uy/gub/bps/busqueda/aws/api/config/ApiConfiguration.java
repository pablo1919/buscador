package uy.gub.bps.busqueda.aws.api.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiConfiguration {
	private final String baseUrl;
	private final int connectTimeoutMillis;
	private final int readTimeoutMillis;
	private final Map<String, String> defaultHeaders;

	public ApiConfiguration(String baseUrl) {
		this(baseUrl, 15000, 30000, Collections.<String, String> emptyMap());
	}

	public ApiConfiguration(
			String baseUrl, 
			int connectTimeoutMillis,
			int readTimeoutMillis, 
			Map<String, String> headers) 
	{
		if (baseUrl == null || baseUrl.trim().isEmpty())
			throw new IllegalArgumentException("baseUrl es obligatoria");
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0,	baseUrl.length() - 1) : baseUrl;
		this.connectTimeoutMillis = connectTimeoutMillis;
		this.readTimeoutMillis = readTimeoutMillis;
		this.defaultHeaders = Collections.unmodifiableMap(new LinkedHashMap<String, String>(headers));
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public int getConnectTimeoutMillis() {
		return connectTimeoutMillis;
	}

	public int getReadTimeoutMillis() {
		return readTimeoutMillis;
	}

	public Map<String, String> getDefaultHeaders() {
		return defaultHeaders;
	}
}
