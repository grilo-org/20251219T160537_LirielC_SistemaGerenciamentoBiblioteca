package com.biblioteca.model;

public enum TipoCompra {
    COMPRA,
    ALUGUEL;

    public boolean isAluguel(){
        return this==ALUGUEL;
    }
} 