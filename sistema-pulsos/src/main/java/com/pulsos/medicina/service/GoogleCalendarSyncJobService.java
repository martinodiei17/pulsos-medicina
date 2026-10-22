package com.pulsos.medicina.service;

import com.pulsos.medicina.model.GoogleCalendarSyncJob;
import com.pulsos.medicina.repository.GoogleCalendarSyncJobRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class GoogleCalendarSyncJobService {

    private static final int MAX_TRABAJOS_POR_CICLO = 25;
    private static final int MINUTOS_RECUPERACION = 15;

    private final GoogleCalendarSyncJobRepository jobRepository;
    private final GoogleCalendarSyncService syncService;
    private final TransactionTemplate transactionTemplate;

    public GoogleCalendarSyncJobService(
            GoogleCalendarSyncJobRepository jobRepository,
            GoogleCalendarSyncService syncService,
            PlatformTransactionManager transactionManager) {

        this.jobRepository = jobRepository;
        this.syncService = syncService;

        this.transactionTemplate =
                new TransactionTemplate(transactionManager);

        this.transactionTemplate.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition
                        .PROPAGATION_REQUIRES_NEW
        );
    }

    /**
     * Registra una operación en PostgreSQL.
     *
     * Participa de la transacción del turno.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public GoogleCalendarSyncJob registrarTrabajo(
            Long turnoId,
            Long usuarioId,
            String operacion,
            String googleEventId,
            String googleCalendarId) {

        if (turnoId == null) {
            throw new IllegalArgumentException(
                    "El turno es obligatorio"
            );
        }

        if (usuarioId == null) {
            throw new IllegalArgumentException(
                    "El profesional es obligatorio"
            );
        }

        if (!"CREAR".equals(operacion)
                && !"ACTUALIZAR".equals(operacion)
                && !"ELIMINAR".equals(operacion)) {

            throw new IllegalArgumentException(
                    "Operación de sincronización no válida"
            );
        }

        GoogleCalendarSyncJob job =
                new GoogleCalendarSyncJob();

        job.setTurnoId(turnoId);
        job.setUsuarioId(usuarioId);
        job.setOperacion(operacion);
        job.setEstado("PENDIENTE");

        job.setGoogleEventId(googleEventId);
        job.setGoogleCalendarId(googleCalendarId);

        job.setIdempotencyKey(
                UUID.randomUUID().toString()
        );

        job.setIntentos(0);
        job.setProximoIntento(LocalDateTime.now());

        return jobRepository.save(job);
    }

    /**
     * Procesa periódicamente la cola.
     *
     * Requiere @EnableScheduling.
     */
    @Scheduled(fixedDelay = 60000)
    public void procesarPendientes() {

        try {
            recuperarTrabajosInterrumpidos();
        } catch (Exception e) {
            System.err.println(
                    "Google Calendar: error recuperando trabajos: "
                    + e.getMessage()
            );
        }

        List<GoogleCalendarSyncJob> pendientes;

        try {

            pendientes =
                    jobRepository.buscarTrabajosPendientesLimitados(
                            LocalDateTime.now(),
                            PageRequest.of(
                                    0,
                                    MAX_TRABAJOS_POR_CICLO
                            )
                    );

        } catch (Exception e) {

            System.err.println(
                    "Google Calendar: error consultando pendientes: "
                    + e.getMessage()
            );

            return;
        }

        for (GoogleCalendarSyncJob pendiente : pendientes) {

            try {
                procesarTrabajo(pendiente.getId());

            } catch (Exception e) {

                System.err.println(
                        "Google Calendar: error procesando trabajo "
                        + pendiente.getId()
                        + ": "
                        + e.getMessage()
                );
            }
        }
    }

    /**
     * Reclama el trabajo, realiza la sincronización
     * y registra su resultado.
     */
    public void procesarTrabajo(Long jobId) {

        if (jobId == null) {
            return;
        }

        boolean reclamado = reclamarTrabajo(jobId);

        if (!reclamado) {
            return;
        }

        try {

            GoogleCalendarSyncJob job =
                    jobRepository.findById(jobId)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Trabajo no encontrado: "
                                            + jobId
                                    )
                            );

            syncService.sincronizarTrabajo(job);

            completarTrabajo(jobId);

        } catch (Exception e) {

            registrarError(jobId, e);
        }
    }

    /**
     * Reclama una operación mediante una transacción
     * independiente.
     */
    private boolean reclamarTrabajo(Long jobId) {

        Boolean resultado = transactionTemplate.execute(
                status -> {

                    int actualizados =
                            jobRepository.reclamarTrabajo(
                                    jobId,
                                    LocalDateTime.now()
                            );

                    return actualizados == 1;
                }
        );

        return Boolean.TRUE.equals(resultado);
    }

    /**
     * Marca como completada la operación.
     */
    private void completarTrabajo(Long jobId) {

        transactionTemplate.executeWithoutResult(status -> {

            GoogleCalendarSyncJob job =
                    jobRepository.buscarPorIdConBloqueo(jobId)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Trabajo no encontrado: "
                                            + jobId
                                    )
                            );

            if (!"PROCESANDO".equals(job.getEstado())) {
                return;
            }

            job.setEstado("COMPLETADO");
            job.setUltimoError(null);
            job.setProximoIntento(null);

            jobRepository.save(job);
        });
    }

    /**
     * Registra el error y calcula el próximo intento.
     */
    private void registrarError(
            Long jobId,
            Exception error) {

        transactionTemplate.executeWithoutResult(status -> {

            GoogleCalendarSyncJob job =
                    jobRepository.buscarPorIdConBloqueo(jobId)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Trabajo no encontrado: "
                                            + jobId
                                    )
                            );

            if (!"PROCESANDO".equals(job.getEstado())) {
                return;
            }

            int intentos = job.getIntentos() + 1;

            job.setIntentos(intentos);
            job.setEstado("ERROR");

            String mensaje = error.getMessage();

            if (mensaje == null || mensaje.isBlank()) {
                mensaje = error.getClass().getSimpleName();
            }

            if (mensaje.length() > 1000) {
                mensaje = mensaje.substring(0, 1000);
            }

            job.setUltimoError(mensaje);

            long minutosEspera = Math.min(
                    60,
                    1L << Math.min(intentos - 1, 6)
            );

            job.setProximoIntento(
                    LocalDateTime.now()
                            .plusMinutes(minutosEspera)
            );

            jobRepository.save(job);
        });
    }

    /**
     * Recupera trabajos que quedaron interrumpidos
     * por un reinicio o una caída del servidor.
     */
    public void recuperarTrabajosInterrumpidos() {

        transactionTemplate.executeWithoutResult(status -> {

            LocalDateTime ahora = LocalDateTime.now();

            LocalDateTime limite =
                    ahora.minusMinutes(
                            MINUTOS_RECUPERACION
                    );

            int recuperados =
                    jobRepository.recuperarTrabajosInterrumpidos(
                            limite,
                            ahora
                    );

            if (recuperados > 0) {

                System.out.println(
                        "Google Calendar: trabajos recuperados: "
                        + recuperados
                );
            }
        });
    }

    /**
     * Lista operaciones pendientes del profesional.
     */
    public List<GoogleCalendarSyncJob> listarPendientes(
            Long usuarioId) {

        return jobRepository.buscarPendientesPorUsuario(
                usuarioId
        );
    }

    /**
     * Cuenta trabajos pendientes o con error.
     */
    public long contarPendientes() {

        return jobRepository.countByEstado("PENDIENTE")
                + jobRepository.countByEstado("ERROR");
    }
}
