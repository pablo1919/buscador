package uy.gub.bps.busqueda.aws.api.client;

import java.io.*;
import java.net.*;
import java.util.*;

import uy.gub.bps.busqueda.aws.api.config.ApiConfiguration;
import uy.gub.bps.busqueda.aws.api.exception.ApiException;
import uy.gub.bps.busqueda.aws.api.model.common.ApiResponse;

public class BaseRestClient {
	private final ApiConfiguration config;

	public BaseRestClient(ApiConfiguration c) {
		if (c == null)
			throw new IllegalArgumentException("config es obligatoria");
		config = c;
	}

	public ApiResponse get(String path) throws ApiException {
		return execute("GET", path, null);
	}

	public ApiResponse post(String path, String json) throws ApiException {
		return execute("POST", path, json);
	}

	public ApiResponse put(String path, String json) throws ApiException {
		return execute("PUT", path, json);
	}

	public ApiResponse delete(String path, String json) throws ApiException {
		return execute("DELETE", path, json);
	}

	//private
	public ApiResponse execute(String method, String path, String body)	throws ApiException {
		HttpURLConnection c = null;
		try {
			c = (HttpURLConnection) new URL(config.getBaseUrl() + path).openConnection();
			
			System.out.println("URL completa a enviar: " + config.getBaseUrl() + path);
			System.out.println("connection.getURL: "+ c.getURL());
			
			c.setRequestMethod(method);
			c.setConnectTimeout(config.getConnectTimeoutMillis());
			c.setReadTimeout(config.getReadTimeoutMillis());
			c.setUseCaches(false);
			c.setRequestProperty("Accept", "application/json");
			c.setRequestProperty("Accept-Language", "es-UY,es;q=0.9");
			c.setRequestProperty("User-Agent", "Application-innovaportal-cliente-busqueda-aws");
			
			for (Map.Entry<String, String> h : config.getDefaultHeaders().entrySet())
				c.setRequestProperty(h.getKey(), h.getValue());
			if (body != null) {
				c.setDoOutput(true);
				c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
				byte[] bytes = body.getBytes("UTF-8");
				c.setFixedLengthStreamingMode(bytes.length);
				OutputStream out = c.getOutputStream();
				try {
					out.write(bytes);
				} finally {
					out.close();
				}
			}
			
			// Headers enviados
//			Map<String, List<String>> headersReq = c.getRequestProperties();
//			for (Map.Entry<String, List<String>> h : headersReq.entrySet()) {
//			    System.out.println("[BaseRestClient]execute() request Header enviado: "+ h.getKey() + "=" + h.getValue());
//			}
			
			int status = c.getResponseCode();
			InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
			String response = read(in);
			ApiResponse r = new ApiResponse(status, response, c.getHeaderFields());

			//TODO: BORRAR 
			//solamente para logueo del user-agent hasta que se tome una definici�n
			if (r.isSuccessful()){
				System.out.println("[BaseRestClient]execute(), Response STATUS=" + status);
				Map<String,List<String>> headers = c.getHeaderFields();
				for (Map.Entry<String,List<String>> h : headers.entrySet()) {
				    //System.out.println("[BaseRestClient]execute() ApiResponse header: " + h.getKey() + " => " + h.getValue());
				}
				//System.out.println("[BaseRestClient]execute(), request Header User-Agent => " + c.getRequestProperty("User-Agent"));
			}			
			
			if (!r.isSuccessful()){
				System.out.println("[BaseRestClient]execute(), ApiResponse ERROR, STATUS=" + status);
				Map<String,List<String>> headers = c.getHeaderFields();
				for (Map.Entry<String,List<String>> h : headers.entrySet()) {
				    System.out.println("[BaseRestClient]execute() ApiResponse ERROR | header: " + h.getKey() + " => " + h.getValue());
				}
				System.out.println("[BaseRestClient]execute(), ApiResponse ERROR | User-Agent => " + c.getRequestProperty("User-Agent"));
				throw new ApiException(status, response);
			}
			return r;
		} catch (ApiException e) {
			System.out.println("[BaseRestClient]execute() ApiException HTTP: " + e.getStatusCode());
			System.out.println("[BaseRestClient]execute() ApiException BODY: " + e.getResponseBody());
			throw e;
		} catch (Exception e) {
			throw new ApiException("[BaseRestClient]execute() Error en " + method + " " + config.getBaseUrl() + path, e);
		} finally {
			if (c != null)
				c.disconnect();
		}
	}

	private static String read(InputStream in) throws IOException {
		if (in == null)
			return "";
		BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		try {
			StringBuilder b = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null)
				b.append(line);
			return b.toString();
		} finally {
			r.close();
		}
	}
}
