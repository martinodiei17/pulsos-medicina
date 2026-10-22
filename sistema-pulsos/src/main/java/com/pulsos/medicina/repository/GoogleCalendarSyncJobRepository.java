package com.pulsos.medicina.repository;
import com.pulsos.medicina.model.GoogleCalendarSyncJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;
public interface GoogleCalendarSyncJobRepository extends JpaRepository<GoogleCalendarSyncJob,Long> {
 List<GoogleCalendarSyncJob> findByTurnoIdOrderByFechaCreacionDesc(Long id);
 List<GoogleCalendarSyncJob> findByUsuarioIdOrderByFechaCreacionDesc(Long id);
 List<GoogleCalendarSyncJob> findByEstadoOrderByFechaCreacionAsc(String estado);
 @Query("""
 SELECT j FROM GoogleCalendarSyncJob j WHERE j.estado IN ('PENDIENTE','ERROR')
 AND j.proximoIntento <= :ahora
 AND NOT EXISTS (SELECT a.id FROM GoogleCalendarSyncJob a WHERE a.turnoId=j.turnoId
 AND a.id < j.id AND a.estado IN ('PENDIENTE','ERROR','PROCESANDO')) ORDER BY j.id ASC
 """)
 List<GoogleCalendarSyncJob> buscarTrabajosPendientes(@Param("ahora") LocalDateTime ahora);
 @Query("""
 SELECT j FROM GoogleCalendarSyncJob j WHERE j.estado IN ('PENDIENTE','ERROR')
 AND j.proximoIntento <= :ahora
 AND NOT EXISTS (SELECT a.id FROM GoogleCalendarSyncJob a WHERE a.turnoId=j.turnoId
 AND a.id < j.id AND a.estado IN ('PENDIENTE','ERROR','PROCESANDO')) ORDER BY j.id ASC
 """)
 List<GoogleCalendarSyncJob> buscarTrabajosPendientesLimitados(@Param("ahora") LocalDateTime ahora, Pageable page);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("SELECT j FROM GoogleCalendarSyncJob j WHERE j.id=:id")
 Optional<GoogleCalendarSyncJob> buscarPorIdConBloqueo(@Param("id") Long id);
 @Modifying(flushAutomatically=true,clearAutomatically=true)
 @Query("UPDATE GoogleCalendarSyncJob j SET j.estado='PROCESANDO',j.fechaActualizacion=:ahora,j.proximoIntento=null WHERE j.id=:id AND j.estado IN ('PENDIENTE','ERROR') AND j.proximoIntento<=:ahora")
 int reclamarTrabajo(@Param("id") Long id,@Param("ahora") LocalDateTime ahora);
 @Modifying(flushAutomatically=true,clearAutomatically=true)
 @Query("UPDATE GoogleCalendarSyncJob j SET j.estado='PENDIENTE',j.proximoIntento=:ahora,j.fechaActualizacion=:ahora WHERE j.estado='PROCESANDO' AND j.fechaActualizacion<:limite")
 int recuperarTrabajosInterrumpidos(@Param("limite") LocalDateTime limite,@Param("ahora") LocalDateTime ahora);
 Optional<GoogleCalendarSyncJob> findByIdempotencyKey(String key);
 boolean existsByTurnoIdAndEstadoIn(Long turnoId,Collection<String> estados);
 @Query("SELECT j FROM GoogleCalendarSyncJob j WHERE j.usuarioId=:id AND j.estado IN ('PENDIENTE','ERROR') ORDER BY j.id ASC")
 List<GoogleCalendarSyncJob> buscarPendientesPorUsuario(@Param("id") Long id);
 long countByEstado(String estado);
}
