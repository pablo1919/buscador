package uy.gub.bps.busqueda.aws.api.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public final class UrlUtils {
	
	private UrlUtils() {
	}

	public static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String query(Map<String, ?> p) {
		StringBuilder b = new StringBuilder();
		for (Map.Entry<String, ?> e : p.entrySet()) {
			if (e.getValue() == null)
				continue;
			b.append(b.length() == 0 ? '?' : '&')
					.append(encode(e.getKey()))
					.append('=')
					.append(encode(String.valueOf(e.getValue())));
		}
		return b.toString();
	}
}
