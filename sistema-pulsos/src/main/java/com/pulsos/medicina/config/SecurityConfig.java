package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Método para guardar un usuario nuevo
    public Usuario guardarUsuario(Usuario usuario) {
        // Encriptar la contraseña obligatoriamente para registros nuevos
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    // Método seguro para actualizar un usuario existente sin romper la contraseña
    public Usuario actualizarUsuario(Long id, Usuario usuarioActualizado, String nuevaPassword) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuarioExistente = usuarioOpt.get();
            
            // Actualizar campos básicos
            usuarioExistente.setEmail(usuarioActualizado.getEmail());
            // Actualiza aquí otros campos que tenga tu entidad Usuario (ej. nombre, apellido, etc.)

            // VALIDACIÓN CLAVE: Si la nueva contraseña no está vacía, se encripta y se actualiza.
            // Si viene vacía, se MANTIENE la contraseña anterior para que no se rompa el acceso.
            if (nuevaPassword != null && !nuevaPassword.trim().isEmpty()) {
                usuarioExistente.setPassword(passwordEncoder.encode(nuevaPassword));
            }

            return usuarioRepository.save(usuarioExistente);
        } else {
            throw new RuntimeException("Usuario no encontrado con ID: " + id);
        }
    }
}
