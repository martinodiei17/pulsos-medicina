package com.pulsos.medicina.repository;

import com.pulsos.medicina.model.DocumentoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<DocumentoAdjunto, Long> {
    List<DocumentoAdjunto> findByPacienteId(Long pacienteId);
}
