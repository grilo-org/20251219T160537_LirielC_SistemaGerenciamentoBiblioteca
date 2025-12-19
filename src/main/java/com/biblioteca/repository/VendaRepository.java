package com.biblioteca.repository;

import com.biblioteca.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository Spring Data JPA para Venda.
 */
@Repository
public interface VendaRepository extends JpaRepository<Venda, String> {
    
    // Buscas por cliente
    List<Venda> findByClienteNomeContainingIgnoreCase(String nomeCliente);
    List<Venda> findByClienteCpf(String cpf);
    List<Venda> findByClienteCpfContaining(String cpf);
    
    // Buscas por status
    List<Venda> findByStatus(String status);
    @Query("SELECT v FROM Venda v WHERE v.status = 'PAGO'")
    List<Venda> findPaidSales();
    
    // Buscas por tipo de pagamento
    List<Venda> findByTipoPagamento(String tipoPagamento);
    
    // Buscas por data
    List<Venda> findByDataVendaBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT v FROM Venda v WHERE v.dataVenda >= :startDate")
    List<Venda> findSalesAfterDate(@Param("startDate") LocalDateTime startDate);
    
    // Estatísticas de vendas
    @Query("SELECT SUM(v.valorTotal) FROM Venda v WHERE v.status = 'PAGO'")
    Double sumTotalPaidSales();
    
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.status = 'PAGO'")
    long countPaidSales();
    
    @Query("SELECT AVG(v.valorTotal) FROM Venda v WHERE v.status = 'PAGO'")
    Double averageSaleValue();
    
    // Vendas por período
    @Query("SELECT SUM(v.valorTotal) FROM Venda v WHERE v.status = 'PAGO' AND v.dataVenda BETWEEN :startDate AND :endDate")
    Double sumPaidSalesBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.status = 'PAGO' AND v.dataVenda BETWEEN :startDate AND :endDate")
    long countPaidSalesBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Vendas por tipo de pagamento
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.tipoPagamento = :tipo AND v.status = 'PAGO'")
    long countPaidSalesByPaymentType(@Param("tipo") String tipo);
    
    @Query("SELECT SUM(v.valorTotal) FROM Venda v WHERE v.tipoPagamento = :tipo AND v.status = 'PAGO'")
    Double sumPaidSalesByPaymentType(@Param("tipo") String tipo);
} 