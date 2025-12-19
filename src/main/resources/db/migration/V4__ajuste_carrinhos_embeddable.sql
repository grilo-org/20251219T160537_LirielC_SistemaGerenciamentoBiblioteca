-- V4 - Ajuste de colunas na tabela carrinhos para compatibilidade com Embeddable DadosCliente
ALTER TABLE carrinhos 
  ADD COLUMN nome VARCHAR(255),
  ADD COLUMN cpf VARCHAR(20),
  ADD COLUMN email VARCHAR(255),
  ADD COLUMN endereco VARCHAR(255); 