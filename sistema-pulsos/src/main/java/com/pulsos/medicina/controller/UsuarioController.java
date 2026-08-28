package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Rol;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarTodos(String q) {
        if (q != null && !q.trim().isEmpty()) {
            return usuarioRepository.findAll().stream()
                    .filter(u -> (u.getNombreCompleto() != null && u.getNombreCompleto().toLowerCase().contains(q.toLowerCase()))
                            || (u.getUsername() != null && u.getUsername().toLowerCase().contains(q.toLowerCase()))
                            || (u.getEspecialidad() != null && u.getEspecialidad().toLowerCase().contains(q.toLowerCase())))
                    .toList();
        }
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarMedicos() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.isActivo() && u.getRoles() != null && u.getRoles().contains(Rol.MEDICO))
                .toList();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Transactional
    public void guardarOActualizar(Usuario usuario, String passwordPlana) {
        if (usuario.getId() != null) {
            Usuario usuarioExistente = usuarioRepository.findById(usuario.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            usuarioExistente.setNombreCompleto(usuario.getNombreCompleto());
            usuarioExistente.setUsername(usuario.getUsername());
            usuarioExistente.setEmail(usuario.getEmail());
            usuarioExistente.setEspecialidad(usuario.getEspecialidad());
            usuarioExistente.setMatricula(usuario.getMatricula());
            usuarioExistente.setRoles(usuario.getRoles());
            usuarioExistente.setActivo(usuario.isActivo());

            if (passwordPlana != null && !passwordPlana.trim().isEmpty()) {
                usuarioExistente.setPassword(passwordEncoder.encode(passwordPlana));
            }
            usuarioRepository.save(usuarioExistente);
        } else {
            if (passwordPlana != null && !passwordPlana.trim().isEmpty()) {
                usuario.setPassword(passwordEncoder.encode(passwordPlana));
            }
            usuarioRepository.save(usuario);
        }
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }
}
