package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public Usuario save(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public void deleteById(Long id) {
        usuarioRepository.deleteById(id);
    }

    public List<Usuario> buscar(String texto) {
        return usuarioRepository.findByNombreCompletoContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrEspecialidadContainingIgnoreCase(
                texto, texto, texto);
    }
}