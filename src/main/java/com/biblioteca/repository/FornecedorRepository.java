package com.biblioteca.repository;

import com.biblioteca.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA para Fornecedor.
 */
@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    
    // Buscas básicas
    List<Fornecedor> findByNomeContainingIgnoreCase(String nome);
    Optional<Fornecedor> findByEmail(String email);
    
    // Verificações de existência
    boolean existsByEmail(String email);
    
    // Estatísticas
    @Query("SELECT COUNT(f) FROM Fornecedor f")
    long countTotalSuppliers();
} 