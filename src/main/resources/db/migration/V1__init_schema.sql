-- Versão inicial do schema do Sistema de Biblioteca
-- Gerado manualmente como ponto de partida para o Flyway

-- =====================
-- TABELA USUARIOS
-- =====================
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    login VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255),
    cpf VARCHAR(20),
    endereco VARCHAR(255),
    senha VARCHAR(255),
    role VARCHAR(50),
    ativo TINYINT(1),
    token_recuperacao VARCHAR(255)
) ENGINE=InnoDB;

-- =====================
-- TABELA LIVROS
-- =====================
CREATE TABLE IF NOT EXISTS livros (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    valor DOUBLE NOT NULL,
    quantidade_estoque INT,
    url_imagem VARCHAR(500)
) ENGINE=InnoDB;

