package uy.gub.bps.busqueda.web.salud;

/**
 * Clasificación del estado del servicio de búsqueda obtenida a partir del
 * endpoint /salud, usada para enriquecer el mensaje de error mostrado al
 * usuario sin exponer información técnica (RF1.4).
 */
public enum EstadoSalud {

    /** El servicio respondió operativo (HTTP 200). */
    OPERATIVO,

    /** El servicio reportó una dependencia caída (HTTP 503). */
    DEGRADADO,

    /**
     * No se pudo determinar el estado (sondeo fallido, timeout o caché fría
     * durante otro sondeo en curso). Se trata como no concluyente.
     */
    DESCONOCIDO
}
