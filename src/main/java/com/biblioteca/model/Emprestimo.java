package com.biblioteca.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "emprestimos")
public class Emprestimo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    @Column(nullable = false)
    private LocalDate dataEmprestimo;

    @Column(nullable = false)
    private LocalDate dataPrevista;

    private LocalDate dataDevolucao;

    @Column(nullable = false)
    private boolean devolvido;

    @Column(nullable = false)
    private double valorEmprestimo;

    @Column(name="prazo_dias")
    private Integer prazoDias;

    @Column(name="multa_total")
    private Double multaTotal;

    @Column(nullable = false)
    private String status;

    public Emprestimo() {}

    public Emprestimo(Usuario usuario, Livro livro, LocalDate dataEmprestimo, LocalDate dataPrevista) {
        this.usuario = usuario;
        this.livro = livro;
        this.dataEmprestimo = dataEmprestimo;
        this.dataPrevista = dataPrevista;
        this.devolvido = false;
        this.valorEmprestimo = livro.getValor() * 0.10; // 10% do valor do livro
        this.status = "ATIVO";
        this.multaTotal = 0.0;
    }

    public double calcularMulta() {
        if (devolvido) {
            if (dataDevolucao.isAfter(dataPrevista)) {
                long diasAtraso = ChronoUnit.DAYS.between(dataPrevista, dataDevolucao);
                return diasAtraso * (valorEmprestimo * 0.10); // 10% do valor do empréstimo por dia de atraso
            }
        } else if (LocalDate.now().isAfter(dataPrevista)) {
            long diasAtraso = ChronoUnit.DAYS.between(dataPrevista, LocalDate.now());
            return diasAtraso * (valorEmprestimo * 0.10);
        }
        return 0.0;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Livro getLivro() {
        return livro;
    }

    public void setLivro(Livro livro) {
        this.livro = livro;
    }

    public LocalDate getDataEmprestimo() {
        return dataEmprestimo;
    }

    public void setDataEmprestimo(LocalDate dataEmprestimo) {
        this.dataEmprestimo = dataEmprestimo;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public void setDataPrevista(LocalDate dataPrevista) {
        this.dataPrevista = dataPrevista;
    }

    public LocalDate getDataDevolucao() {
        return dataDevolucao;
    }

    public void setDataDevolucao(LocalDate dataDevolucao) {
        this.dataDevolucao = dataDevolucao;
    }

    public boolean isDevolvido() {
        return devolvido;
    }

    public void setDevolvido(boolean devolvido) {
        this.devolvido = devolvido;
    }

    public double getValorEmprestimo() {
        return valorEmprestimo;
    }

    public void setValorEmprestimo(double valorEmprestimo) {
        this.valorEmprestimo = valorEmprestimo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPrazoDias() { return prazoDias; }
    public void setPrazoDias(Integer prazoDias) { this.prazoDias = prazoDias; }

    public Double getMultaTotal() { return multaTotal; }
    public void setMultaTotal(Double multaTotal) { this.multaTotal = multaTotal; }
} 