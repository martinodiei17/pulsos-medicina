package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Rol;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        List<SimpleGrantedAuthority> authorities = usuario.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.name()))
                .collect(Collectors.toList());

        return new User(usuario.getUsername(), usuario.getPassword(), usuario.isActivo(), true, true, true, authorities);
    }

    public List<Usuario> listarTodos(String busqueda) {
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            return usuarioRepo.findByNombreCompletoContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrEspecialidadContainingIgnoreCase(
                    busqueda.trim(), busqueda.trim(), busqueda.trim());
        }
        return usuarioRepo.findAll();
    }

    public List<Usuario> listarMedicos() {
        return usuarioRepo.findAll().stream()
                .filter(u -> u.isActivo() && u.getRoles().contains(Rol.ROLE_MEDICO))
                .collect(Collectors.toList());
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepo.findById(id);
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepo.findByUsername(username);
    }

    public Usuario guardarOActualizar(Usuario formUsuario, String passwordPlana) {
        Usuario usuario;
        if (formUsuario.getId() != null) {
            usuario = usuarioRepo.findById(formUsuario.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            usuario.setNombreCompleto(formUsuario.getNombreCompleto());
            usuario.setUsername(formUsuario.getUsername());
            usuario.setEmail(formUsuario.getEmail());
            usuario.setTelefono(formUsuario.getTelefono());
            usuario.setEspecialidad(formUsuario.getEspecialidad());
            usuario.setMatricula(formUsuario.getMatricula());
            usuario.setActivo(formUsuario.isActivo());
            usuario.setRoles(formUsuario.getRoles());

            if (passwordPlana != null && !passwordPlana.trim().isEmpty()) {
                usuario.setPassword(passwordEncoder.encode(passwordPlana.trim()));
            }
        } else {
            usuario = formUsuario;
            if (passwordPlana != null && !passwordPlana.trim().isEmpty()) {
                usuario.setPassword(passwordEncoder.encode(passwordPlana.trim()));
            } else {
                usuario.setPassword(passwordEncoder.encode("pulsos123"));
            }
            if (usuario.getTokenCalendario() == null || usuario.getTokenCalendario().isEmpty()) {
                usuario.setTokenCalendario(UUID.randomUUID().toString());
            }
        }

        return usuarioRepo.save(usuario);
    }

    public void cambiarEstado(Long id, boolean activo) {
        Usuario usuario = usuarioRepo.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setActivo(activo);
        usuarioRepo.save(usuario);
    }
}
