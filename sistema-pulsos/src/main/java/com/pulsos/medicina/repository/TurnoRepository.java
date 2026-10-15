
package com.pulsos.medicina.repository;

import com.pulsos.medicina.model.Turno;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    @EntityGraph(attributePaths = {"paciente"})
    List<Turno> findByFechaHoraBetweenOrderByFechaHoraAsc(
            LocalDateTime start,
            LocalDateTime end
    );

    @EntityGraph(attributePaths = {"paciente"})
    List<Turno> findAllByOrderByFechaHoraDesc();

    @EntityGraph(attributePaths = {"paciente"})
    List<Turno> findByMedicoAsignadoOrderByFechaHoraAsc(
            String medicoAsignado
    );
}

