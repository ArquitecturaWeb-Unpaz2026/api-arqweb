package com.empresa.memoria.model;

import java.util.List;

public class GrupoTecnicos {
    private final Long id;
    private final String nombre;
    private final List<Long> tecnicosIds;

    public GrupoTecnicos(Long id, String nombre, List<Long> tecnicosIds) {
        this.id = id;
        this.nombre = nombre;
        this.tecnicosIds = List.copyOf(tecnicosIds);
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public List<Long> getTecnicosIds() { return tecnicosIds; }
    public boolean contieneTecnico(Long tecnicoId) { return tecnicosIds.contains(tecnicoId); }
}
