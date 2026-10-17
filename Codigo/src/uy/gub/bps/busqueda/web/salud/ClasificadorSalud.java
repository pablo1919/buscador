package uy.gub.bps.busqueda.web.salud;

import uy.gub.bps.busqueda.aws.api.ClienteApiAws;
import uy.gub.bps.busqueda.aws.api.model.common.ApiResponse;
import uy.gub.bps.busqueda.web.salud.CacheEstadoSalud.SondeoSalud;

/**
 * Traduce el resultado de /salud al {@link EstadoSalud} usado por la interfaz.
 * Sólo se apoya en el código HTTP documentado en el contrato (200 operativo,
 * 503 degradado) para no depender de campos del cuerpo no confirmados.
 */
public final class ClasificadorSalud implements SondeoSalud {

    private final ClienteApiAws client;

    public ClasificadorSalud(ClienteApiAws client) {
        this.client = client;
    }

    @Override
    public EstadoSalud sondear() {
        try {
            ApiResponse respuesta = client.sistema().salud();
            int status = respuesta.getStatusCode();
            if (status == 200) {
                return EstadoSalud.OPERATIVO;
            }
            if (status == 503) {
                return EstadoSalud.DEGRADADO;
            }
            return EstadoSalud.DESCONOCIDO;
        } catch (Exception e) {
            // Un fallo al sondear no debe propagarse: estado no concluyente.
            return EstadoSalud.DESCONOCIDO;
        }
    }
}
