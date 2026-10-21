package com.empresa.taskmanager.model;

public class Tecnico {
    private final Long id;
    private final String nombre;

    public Tecnico(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
