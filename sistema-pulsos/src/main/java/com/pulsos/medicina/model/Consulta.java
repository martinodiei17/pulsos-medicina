package com.pulsos.medicina.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultas")
public class Consulta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String diagnostico;

    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    private String medicoTratante;
    private String matriculaMedico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @Column(columnDefinition = "TEXT")
    private String enfermedadActual;
    @Column(columnDefinition = "TEXT")
    private String examenFisico;
    @Column(columnDefinition = "TEXT")
    private String diagnosticosSecundarios;
    @Column(columnDefinition = "TEXT")
    private String evolucion;
    @Column(columnDefinition = "TEXT")
    private String planSeguimiento;
    private Integer presionSistolica;
    private Integer presionDiastolica;
    private Integer frecuenciaCardiaca;
    private Integer frecuenciaRespiratoria;
    private Integer saturacionOxigeno;
    private java.math.BigDecimal pesoKg;
    private java.math.BigDecimal tallaCm;
    private java.math.BigDecimal temperaturaC;
    private java.time.LocalDate proximoControl;

    public Consulta() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
    public String getTratamiento() { return tratamiento; }
    public void setTratamiento(String tratamiento) { this.tratamiento = tratamiento; }
    public String getMedicoTratante() { return medicoTratante; }
    public void setMedicoTratante(String medicoTratante) { this.medicoTratante = medicoTratante; }
    public String getMatriculaMedico() { return matriculaMedico; }
    public void setMatriculaMedico(String matriculaMedico) { this.matriculaMedico = matriculaMedico; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public String getEnfermedadActual() { return enfermedadActual; }
    public void setEnfermedadActual(String value) { this.enfermedadActual = value; }
    public String getExamenFisico() { return examenFisico; }
    public void setExamenFisico(String value) { this.examenFisico = value; }
    public String getDiagnosticosSecundarios() { return diagnosticosSecundarios; }
    public void setDiagnosticosSecundarios(String value) { this.diagnosticosSecundarios = value; }
    public String getEvolucion() { return evolucion; }
    public void setEvolucion(String value) { this.evolucion = value; }
    public String getPlanSeguimiento() { return planSeguimiento; }
    public void setPlanSeguimiento(String value) { this.planSeguimiento = value; }
    public Integer getPresionSistolica() { return presionSistolica; }
    public void setPresionSistolica(Integer value) { this.presionSistolica = value; }
    public Integer getPresionDiastolica() { return presionDiastolica; }
    public void setPresionDiastolica(Integer value) { this.presionDiastolica = value; }
    public Integer getFrecuenciaCardiaca() { return frecuenciaCardiaca; }
    public void setFrecuenciaCardiaca(Integer value) { this.frecuenciaCardiaca = value; }
    public Integer getFrecuenciaRespiratoria() { return frecuenciaRespiratoria; }
    public void setFrecuenciaRespiratoria(Integer value) { this.frecuenciaRespiratoria = value; }
    public Integer getSaturacionOxigeno() { return saturacionOxigeno; }
    public void setSaturacionOxigeno(Integer value) { this.saturacionOxigeno = value; }
    public java.math.BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(java.math.BigDecimal value) { this.pesoKg = value; }
    public java.math.BigDecimal getTallaCm() { return tallaCm; }
    public void setTallaCm(java.math.BigDecimal value) { this.tallaCm = value; }
    public java.math.BigDecimal getTemperaturaC() { return temperaturaC; }
    public void setTemperaturaC(java.math.BigDecimal value) { this.temperaturaC = value; }
    public java.time.LocalDate getProximoControl() { return proximoControl; }
    public void setProximoControl(java.time.LocalDate value) { this.proximoControl = value; }
}
