-- Adiciona campos ISBN e autor à tabela livros
ALTER TABLE livros 
ADD COLUMN isbn VARCHAR(20) UNIQUE,
ADD COLUMN autor VARCHAR(200);

-- Adiciona índice para busca por autor
CREATE INDEX idx_livros_autor ON livros(autor);

-- Adiciona comentários
ALTER TABLE livros 
MODIFY COLUMN isbn VARCHAR(20) COMMENT 'ISBN-10 ou ISBN-13 do livro',
MODIFY COLUMN autor VARCHAR(200) COMMENT 'Nome do autor do livro'; 