-- Atualiza a role USER para FUNCIONARIO nos usuários existentes
UPDATE usuarios 
SET role = 'FUNCIONARIO' 
WHERE role = 'USER';

