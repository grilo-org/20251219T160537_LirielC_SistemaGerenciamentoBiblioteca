package com.biblioteca.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.core.annotation.Order;

/**
 * Configuração de segurança para o Sistema de Biblioteca.
 * Implementa autenticação e autorização para as rotas web.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Primeiro, libera explicitamente a rota de recuperação de senha
        http.authorizeHttpRequests(authz -> authz
            .requestMatchers("/recuperar-senha", "/recuperar-senha/**").permitAll()
        );

        // Desabilita a proteção CSRF para endpoints REST usados pelo front-end
        http.csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/**")
        );

        // Em seguida, deixe o Vaadin aplicar a configuração padrão (inclui anyRequest)
        super.configure(http);

        // Define a view de login e página de acesso negado
        setLoginView(http, com.biblioteca.views.LoginView.class, "/minha-conta");

        // Página de acesso negado
        http.exceptionHandling().accessDeniedPage("/access-denied");
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder(){
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}