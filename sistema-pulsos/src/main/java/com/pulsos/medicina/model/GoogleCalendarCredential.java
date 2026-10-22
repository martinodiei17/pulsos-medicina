package com.pulsos.medicina.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="google_calendar_credentials")
public class GoogleCalendarCredential {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="usuario_id",unique=true,nullable=false) private Usuario usuario;
 @Column(length=2048) private String refreshTokenCifrado;
 private String googleEmail;
 private String calendarId;
 private LocalDateTime ultimaSync;
 @Column(length=1000) private String ultimoError;
 private LocalDateTime ultimoErrorFecha;
 public Long getId(){return id;}
 public Usuario getUsuario(){return usuario;} public void setUsuario(Usuario v){usuario=v;}
 public String getRefreshTokenCifrado(){return refreshTokenCifrado;} public void setRefreshTokenCifrado(String v){refreshTokenCifrado=v;}
 public String getGoogleEmail(){return googleEmail;} public void setGoogleEmail(String v){googleEmail=v;}
 public String getCalendarId(){return calendarId;} public void setCalendarId(String v){calendarId=v;}
 public LocalDateTime getUltimaSync(){return ultimaSync;} public void setUltimaSync(LocalDateTime v){ultimaSync=v;}
 public String getUltimoError(){return ultimoError;} public void setUltimoError(String v){ultimoError=v;}
 public LocalDateTime getUltimoErrorFecha(){return ultimoErrorFecha;} public void setUltimoErrorFecha(LocalDateTime v){ultimoErrorFecha=v;}
}
