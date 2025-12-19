package com.biblioteca.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "compras")
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "carrinho_id", nullable = false)
    private Carrinho carrinho;

    @ManyToOne
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @ManyToOne
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @Column(nullable = false)
    private LocalDate data;

    public Compra() {}
    public Compra(Long id, Carrinho carrinho, Fornecedor fornecedor, Usuario responsavel, LocalDate data) {
        this.id = id;
        this.carrinho = carrinho;
        this.fornecedor = fornecedor;
        this.responsavel = responsavel;
        this.data = data;
    }

    public void printCompra() {
        System.out.println("=== Relatório de Compra ===");
        System.out.println("ID da Compra: " + getId());
        System.out.println("Carrinho: ");
        System.out.println("    --");
        for (LivroCarrinho livroCarrinho : carrinho.getLivros()) {
            System.out.println("    Livro: " + livroCarrinho.getLivro().getTitulo() + 
                             " (ID: " + livroCarrinho.getLivro().getId() + ")");
            System.out.println("    Quantidade: " + livroCarrinho.getQuantidade());
            System.out.println("    --");
        }
        System.out.println("Fornecedor: " + getFornecedor().getNome() + 
                         " (ID: " + getFornecedor().getId() + ")");
        System.out.println("Responsável: " + getResponsavel().getNome() + 
                         " (ID: " + getResponsavel().getId() + ")");
        System.out.println("Data da Compra: " + getData());
        System.out.println("Total da Compra: R$" + String.format("%.2f", carrinho.calcularTotal()));
        System.out.println("============================");
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Carrinho getCarrinho() { return carrinho; }
    public void setCarrinho(Carrinho carrinho) { this.carrinho = carrinho; }
    public Fornecedor getFornecedor() { return fornecedor; }
    public void setFornecedor(Fornecedor fornecedor) { this.fornecedor = fornecedor; }
    public Usuario getResponsavel() { return responsavel; }
    public void setResponsavel(Usuario responsavel) { this.responsavel = responsavel; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
}