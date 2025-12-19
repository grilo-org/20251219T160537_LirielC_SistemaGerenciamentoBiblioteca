package com.biblioteca.service;

import com.biblioteca.model.Usuario;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public class AuthenticationService {

    public static String encryptPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static Boolean matchPassword(String password, String userPassword) {
        return BCrypt.checkpw(password, userPassword);
    }

    public static Optional<Usuario> authenticateUsuario(String login, String senha) {
        Usuario usuario = UsuarioService.buscarUsuario(login);
         if(Objects.nonNull(usuario) && matchPassword(senha, usuario.getSenha())) {
             System.out.println("Usuário Autenticado");
             return Optional.of(usuario);
         } else {
             System.out.println("Credenciais Inválidas");
             return Optional.empty();
         }
    }

    public static String generateAuthenticationToken(String email) {
        int leftLimit = 97; //'a'
        int rightLimit = 122; //'z'
        int targetStringLength = 20;
        Random random = new Random();

        String generatedToken = random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        // Armazena o token em texto puro (mock). Em produção, ideal seria armazenar hash.
        UsuarioService.atualizarTokenRecuperacao(email, generatedToken);
        return generatedToken;
    }

}
