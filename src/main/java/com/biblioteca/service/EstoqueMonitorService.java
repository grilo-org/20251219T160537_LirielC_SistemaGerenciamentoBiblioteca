package com.biblioteca.service;

import com.biblioteca.model.Livro;
import com.biblioteca.repository.LivroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço que verifica estoque de livros periodicamente e notifica gerentes quando < 5.
 */
@Service
public class EstoqueMonitorService {

    private static final int LIMIAR_ESTOQUE = 5;

    @Autowired(required = false)
    private LivroRepository livroRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private UsuarioService usuarioService;

    // Verifica a cada hora (cron: a cada 60 min)
    @Scheduled(cron = "0 0 * * * *")
    public void verificarEstoque() {
        if (livroRepository == null) return;
        List<Livro> baixos = livroRepository.findAll().stream()
                .filter(l -> l.getQuantidadeEstoque()!=null && l.getQuantidadeEstoque()<LIMIAR_ESTOQUE)
                .toList();
        if (baixos.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Os seguintes livros estão com estoque baixo ( < ").append(LIMIAR_ESTOQUE).append("):\n\n");
        baixos.forEach(l -> sb.append(l.getTitulo()).append(" (atual: ").append(l.getQuantidadeEstoque()).append(")\n"));

        // Envia email para todos os gerentes
        usuarioService.listarUsuariosWeb().stream()
                .filter(u -> "GERENTE".equalsIgnoreCase(u.getRole()))
                .filter(u -> u.getEmail()!=null)
                .forEach(ger -> {
                    if(emailService!=null){
                        try{
                            emailService.enviarEmail(ger.getEmail(), "Estoque baixo de livros", sb.toString());
                        }catch(Exception ex){
                            System.out.println("Erro ao enviar alerta de estoque: "+ex.getMessage());
                        }
                    }
                });
    }
} 