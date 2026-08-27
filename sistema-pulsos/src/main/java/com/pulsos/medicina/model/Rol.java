package com.pulsos.medicina.model;

public enum Rol {
    ROLE_MEDICO("Médico / Profesional"),
    ROLE_RECEPCION("Recepción / Secretaría"),
    ROLE_ADMIN("Administrador");

    private final String descripcion;

    Rol(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
