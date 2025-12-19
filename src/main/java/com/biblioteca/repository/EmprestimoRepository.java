
package com.biblioteca.repository;

import com.biblioteca.model.Emprestimo;
import com.biblioteca.model.Usuario;
import com.biblioteca.model.Livro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository Spring Data JPA para Emprestimo.
 */
@Repository
public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {
    
    // Buscas por usuário
    List<Emprestimo> findByUsuario(Usuario usuario);
    List<Emprestimo> findByUsuarioId(Long usuarioId);
    
    // Buscas por livro
    List<Emprestimo> findByLivro(Livro livro);
    List<Emprestimo> findByLivroId(Long livroId);
    
    // Buscas por status
    @Query("SELECT e FROM Emprestimo e WHERE e.devolvido = false")
    List<Emprestimo> findActiveLoans();
    
    @Query("SELECT e FROM Emprestimo e WHERE e.devolvido = true")
    List<Emprestimo> findReturnedLoans();
    
    // Empréstimos atrasados
    @Query("SELECT e FROM Emprestimo e WHERE e.dataPrevista < :today AND e.devolvido = false")
    List<Emprestimo> findOverdueLoans(@Param("today") LocalDate today);
    
    // Buscas por data
    List<Emprestimo> findByDataEmprestimoBetween(LocalDate startDate, LocalDate endDate);
    List<Emprestimo> findByDataPrevistaBetween(LocalDate startDate, LocalDate endDate);
    
    // Estatísticas
    @Query("SELECT COUNT(e) FROM Emprestimo e WHERE e.devolvido = false")
    long countActiveLoans();
    
    @Query("SELECT COUNT(e) FROM Emprestimo e WHERE e.dataPrevista < :today AND e.devolvido = false")
    long countOverdueLoans(@Param("today") LocalDate today);
    
    @Query("SELECT COUNT(e) FROM Emprestimo e WHERE e.usuario = :usuario AND e.devolvido = false")
    long countActiveLoansByUser(@Param("usuario") Usuario usuario);
    
    // Verificações
    @Query("SELECT COUNT(e) > 0 FROM Emprestimo e WHERE e.usuario = :usuario AND e.livro = :livro AND e.devolvido = false")
    boolean hasActiveLoanForUserAndBook(@Param("usuario") Usuario usuario, @Param("livro") Livro livro);
    
    // Buscas para multas
    @Query("SELECT DISTINCT e.usuario FROM Emprestimo e WHERE " +
           "(e.devolvido = false AND e.dataPrevista < :today) OR " +
           "(e.devolvido = true AND e.dataDevolucao > e.dataPrevista AND e.multaTotal > 0)")
    List<Usuario> findUsersWithFines(@Param("today") LocalDate today);
    
    @Query("SELECT e FROM Emprestimo e WHERE e.usuario = :usuario AND " +
           "((e.devolvido = false AND e.dataPrevista < :today) OR " +
           "(e.devolvido = true AND e.dataDevolucao > e.dataPrevista AND e.multaTotal > 0))")
    List<Emprestimo> findLoansWithFinesByUser(@Param("usuario") Usuario usuario, @Param("today") LocalDate today);
} 