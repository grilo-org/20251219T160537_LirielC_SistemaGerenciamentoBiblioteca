package com.biblioteca.repository;

import com.biblioteca.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA para Cliente.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    // Buscas básicas
    Optional<Cliente> findByCpf(String cpf);
    List<Cliente> findByNomeContainingIgnoreCase(String nome);
    Optional<Cliente> findByEmail(String email);
    
    // Buscas por endereço
    List<Cliente> findByCidadeContainingIgnoreCase(String cidade);
    List<Cliente> findByEstado(String estado);
    List<Cliente> findByCep(String cep);
    
    // Verificações de existência
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
    
    // Estatísticas
    @Query("SELECT COUNT(c) FROM Cliente c")
    long countTotalClients();
} 