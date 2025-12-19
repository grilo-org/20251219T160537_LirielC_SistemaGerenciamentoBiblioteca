package com.biblioteca.views;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;

/**
 * View de entrada que redireciona usuários baseado no seu papel
 */
@Route(value = "", layout = MainLayout.class)
@RolesAllowed({"FUNCIONARIO", "GERENTE", "ADMIN", "USER", "CLIENTE"})
public class HomeView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated()) {
            // Verificar se é cliente
            boolean isCliente = auth.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CLIENTE".equals(authority.getAuthority()));
            
            if (isCliente) {
                // Clientes vão para minha conta
                event.forwardTo("minha-conta");
            } else {
                // Funcionários, gerentes, admins vão para dashboard
                event.forwardTo("dashboard");
            }
        } else {
            // Usuário não autenticado vai para login
            event.forwardTo("login");
        }
    }
} 