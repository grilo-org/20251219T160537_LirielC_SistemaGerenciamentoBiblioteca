package com.biblioteca.config;

import com.biblioteca.model.Usuario;
import com.biblioteca.repository.UsuarioRepository;
import com.biblioteca.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Garante que exista um usuário ADMIN padrão na primeira execução.
 * Login: admin  | Senha: admin (criptografada com BCrypt)
 */
@Component
@Order(1)
public class DefaultAdminInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByLogin("admin")) {
            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setLogin("admin");
            admin.setEmail("admin@sistema.local");
            admin.setSenha(AuthenticationService.encryptPassword("admin")); // Senha criptografada
            admin.setRole("ADMIN");
            admin.setStatus(Boolean.TRUE);
            usuarioRepository.save(admin);
            System.out.println("🛡️  Usuário ADMIN padrão criado (login: admin / senha: admin) - Senha criptografada");
        }
    }
} 