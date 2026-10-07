package com.pulsos.medicina.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_adjuntos")
public class DocumentoAdjunto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreArchivoOriginal;
    private String nombreArchivoAlmacenado;
    private String tipoDocumento;
    private String descripcion;
    private String contentType;
    private long tamanio;
    private String subidoPor;
    private LocalDateTime fechaCarga = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    public DocumentoAdjunto() {}

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    public boolean isPdf() {
        return "application/pdf".equalsIgnoreCase(contentType);
    }

    public String getTamanioFormateado() {
        if (tamanio < 1024) return tamanio + " B";
        int exp = (int) (Math.log(tamanio) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", tamanio / Math.pow(1024, exp), pre);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreArchivoOriginal() { return nombreArchivoOriginal; }
    public void setNombreArchivoOriginal(String nombreArchivoOriginal) { this.nombreArchivoOriginal = nombreArchivoOriginal; }
    public String getNombreArchivoAlmacenado() { return nombreArchivoAlmacenado; }
    public void setNombreArchivoAlmacenado(String nombreArchivoAlmacenado) { this.nombreArchivoAlmacenado = nombreArchivoAlmacenado; }
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getTamanio() { return tamanio; }
    public void setTamanio(long tamanio) { this.tamanio = tamanio; }
    public String getSubidoPor() { return subidoPor; }
    public void setSubidoPor(String subidoPor) { this.subidoPor = subidoPor; }
    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
}
