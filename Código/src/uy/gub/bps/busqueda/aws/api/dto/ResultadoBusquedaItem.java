package uy.gub.bps.busqueda.aws.api.dto;

import java.util.regex.Pattern;

public class ResultadoBusquedaItem {

    private String id;
    private String tipo;
    private String titulo;
    private String url;
    private String urlCanonica;
    private String resumen;
    private String fechaActualizacion;
    private String fragmento;
    private String etiqueta; //para mostrar en UI

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public String getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(String fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getFragmento() {
        return fragmento;
    }

    public void setFragmento(String fragmento) {
        this.fragmento = fragmento;
    }

	public String getUrlCanonica() {
		return urlCanonica;
	}

	public void setUrlCanonica(String urlCanonica) {
		this.urlCanonica = urlCanonica;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	public void setEtiqueta(String etiqueta) {
		this.etiqueta = etiqueta;
	}
	
	private static final Pattern TAG_EM = Pattern.compile("</?em>"); // matchea <em> y </em>
	private static final Pattern ESPACIOS = Pattern.compile("\\s+"); // matchea 2+ espacios consecutivos

	/*
	 * Elimina tags HTML y espacios contiguos
	 */
	public String getFragmentoSinHtml() {
		if (fragmento == null) return null;
		return ESPACIOS.matcher(TAG_EM.matcher(fragmento).replaceAll(""))
				.replaceAll(" ").trim();
	}
	
	/*
	 * Determina si resumen == fragmento sin HTML
	 */
	public boolean isResumenDistintoFragmento() {
	    if (fragmento == null || resumen == null || resumen.isEmpty()) return false;	    
	    String fragmentoSinHtml = getFragmentoSinHtml();
	    String resumenSinEspaciosDobles = ESPACIOS.matcher(resumen).replaceAll(" ");
	    return fragmentoSinHtml != null 
	    		&& !fragmentoSinHtml.equalsIgnoreCase(resumenSinEspaciosDobles.trim());
	}

}