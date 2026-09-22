package com.pulsos.medicina.config;

import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Este bean conecta Spring Security con tu base de datos mediante el repositorio de usuarios
    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return username -> usuarioRepository.findByUsername(username)
                .map(usuario -> org.springframework.security.core.userdetails.User.builder()
                        .username(usuario.getUsername())
                        .password(usuario.getPassword())
                        // Mapeamos los roles convirtiéndolos a texto (ej: ROLE_ADMIN)
                        .roles(usuario.getRoles().stream()
                                .map(r -> r.name().replace("ROLE_", ""))
                                .toArray(String[]::new))
                        .disabled(!usuario.isActivo())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
            .requestMatchers("/login", "/error", "/css/**", "/js/**", "/images/**", "/api/calendario/**").permitAll()
            // Cambiamos hasRole por hasAuthority para evitar problemas con el prefijo "ROLE_"
            .requestMatchers("/usuarios/**").hasAuthority("ADMIN")
            .requestMatchers("/pacientes/*/consultas", "/pacientes/*/adjuntos", "/pacientes/adjuntos/*/eliminar").hasAnyAuthority("MEDICO", "ADMIN")
            .requestMatchers("/pacientes/**", "/turnos/**").authenticated()
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .defaultSuccessUrl("/turnos", true)
            .permitAll()
        )
        .logout(logout -> logout
            .logoutSuccessUrl("/login?logout")
            .permitAll()
        )
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/calendario/**")
        )
        .headers(headers -> headers.frameOptions(f -> f.sameOrigin()));
        
    return http.build();
}
    }
}
