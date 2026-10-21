package com.empresa.taskmanager.model;

public class Administrador {
    private final Long id;
    private final String nombre;

    public Administrador(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
