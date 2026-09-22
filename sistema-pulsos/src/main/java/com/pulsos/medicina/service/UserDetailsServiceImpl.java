package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // Asignamos una autoridad por defecto basada en su rol o una genérica si no tiene
        String rolStr = usuario.getRol() != null ? usuario.getRol().name() : "ADMIN";
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + rolStr));

        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.isActivo(), // Valida si el usuario está activo
                true,
                true,
                true,
                authorities
        );
    }
}
