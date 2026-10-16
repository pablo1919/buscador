package uy.gub.bps.busqueda.aws.rest;

import java.util.Date;

/**
 * API REST para consumo de servicios de AWS
 *
 */
public interface ApiRest {
	/**
	 * Cliente: script de réplica
	 * endpoint: Datapower
	 * obtiene los contenidos para actualizar el índice en AWS 
	 * @return resultado de la operación en AWS: OK/ERROR
	 * 
	 */
	String buscar(String query, String searchscope);
	String indexar(Date fechaDesde);
	void glosario();
	String destacados();
	String pesos();
	
}
