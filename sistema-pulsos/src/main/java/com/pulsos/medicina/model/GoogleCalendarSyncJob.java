package com.pulsos.medicina.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="google_calendar_sync_jobs")
public class GoogleCalendarSyncJob {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long turnoId; private Long usuarioId; private String operacion; private String estado="PENDIENTE";
 private String googleEventId; private String googleCalendarId;
 @Column(unique=true) private String idempotencyKey;
 private int intentos=0; private LocalDateTime proximoIntento;
 private LocalDateTime fechaCreacion; private LocalDateTime fechaActualizacion;
 @Column(length=1000) private String ultimoError;
 @PrePersist void crear(){fechaCreacion=LocalDateTime.now();fechaActualizacion=fechaCreacion;}
 @PreUpdate void actualizar(){fechaActualizacion=LocalDateTime.now();}
 public Long getId(){return id;}
 public Long getTurnoId(){return turnoId;} public void setTurnoId(Long v){turnoId=v;}
 public Long getUsuarioId(){return usuarioId;} public void setUsuarioId(Long v){usuarioId=v;}
 public String getOperacion(){return operacion;} public void setOperacion(String v){operacion=v;}
 public String getEstado(){return estado;} public void setEstado(String v){estado=v;}
 public String getGoogleEventId(){return googleEventId;} public void setGoogleEventId(String v){googleEventId=v;}
 public String getGoogleCalendarId(){return googleCalendarId;} public void setGoogleCalendarId(String v){googleCalendarId=v;}
 public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
 public int getIntentos(){return intentos;} public void setIntentos(int v){intentos=v;}
 public LocalDateTime getProximoIntento(){return proximoIntento;} public void setProximoIntento(LocalDateTime v){proximoIntento=v;}
 public LocalDateTime getFechaCreacion(){return fechaCreacion;} public void setFechaCreacion(LocalDateTime v){fechaCreacion=v;}
 public LocalDateTime getFechaActualizacion(){return fechaActualizacion;} public void setFechaActualizacion(LocalDateTime v){fechaActualizacion=v;}
 public String getUltimoError(){return ultimoError;} public void setUltimoError(String v){ultimoError=v;}
}
