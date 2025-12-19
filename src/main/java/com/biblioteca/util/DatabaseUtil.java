package com.biblioteca.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {
    
    // Configurações do banco de dados (use as mesmas definidas no persistence.xml)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/biblioteca?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "reputation13";

    public static void criarTabelasSeNaoExistirem() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Conectado ao banco de dados. Verificando tabelas...");

            // Modifica a tabela de vendas para adicionar valor padrão para dataVenda
            String alterTableVendas = 
                "ALTER TABLE vendas MODIFY COLUMN data_venda DATETIME DEFAULT CURRENT_TIMESTAMP";
            
            try {
                stmt.execute(alterTableVendas);
                System.out.println("Tabela vendas modificada com sucesso!");
            } catch (SQLException e) {
                System.out.println("Erro ao modificar tabela vendas: " + e.getMessage());
            }
            
            // Verifica tabela ItemVenda
            String alterTableItensVenda = 
                "ALTER TABLE itens_venda MODIFY COLUMN venda_id VARCHAR(255) NOT NULL";
            
            try {
                stmt.execute(alterTableItensVenda);
                System.out.println("Tabela itens_venda modificada com sucesso!");
            } catch (SQLException e) {
                System.out.println("Erro ao modificar tabela itens_venda: " + e.getMessage());
            }
            
            System.out.println("Verificação e ajuste das tabelas concluído!");
            
        } catch (SQLException e) {
            System.out.println("Erro ao conectar ao banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 