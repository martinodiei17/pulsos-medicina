package com.pulsos.medicina.repository;

import com.pulsos.medicina.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    List<Paciente> findByNombreCompletoContainingIgnoreCaseOrDniContaining(String nombre, String dni);
}
