package com.pulsos.medicina;

import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class PulsosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulsosApplication.class, args);
    }

    // Este bloque corrige y encripta automáticamente la contraseña del admin al iniciar
    @Bean
    CommandLineRunner initAdminPassword(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            usuarioRepository.findByUsername("admin").ifPresent(admin -> {
                admin.setPassword(passwordEncoder.encode("admin123"));
                usuarioRepository.save(admin);
                System.out.println("--> Contraseña de admin actualizada y encriptada correctamente.");
            });
        };
    }
}
