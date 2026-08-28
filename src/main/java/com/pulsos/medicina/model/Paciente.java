package com.pulsos.medicina.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pacientes")
public class Paciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreCompleto;

    @Column(unique = true, nullable = false)
    private String dni;

    private LocalDate fechaNacimiento;
    private String telefono;
    private String email;
    private String obraSocial;
    private String numeroAfiliado;
    private String grupoSanguineo;

    @Column(columnDefinition = "TEXT")
    private String antecedentesMedicos;

    @Column(columnDefinition = "TEXT")
    private String alergias;

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha DESC")
    private List<Consulta> consultas = new ArrayList<>();

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaCarga DESC")
    private List<DocumentoAdjunto> adjuntos = new ArrayList<>();

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaHora ASC")
    private List<Turno> turnos = new ArrayList<>();

    public Paciente() {}

    public Integer getEdad() {
        if (fechaNacimiento == null) return null;
        try {
            return Period.between(fechaNacimiento, LocalDate.now()).getYears();
        } catch (Exception e) {
            return null;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getObraSocial() { return obraSocial; }
    public void setObraSocial(String obraSocial) { this.obraSocial = obraSocial; }
    public String getNumeroAfiliado() { return numeroAfiliado; }
    public void setNumeroAfiliado(String numeroAfiliado) { this.numeroAfiliado = numeroAfiliado; }
    public String getGrupoSanguineo() { return grupoSanguineo; }
    public void setGrupoSanguineo(String grupoSanguineo) { this.grupoSanguineo = grupoSanguineo; }
    public String getAntecedentesMedicos() { return antecedentesMedicos; }
    public void setAntecedentesMedicos(String antecedentesMedicos) { this.antecedentesMedicos = antecedentesMedicos; }
    public String getAlergias() { return alergias; }
    public void setAlergias(String alergias) { this.alergias = alergias; }
    public List<Consulta> getConsultas() { return consultas; }
    public void setConsultas(List<Consulta> consultas) { this.consultas = consultas; }
    public List<DocumentoAdjunto> getAdjuntos() { return adjuntos; }
    public void setAdjuntos(List<DocumentoAdjunto> adjuntos) { this.adjuntos = adjuntos; }
    public List<Turno> getTurnos() { return turnos; }
    public void setTurnos(List<Turno> turnos) { this.turnos = turnos; }
}
