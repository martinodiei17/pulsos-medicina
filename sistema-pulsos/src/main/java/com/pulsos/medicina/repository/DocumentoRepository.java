package com.pulsos.medicina.repository;

import com.pulsos.medicina.model.DocumentoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentoRepository extends JpaRepository<DocumentoAdjunto, Long> {
}
