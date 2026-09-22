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
            // Reemplaza "smeregone" y la contraseña por la que utilices normalmente
            String username = "smeregone";
            
            if (usuarioRepository.findByUsername(username).isEmpty()) {
                Usuario admin = new Usuario();
                admin.setUsername(username);
                // Asegúrate de usar el encoder para que Spring Security reconozca el hash
                admin.setPassword(passwordEncoder.encode("tu_contraseña_segura")); 
                // Configura roles o nombre según las propiedades de tu entidad Usuario
                
                usuarioRepository.save(admin);
                System.out.println("Usuario inicial " + username + " creado exitosamente en PostgreSQL.");
            }
        };
    }
}
