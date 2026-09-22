package com.pulsos.medicina.config;

import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Configurar o actualizar el usuario smeregone
            crearOActualizarUsuario(usuarioRepository, passwordEncoder, "smeregone", "123456", "Dra. Maria Sol Meregone");

            // Configurar o actualizar el usuario admin
            crearOActualizarUsuario(usuarioRepository, passwordEncoder, "admin", "123456", "Administrador Pulsos");
        };
    }

    private void crearOActualizarUsuario(UsuarioRepository repo, PasswordEncoder encoder, String username, String passwordPlana, String nombre) {
        Usuario usuario = repo.findByUsername(username).orElse(new Usuario());
        usuario.setUsername(username);
        usuario.setPassword(encoder.encode(passwordPlana));
        usuario.setNombreCompleto(nombre);
        usuario.setActivo(true);
        repo.save(usuario);
        System.out.println("Usuario " + username + " configurado exitosamente con contraseña: " + passwordPlana);
    }
}
