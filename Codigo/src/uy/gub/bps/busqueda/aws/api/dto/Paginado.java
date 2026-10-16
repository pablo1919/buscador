package uy.gub.bps.busqueda.aws.api.dto;

import java.util.List;
import java.util.Map;

public class Paginado {

    private Integer pagina;
    private Integer porPagina;
    private Integer total;
    private Integer totalPaginas;
    private Integer maxResultados;
    private String cursor;
    private Map<String, Integer> porTipo;
    private List<Filtro> filtros;

    public Integer getPagina() {
        return pagina;
    }

    public void setPagina(Integer pagina) {
        this.pagina = pagina;
    }

    public Integer getPorPagina() {
        return porPagina;
    }

    public void setPorPagina(Integer porPagina) {
        this.porPagina = porPagina;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getTotalPaginas() {
        return totalPaginas;
    }

    public void setTotalPaginas(Integer totalPaginas) {
        this.totalPaginas = totalPaginas;
    }

    public Integer getMaxResultados() {
        return maxResultados;
    }

    public void setMaxResultados(Integer maxResultados) {
        this.maxResultados = maxResultados;
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public Map<String, Integer> getPorTipo() {
        return porTipo;
    }

    public void setPorTipo(Map<String, Integer> porTipo) {
        this.porTipo = porTipo;
    }

	public List<Filtro> getFiltros() {
		return filtros;
	}

	public void setFiltros(List<Filtro> filtros) {
		this.filtros = filtros;
	}
}
