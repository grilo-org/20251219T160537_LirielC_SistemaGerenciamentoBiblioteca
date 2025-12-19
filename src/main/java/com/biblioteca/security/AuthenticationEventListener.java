package com.biblioteca.security;

import com.biblioteca.model.Usuario;
import com.biblioteca.service.AuditoriaService;
import com.biblioteca.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AuthenticationEventListener {

    @Autowired(required = false)
    private AuditoriaService auditoriaService;
    
    @Autowired(required = false)
    private UsuarioService usuarioService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (auditoriaService == null || usuarioService == null) {
            return;
        }

        String username = event.getAuthentication().getName();
        
        usuarioService.buscarUsuarioByLoginWeb(username).ifPresent(usuario -> {
            String detalhes = String.format("Login realizado com sucesso em %s via %s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                "Web Interface");
            
            auditoriaService.registrarLogin(usuario, true, detalhes);
        });
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        if (auditoriaService == null) {
            return;
        }

        String username = event.getAuthentication().getName();
        String detalhes = String.format("Tentativa de login falhada para usuário '%s' em %s. Motivo: %s",
            username,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
            event.getException().getMessage());

        // Para falhas, não temos o usuário válido, então passamos null
        auditoriaService.registrarLogin(null, false, detalhes);
    }

    /**
     * Método para registrar logout manualmente (chamado pelo MainLayout)
     */
    public void registrarLogout(String username) {
        if (auditoriaService == null || usuarioService == null) {
            return;
        }

        usuarioService.buscarUsuarioByLoginWeb(username).ifPresent(usuario -> {
            auditoriaService.registrarLogout(usuario);
        });
    }
} 