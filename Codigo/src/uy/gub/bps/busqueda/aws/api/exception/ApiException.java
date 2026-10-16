package uy.gub.bps.busqueda.aws.api.exception;

public class ApiException extends Exception {
	private static final long serialVersionUID = 7307286234087231689L;
	private final int statusCode;
	private final String responseBody;

	public ApiException(String m, Throwable c) {
		super(m, c);
		statusCode = -1;
		responseBody = null;
	}

	public ApiException(int s, String b) {
		super("ApiException HTTP " + s + (b == null ? "" : ": " + b));
		statusCode = s;
		responseBody = b;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getResponseBody() {
		return responseBody;
	}
}
