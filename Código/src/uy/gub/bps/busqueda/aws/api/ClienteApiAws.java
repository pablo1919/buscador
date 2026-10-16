package uy.gub.bps.busqueda.aws.api;

import uy.gub.bps.busqueda.aws.api.client.BaseRestClient;
import uy.gub.bps.busqueda.aws.api.config.ApiConfiguration;
import uy.gub.bps.busqueda.aws.api.service.AdministracionService;
import uy.gub.bps.busqueda.aws.api.service.BusquedaService;
import uy.gub.bps.busqueda.aws.api.service.HistorialService;
import uy.gub.bps.busqueda.aws.api.service.IndexacionService;
import uy.gub.bps.busqueda.aws.api.service.SistemaService;

public final class ClienteApiAws {
	private final SistemaService sistema;
	private final BusquedaService busqueda;
	private final IndexacionService indexacion;
	private final HistorialService historial;
	private final AdministracionService administracion;

	public ClienteApiAws(ApiConfiguration c) {
		BaseRestClient r = new BaseRestClient(c);
		sistema = new SistemaService(r);
		busqueda = new BusquedaService(r);
		indexacion = new IndexacionService(r);
		historial = new HistorialService(r);
		administracion = new AdministracionService(r);
	}

	public SistemaService sistema() {
		return sistema;
	}

	public BusquedaService busqueda() {
		return busqueda;
	}

	public IndexacionService indexacion() {
		return indexacion;
	}

	public HistorialService historial() {
		return historial;
	}

	public AdministracionService administracion() {
		return administracion;
	}
}
