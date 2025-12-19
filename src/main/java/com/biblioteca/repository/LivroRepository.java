package com.biblioteca.repository;

import com.biblioteca.model.Livro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA para Livro.
 */
@Repository
public interface LivroRepository extends JpaRepository<Livro, Long> {
    
    // Buscas básicas
    List<Livro> findByTituloContainingIgnoreCase(String titulo);
    List<Livro> findByAutorContainingIgnoreCase(String autor);
    List<Livro> findByIsbn(String isbn);
    
    // Busca combinada
    @Query("SELECT l FROM Livro l WHERE " +
           "(:titulo IS NULL OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))) AND " +
           "(:autor IS NULL OR LOWER(l.autor) LIKE LOWER(CONCAT('%', :autor, '%'))) AND " +
           "(:isbn IS NULL OR l.isbn = :isbn)")
    List<Livro> findByMultipleCriteria(@Param("titulo") String titulo, 
                                      @Param("autor") String autor, 
                                      @Param("isbn") String isbn);
    
    // Buscas por disponibilidade
    @Query("SELECT l FROM Livro l WHERE l.quantidadeEstoque > 0")
    List<Livro> findAvailableBooks();
    
    @Query("SELECT l FROM Livro l WHERE l.quantidadeEstoque IS NOT NULL AND l.quantidadeEstoque > 0")
    List<Livro> findBooksWithStock();
    
    // Buscas por preço
    @Query("SELECT l FROM Livro l WHERE l.valor BETWEEN :precoMin AND :precoMax")
    List<Livro> findByPrecoBetween(@Param("precoMin") Double precoMin, @Param("precoMax") Double precoMax);
    
    // Estatísticas
    @Query("SELECT COUNT(l) FROM Livro l WHERE l.quantidadeEstoque IS NOT NULL AND l.quantidadeEstoque > 0")
    long countAvailableBooks();
    
    @Query("SELECT COALESCE(SUM(l.quantidadeEstoque), 0) FROM Livro l WHERE l.quantidadeEstoque IS NOT NULL")
    Long sumTotalQuantity();
    
    @Query("SELECT COALESCE(SUM(l.quantidadeEstoque), 0) FROM Livro l WHERE l.quantidadeEstoque > 0")
    Long sumAvailableQuantity();
    
    // Verificações
    boolean existsByIsbn(String isbn);
    boolean existsByIsbnAndIdNot(String isbn, Long id);
} 