package uy.gub.bps.busqueda.aws.api.dto;

import java.util.ArrayList;
import java.util.List;

public class ResultadoBusquedaResponse {

    private List<ResultadoBusquedaItem> resultados = new ArrayList<ResultadoBusquedaItem>();
    private Paginado paginado;

    public List<ResultadoBusquedaItem> getResultados() {
        return resultados;
    }

    public void setResultados(
            List<ResultadoBusquedaItem> resultados) {
        this.resultados = resultados;
    }

    public Paginado getPaginado() {
        return paginado;
    }

    public void setPaginado(Paginado paginado) {
        this.paginado = paginado;
    }
}
