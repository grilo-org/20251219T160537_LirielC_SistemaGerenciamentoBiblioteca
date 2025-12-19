package com.biblioteca.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carrinhos")
public class Carrinho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "carrinho_id")
    private List<LivroCarrinho> livros = new ArrayList<>();

    @Column(nullable = false)
    private Double total;

    @Embedded
    private DadosCliente dadosCliente;

    public Carrinho() {}

    public Carrinho(Long id, Usuario cliente) {
        this.id = id;
        this.cliente = cliente;
        this.total = 0.0;
    }

    public void adicionarLivro(Livro livro) {
        boolean livroExiste = false;
        for (LivroCarrinho lc : livros) {
            if (livro.getTitulo().equals(lc.getLivro().getTitulo())) {
                livroExiste = true;
                lc.setQuantidade(lc.getQuantidade() + 1);
                break;
            }
        }
        if (!livroExiste) {
            // Cria uma cópia do livro para não afetar o original
            Livro livroCopia = new Livro(livro.getTitulo());
            livroCopia.setId(livro.getId());
            livroCopia.setValor(livro.getValor());
            livros.add(new LivroCarrinho(livroCopia, 1));
        }
    }

    public void adicionarLivro(Livro livro, int quantidade) {
        boolean livroExiste = false;
        for (LivroCarrinho lc : livros) {
            if (livro.getTitulo().equals(lc.getLivro().getTitulo())) {
                livroExiste = true;
                lc.setQuantidade(lc.getQuantidade() + quantidade);
                break;
            }
        }
        if (!livroExiste) {
            // Cria uma cópia do livro para não afetar o original
            Livro livroCopia = new Livro(livro.getTitulo());
            livroCopia.setId(livro.getId());
            livroCopia.setValor(livro.getValor());
            livros.add(new LivroCarrinho(livroCopia, quantidade));
        }
    }

    public void removerLivro(String nomeLivro) {
        livros.removeIf(lc -> nomeLivro.equals(lc.getLivro().getTitulo()));
    }

    public void removerLivro(String nomeLivro, int quantidade) {
        // Usar iterator para poder remover itens com segurança
        var iterator = livros.iterator();
        while (iterator.hasNext()) {
            LivroCarrinho lc = iterator.next();
            if (nomeLivro.equals(lc.getLivro().getTitulo())) {
                lc.setQuantidade(lc.getQuantidade() - quantidade);
                if (lc.getQuantidade() <= 0) {
                    iterator.remove(); // Remove com segurança usando iterator
                }
                break; // Para depois de encontrar o livro
            }
        }
    }

    public double calcularTotal() {
        total = livros.stream()
                     .mapToDouble(LivroCarrinho::calcularValor)
                     .sum();
        return total;
    }

    /**
     * Calcula total considerando aluguel (10%).
     */
    public double calcularTotal(boolean aluguel) {
        total = livros.stream()
                     .mapToDouble(lc -> lc.calcularValor(aluguel))
                     .sum();
        return total;
    }

    // Manual getters and setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Usuario getCliente() {
        return cliente;
    }
    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }
    public List<LivroCarrinho> getLivros() {
        return livros;
    }
    public void setLivros(List<LivroCarrinho> livros) {
        this.livros = livros;
    }
    public Double getTotal() {
        return total;
    }
    public void setTotal(Double total) {
        this.total = total;
    }
    public DadosCliente getDadosCliente() {
        return dadosCliente;
    }
    public void setDadosCliente(DadosCliente dadosCliente) {
        this.dadosCliente = dadosCliente;
    }
}