package com.empresa.taskmanager.security;

import com.empresa.taskmanager.model.Usuario;
import com.empresa.taskmanager.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Carga el usuario de MySQL para que Spring Security compare el hash BCrypt. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario"));
        String autoridad = "ROLE_" + usuario.getRol().getNombre().replace(' ', '_');
        return User.withUsername(usuario.getEmail())
                .password(usuario.getPassword())
                .authorities(autoridad)
                .build();
    }
}
