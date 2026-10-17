package uy.gub.bps.busqueda.web.salud;

/**
 * Caché en memoria del último estado conocido del endpoint /salud.
 *
 * <p>La instancia es única por JVM (vive como campo del servlet). Se consulta
 * únicamente en el camino de error de la búsqueda, con el objetivo de evitar
 * multiplicar las llamadas a /salud cuando el backend está degradado.</p>
 *
 * <p>Comportamiento ante concurrencia: se usa doble verificación con un
 * único sondeo efectivo por ventana de TTL. Mientras un hilo sondea, los
 * demás no esperan: usan el último estado conocido (stale) y, si la caché
 * aún está fría, obtienen DESCONOCIDO. Esto evita encolar peticiones de
 * usuario detrás de un /salud lento y evita el "thundering herd".</p>
 */
public final class CacheEstadoSalud {

    /** TTL por defecto de la caché, en milisegundos. */
    public static final long TTL_MS_POR_DEFECTO = 60000L;

    private final SondeoSalud sondeo;
    /** Ventana de validez del estado cacheado, en milisegundos. */
    private final long ttlMs;
    private final Object lock = new Object();

    private volatile EstadoSalud estado = EstadoSalud.DESCONOCIDO;
    private volatile long timestampNs = 0L;
    private volatile boolean sondeoEnCurso = false;

    public CacheEstadoSalud(SondeoSalud sondeo) {
        this(sondeo, TTL_MS_POR_DEFECTO);
    }

    /**
     * @param sondeo sonda de /salud; obligatoria.
     * @param ttlMs  ventana de validez del estado cacheado, en milisegundos.
     *               Si es menor o igual a cero se usa {@link #TTL_MS_POR_DEFECTO}.
     */
    public CacheEstadoSalud(SondeoSalud sondeo, long ttlMs) {
        if (sondeo == null) {
            throw new IllegalArgumentException("sondeo es obligatorio");
        }
        this.sondeo = sondeo;
        this.ttlMs = ttlMs > 0 ? ttlMs : TTL_MS_POR_DEFECTO;
    }

    /**
     * Devuelve el estado del servicio. Puede disparar un sondeo a /salud si
     * la caché está fría o vencida; en caso contrario reutiliza lo cacheado.
     * Nunca bloquea la espera de otro sondeo en curso: en ese caso devuelve
     * el último estado conocido (stale).
     *
     * @return estado clasificado; nunca null.
     */
    public EstadoSalud obtener() {
        long ahoraNs = System.nanoTime();

        if (esVigente(ahoraNs)) {
            return estado;
        }

        // Alguien ya está sondeando: no esperar, usar lo último conocido.
        if (sondeoEnCurso) {
            return estado;
        }

        synchronized (lock) {
            // Doble verificación: otro hilo pudo refrescar la caché mientras esperábamos.
            ahoraNs = System.nanoTime();
            if (esVigente(ahoraNs)) {
                return estado;
            }
            if (sondeoEnCurso) {
                return estado;
            }

            sondeoEnCurso = true;
            try {
                EstadoSalud nuevo = sondeo.sondear();
                if (nuevo == null) {
                    nuevo = EstadoSalud.DESCONOCIDO;
                }
                estado = nuevo;
                timestampNs = System.nanoTime();
                return estado;
            } finally {
                sondeoEnCurso = false;
            }
        }
    }

    private boolean esVigente(long ahoraNs) {
        return timestampNs != 0L
                && (ahoraNs - timestampNs) / 1000000L < ttlMs;
    }

    /**
     * Sonda de /salud. Devuelve el estado según el código HTTP documentado
     * (200 operativo, 503 degradado). Otros códigos o fallos => DESCONOCIDO.
     * No debe lanzar: cualquier error se traduce a DESCONOCIDO.
     */
    public interface SondeoSalud {
        EstadoSalud sondear();
    }
}
