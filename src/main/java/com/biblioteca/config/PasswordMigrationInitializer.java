package com.biblioteca.config;

import com.biblioteca.model.Usuario;
import com.biblioteca.repository.UsuarioRepository;
import com.biblioteca.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Migra senhas existentes de texto simples para BCrypt.
 * Executa após a criação do admin padrão.
 */
@Component
@Order(2)
public class PasswordMigrationInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        int migrados = 0;
        
        for (Usuario usuario : usuarios) {
            String senha = usuario.getSenha();
            // Verifica se a senha não está criptografada (não começa com $2a$)
            if (senha != null && !senha.startsWith("$2a$")) {
                // Criptografa a senha
                usuario.setSenha(AuthenticationService.encryptPassword(senha));
                usuarioRepository.save(usuario);
                migrados++;
            }
        }
        
        if (migrados > 0) {
            System.out.println("🔐 Migração de senhas: " + migrados + " usuário(s) atualizado(s) para BCrypt");
        }
    }
} 