package com.pulsos.medicina.repository;

import com.pulsos.medicina.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByUsername(String username);

    List<Usuario> findByNombreCompletoContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrEspecialidadContainingIgnoreCase(
            String nombreCompleto, String username, String especialidad);
}
