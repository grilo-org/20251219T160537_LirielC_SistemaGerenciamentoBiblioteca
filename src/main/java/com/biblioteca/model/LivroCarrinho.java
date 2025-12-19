package com.biblioteca.model;

import jakarta.persistence.*;

@Entity
@Table(name = "livros_carrinho")
public class LivroCarrinho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    @Column(nullable = false)
    private int quantidade;

    public LivroCarrinho() {}

    public LivroCarrinho(Livro livro, int quantidade) {
        this.livro = livro;
        this.quantidade = quantidade;
    }

    public double calcularValor() {
        return livro.getValor() * quantidade;
    }

    /**
     * Calcula o valor considerando se é aluguel (10% do valor do livro).
     */
    public double calcularValor(boolean aluguel){
        double base = calcularValor();
        return aluguel ? base * 0.10 : base;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Livro getLivro() {
        return livro;
    }
    public void setLivro(Livro livro) {
        this.livro = livro;
    }
    public int getQuantidade() {
        return quantidade;
    }
    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }
}