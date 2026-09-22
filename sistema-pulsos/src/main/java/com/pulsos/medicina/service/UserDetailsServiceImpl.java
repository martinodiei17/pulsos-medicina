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

import java.util.ArrayList;
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

        List<GrantedAuthority> authorities = new ArrayList<>();

        // Si el usuario es admin, le damos la autoridad ADMIN que exige el SecurityConfig
        if ("admin".equals(usuario.getUsername())) {
            authorities.add(new SimpleGrantedAuthority("ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            // Para otros usuarios (como médicos o personal)
            authorities.add(new SimpleGrantedAuthority("MEDICO"));
            authorities.add(new SimpleGrantedAuthority("ROLE_MEDICO"));
        }

        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.isActivo(),
                true,
                true,
                true,
                authorities
        );
    }
}
