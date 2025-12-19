-- V3 - Criação das tabelas restantes para funcionamento completo do sistema
-- Este script foi gerado a partir do schema produzido automaticamente pelo Hibernate
-- Ele pressupõe que as tabelas 'usuarios' e 'livros' (V1) já existam e que a
-- coluna 'telefone' em 'usuarios' tenha sido adicionada (V2).

-- =====================
-- TABELA FORNECEDORES
-- =====================
CREATE TABLE IF NOT EXISTS fornecedores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255),
    email VARCHAR(255)
) ENGINE=InnoDB;

-- =====================
-- TABELA CLIENTES
-- =====================
CREATE TABLE IF NOT EXISTS clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cpf VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    rua VARCHAR(255),
    numero VARCHAR(50),
    complemento VARCHAR(255),
    bairro VARCHAR(255),
    cidade VARCHAR(255),
    estado VARCHAR(255),
    cep VARCHAR(20)
) ENGINE=InnoDB;

-- =====================
-- TABELA CARRINHOS
-- =====================
CREATE TABLE IF NOT EXISTS carrinhos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    total DOUBLE,
    dados_cliente_nome VARCHAR(255),
    dados_cliente_cpf VARCHAR(20),
    dados_cliente_email VARCHAR(255),
    dados_cliente_endereco VARCHAR(255),
    CONSTRAINT fk_carrinho_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================
-- TABELA LIVROS_CARRINHO
-- =====================
CREATE TABLE IF NOT EXISTS livros_carrinho (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrinho_id BIGINT,
    livro_id BIGINT NOT NULL,
    quantidade INT NOT NULL,
    CONSTRAINT fk_lc_carrinho FOREIGN KEY (carrinho_id) REFERENCES carrinhos(id) ON DELETE CASCADE,
    CONSTRAINT fk_lc_livro FOREIGN KEY (livro_id) REFERENCES livros(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================
-- TABELA EMPRESTIMOS
-- =====================
CREATE TABLE IF NOT EXISTS emprestimos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    livro_id BIGINT NOT NULL,
    data_emprestimo DATE NOT NULL,
    data_prevista DATE NOT NULL,
    data_devolucao DATE,
    devolvido TINYINT(1) NOT NULL,
    valor_emprestimo DOUBLE NOT NULL,
    prazo_dias INT,
    multa_total DOUBLE,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_emp_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_emp_livro FOREIGN KEY (livro_id) REFERENCES livros(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================
-- TABELA VENDAS
-- =====================
CREATE TABLE IF NOT EXISTS vendas (
    id VARCHAR(255) PRIMARY KEY,
    cliente_nome VARCHAR(255),
    cliente_cpf VARCHAR(20),
    cliente_email VARCHAR(255),
    cliente_endereco VARCHAR(255),
    valor_total DOUBLE,
    tipo_pagamento VARCHAR(100),
    tipo_compra VARCHAR(50),
    data_venda DATETIME NOT NULL,
    status VARCHAR(50)
) ENGINE=InnoDB;

-- =====================
-- TABELA ITENS_VENDA
-- =====================
CREATE TABLE IF NOT EXISTS itens_venda (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venda_id VARCHAR(255) NOT NULL,
    livro_id BIGINT,
    quantidade INT,
    valor_unitario DOUBLE,
    valor_total DOUBLE,
    CONSTRAINT fk_item_venda FOREIGN KEY (venda_id) REFERENCES vendas(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_livro FOREIGN KEY (livro_id) REFERENCES livros(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================
-- TABELA COMPRAS
-- =====================
CREATE TABLE IF NOT EXISTS compras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrinho_id BIGINT NOT NULL,
    fornecedor_id BIGINT NOT NULL,
    responsavel_id BIGINT NOT NULL,
    data DATE NOT NULL,
    CONSTRAINT fk_comp_carrinho FOREIGN KEY (carrinho_id) REFERENCES carrinhos(id) ON DELETE CASCADE,
    CONSTRAINT fk_comp_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedores(id) ON DELETE CASCADE,
    CONSTRAINT fk_comp_responsavel FOREIGN KEY (responsavel_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================
-- TABELA AUDITORIA
-- =====================
CREATE TABLE IF NOT EXISTS auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT,
    data DATETIME,
    nome_entidade VARCHAR(255),
    id_entidade BIGINT,
    dados_alterados LONGTEXT,
    acao VARCHAR(100),
    CONSTRAINT fk_aud_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
) ENGINE=InnoDB; 