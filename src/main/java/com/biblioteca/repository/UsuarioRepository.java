package com.biblioteca.repository;

import com.biblioteca.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA para Usuario.
 * 
 * Substitui queries manuais por métodos automáticos do Spring Data.
 * Mantém compatibilidade com UsuarioService existente.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // Métodos de busca automáticos pelo Spring Data
    Optional<Usuario> findByLogin(String login);
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByCpf(String cpf);
    List<Usuario> findByRole(String role);
    List<Usuario> findByStatus(Boolean status);
    
    // Queries customizadas para funcionalidades existentes
    @Query("SELECT u FROM Usuario u WHERE u.login = :login AND u.senha = :senha")
    Optional<Usuario> findByLoginAndSenha(@Param("login") String login, @Param("senha") String senha);
    
    @Query("SELECT u FROM Usuario u WHERE u.status = true ORDER BY u.nome")
    List<Usuario> findActiveUsersOrderByName();
    
    @Query("SELECT u FROM Usuario u WHERE u.role IN :roles AND u.status = true")
    List<Usuario> findByRolesAndActive(@Param("roles") List<String> roles);
    
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.role = :role AND u.status = true")
    long countActiveByRole(@Param("role") String role);
    
    // Verificações de existência
    boolean existsByLogin(String login);
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);
    
    // Busca por token de recuperação
    Optional<Usuario> findByTokenRecuperacao(String tokenRecuperacao);
} 