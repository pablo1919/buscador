package uy.gub.bps.busqueda.aws.api.model.busqueda;

import java.util.LinkedHashMap;
import java.util.Map;

import uy.gub.bps.busqueda.aws.api.util.UrlUtils;

public final class PredictivaQuery {
	private final String q;
	private String ambito;

	public PredictivaQuery(String q) {
		if (q == null || q.trim().isEmpty())
			throw new IllegalArgumentException("q es obligatorio");
		this.q = q;
	}

	public PredictivaQuery ambito(String v) {
		ambito = v;
		return this;
	}

	public String toQueryString() {
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put("q", q);
		p.put("ambito", ambito);
		return UrlUtils.query(p);
	}
}
