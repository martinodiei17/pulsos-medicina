package com.pulsos.medicina.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/images/**", "/h2-console/**", "/api/calendario/**").permitAll()
                .requestMatchers("/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/pacientes/*/consultas", "/pacientes/*/adjuntos", "/pacientes/adjuntos/*/eliminar").hasAnyRole("MEDICO", "ADMIN")
                .requestMatchers("/pacientes/**", "/turnos/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/turnos", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/calendario/**"))
            .headers(headers -> headers.frameOptions(f -> f.sameOrigin()));

        return http.build();
    }
}
