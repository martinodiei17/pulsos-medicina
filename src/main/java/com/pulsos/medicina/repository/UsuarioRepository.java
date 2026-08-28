package com.pulsos.medicina.repository;

import com.pulsos.medicina.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByTokenCalendario(String tokenCalendario);
    Optional<Usuario> findByNombreCompleto(String nombreCompleto);
    List<Usuario> findByNombreCompletoContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrEspecialidadContainingIgnoreCase(String nombre, String username, String especialidad);
}
