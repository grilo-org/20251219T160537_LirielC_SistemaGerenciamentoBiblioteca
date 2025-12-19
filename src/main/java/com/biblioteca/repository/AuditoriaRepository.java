package com.biblioteca.repository;

import com.biblioteca.model.Auditoria;
import com.biblioteca.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository Spring Data JPA para Auditoria.
 */
@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    
    // Buscas por entidade
    List<Auditoria> findByNomeEntidade(String nomeEntidade);
    List<Auditoria> findByIdEntidade(Long idEntidade);
    List<Auditoria> findByNomeEntidadeAndIdEntidade(String nomeEntidade, Long idEntidade);
      // Buscas por ação
    List<Auditoria> findByAcao(String acao);
    
    // Buscas por usuário
    @Query("SELECT a FROM Auditoria a WHERE a.usuario = :usuario")
    List<Auditoria> findByUsuarioEntity(@Param("usuario") Usuario usuario);
    
    @Query("SELECT a FROM Auditoria a WHERE a.usuario.login = :username")
    List<Auditoria> findByUsuarioLogin(@Param("username") String username);
    
    // Buscas por data
    List<Auditoria> findByDataBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT a FROM Auditoria a WHERE a.data >= :startDate ORDER BY a.data DESC")
    List<Auditoria> findRecentAudits(@Param("startDate") LocalDateTime startDate);
    
    // Estatísticas
    @Query("SELECT COUNT(a) FROM Auditoria a WHERE a.acao = :acao")
    long countByAction(@Param("acao") String acao);
    
    @Query("SELECT COUNT(a) FROM Auditoria a WHERE a.usuario.login = :username")
    long countByUser(@Param("username") String username);
    
    // Últimas ações por entidade
    @Query("SELECT a FROM Auditoria a WHERE a.nomeEntidade = :entidade ORDER BY a.data DESC")
    List<Auditoria> findLastActionsByEntity(@Param("entidade") String entidade);
} 