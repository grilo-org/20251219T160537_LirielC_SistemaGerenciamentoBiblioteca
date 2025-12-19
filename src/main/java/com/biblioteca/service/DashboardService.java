package com.biblioteca.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {
    
    @Autowired(required = false)
    private LivroService livroService;
    
    @Autowired(required = false) 
    private UsuarioService usuarioService;
    
    @Autowired(required = false)
    private EmprestimoService emprestimoService;
    
    @Autowired(required = false)
    private ClienteService clienteService;
    
    /**
     * Retorna estatísticas básicas do sistema para o dashboard
     */
    public Map<String, Object> getEstatisticas() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total de livros            if (livroService != null) {                try {                    stats.put("totalLivros", livroService.contarLivros());                    stats.put("livrosDisponiveis", livroService.contarLivrosDisponiveis());                } catch (Exception e) {                    stats.put("totalLivros", 0L);                    stats.put("livrosDisponiveis", 0L);                }            } else {                stats.put("totalLivros", 0L);                stats.put("livrosDisponiveis", 0L);            }
            
            // Total de usuários
            if (usuarioService != null) {
                stats.put("totalUsuarios", usuarioService.contarUsuarios());
            } else {
                stats.put("totalUsuarios", 0L);
            }
            
            // Total de empréstimos
            if (emprestimoService != null) {
                stats.put("totalEmprestimos", emprestimoService.contarEmprestimos());
                stats.put("emprestimosAtivos", emprestimoService.contarEmprestimosAtivos());
            } else {
                stats.put("totalEmprestimos", 0L);
                stats.put("emprestimosAtivos", 0L);
            }
            
            // Total de clientes
            if (clienteService != null) {
                stats.put("totalClientes", clienteService.contarClientes());
            } else {
                stats.put("totalClientes", 0L);
            }
            
            // Estatísticas derivadas
            long totalLivros = (Long) stats.get("totalLivros");
            long livrosDisponiveis = (Long) stats.get("livrosDisponiveis");
            long livrosEmprestados = totalLivros - livrosDisponiveis;
            stats.put("livrosEmprestados", livrosEmprestados);
            
            // Taxa de ocupação da biblioteca
            if (totalLivros > 0) {
                double taxaOcupacao = ((double) livrosEmprestados / totalLivros) * 100;
                stats.put("taxaOcupacao", Math.round(taxaOcupacao * 100.0) / 100.0);
            } else {
                stats.put("taxaOcupacao", 0.0);
            }
            
        } catch (Exception e) {
            // Em caso de erro, retorna valores padrão
            stats.put("totalLivros", 0L);
            stats.put("livrosDisponiveis", 0L);
            stats.put("livrosEmprestados", 0L);
            stats.put("totalUsuarios", 0L);
            stats.put("totalEmprestimos", 0L);
            stats.put("emprestimosAtivos", 0L);
            stats.put("totalClientes", 0L);
            stats.put("taxaOcupacao", 0.0);
        }
        
        return stats;
    }
    
    
    public Map<String, Object> getDadosGraficos() {
        Map<String, Object> dadosGraficos = new HashMap<>();
        
        try {
            Map<String, Long> statusLivros = new HashMap<>();
            if (livroService != null) {                long disponiveis = livroService.contarLivrosDisponiveis();                long total = livroService.contarLivros();                long emprestados = total - disponiveis;
                
                statusLivros.put("Disponíveis", disponiveis);
                statusLivros.put("Emprestados", emprestados);
            } else {
                statusLivros.put("Disponíveis", 0L);
                statusLivros.put("Emprestados", 0L);
            }
            dadosGraficos.put("statusLivros", statusLivros);
            
           
            Map<String, Long> usuariosPorTipo = new HashMap<>();
            usuariosPorTipo.put("ADMIN", 0L);
            usuariosPorTipo.put("GERENTE", 0L);
            usuariosPorTipo.put("FUNCIONARIO", 0L);
            usuariosPorTipo.put("CLIENTE", 0L);
            dadosGraficos.put("usuariosPorTipo", usuariosPorTipo);
            
        } catch (Exception e) {
            // Dados padrão em caso de erro
            Map<String, Long> statusLivros = new HashMap<>();
            statusLivros.put("Disponíveis", 0L);
            statusLivros.put("Emprestados", 0L);
            dadosGraficos.put("statusLivros", statusLivros);
            
            Map<String, Long> usuariosPorTipo = new HashMap<>();
            usuariosPorTipo.put("ADMIN", 0L);
            usuariosPorTipo.put("GERENTE", 0L);
            usuariosPorTipo.put("FUNCIONARIO", 0L);
            usuariosPorTipo.put("CLIENTE", 0L);
            dadosGraficos.put("usuariosPorTipo", usuariosPorTipo);
        }
        
        return dadosGraficos;
    }
    
   
    public Map<String, Object> getAtividadesRecentes() {
        Map<String, Object> atividades = new HashMap<>();
        
        try {
            
            atividades.put("ultimosEmprestimos", "3 empréstimos hoje");
            atividades.put("novosUsuarios", "2 usuários cadastrados esta semana");
            atividades.put("livrosAdicionados", "5 livros adicionados este mês");
            
        } catch (Exception e) {
            atividades.put("ultimosEmprestimos", "Dados indisponíveis");
            atividades.put("novosUsuarios", "Dados indisponíveis");
            atividades.put("livrosAdicionados", "Dados indisponíveis");
        }
        
        return atividades;
    }
} 