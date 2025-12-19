package com.biblioteca.service;

import com.biblioteca.model.Usuario;
import com.biblioteca.model.Venda;
import com.biblioteca.repository.VendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VendaService {

    @Autowired private VendaRepository vendaRepository;
    @Autowired(required = false) private AuditoriaService auditoriaService;

    /**
     * Cria uma nova venda e registra na auditoria
     */
    public Venda criarVenda(Venda venda, Usuario usuario) {
        Venda vendaSalva = vendaRepository.save(venda);
        
        // Registrar auditoria
        if (auditoriaService != null && usuario != null) {
            auditoriaService.createAuditoriaInsertWeb(vendaSalva, usuario);
        }
        
        return vendaSalva;
    }

    /**
     * Atualiza uma venda existente e registra na auditoria
     */
    public Venda atualizarVenda(Venda venda, Usuario usuario) {
        Venda vendaAnterior = vendaRepository.findById(venda.getId()).orElse(null);
        Venda vendaAtualizada = vendaRepository.save(venda);
        
        // Registrar auditoria
        if (auditoriaService != null && usuario != null && vendaAnterior != null) {
            auditoriaService.createAuditoriaUpdateWeb(vendaAtualizada, vendaAnterior, usuario);
        }
        
        return vendaAtualizada;
    }

    /**
     * Atualiza o status de uma venda e registra na auditoria
     */
    public Venda atualizarStatusVenda(String vendaId, String novoStatus, Usuario usuario) {
        return vendaRepository.findById(vendaId).map(venda -> {
            // Criar cópia para auditoria
            Venda vendaAnterior = new Venda();
            vendaAnterior.setId(venda.getId());
            vendaAnterior.setStatus(venda.getStatus());
            vendaAnterior.setClienteNome(venda.getClienteNome());
            vendaAnterior.setClienteCpf(venda.getClienteCpf());
            vendaAnterior.setValorTotal(venda.getValorTotal());
            vendaAnterior.setDataVenda(venda.getDataVenda());
            vendaAnterior.setTipoPagamento(venda.getTipoPagamento());
            
            // Atualizar status
            venda.setStatus(novoStatus);
            Venda vendaAtualizada = vendaRepository.save(venda);
            
            // Registrar auditoria
            if (auditoriaService != null && usuario != null) {
                auditoriaService.createAuditoriaUpdateWeb(vendaAtualizada, vendaAnterior, usuario);
            }
            
            return vendaAtualizada;
        }).orElse(null);
    }
} 