package com.biblioteca.controller;

import com.biblioteca.service.CarrinhoService;
import com.biblioteca.service.StripeWebhookService;
import com.biblioteca.service.UsuarioService;
import com.biblioteca.service.AuditoriaService;
import com.biblioteca.service.VendaService;
import com.stripe.model.checkout.Session;
import com.biblioteca.model.Carrinho;
import com.biblioteca.model.Usuario;
import com.biblioteca.model.Venda;
import com.biblioteca.service.PagamentoService;
import com.biblioteca.repository.VendaRepository;
import com.biblioteca.service.DocumentoFiscalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    @Autowired private CarrinhoService carrinhoService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private StripeWebhookService webhookService;
    @Autowired private VendaRepository vendaRepository;
    @Autowired(required = false) private AuditoriaService auditoriaService;
    @Autowired private VendaService vendaService;

    @PostMapping("/create-session/{login}")
    public ResponseEntity<Map<String,String>> createSession(@PathVariable String login, @RequestParam String paymentType){
        Usuario usuario = usuarioService.buscarUsuarioByLoginWeb(login).orElseThrow();
        Carrinho carrinho = carrinhoService.obterOuCriarCarrinho(usuario);
        boolean aluguel = false; // endpoint default compra
        Session session = PagamentoService.criarSessaoCheckout(carrinho,paymentType,usuario.getCpf(), aluguel);
        
        // Registrar a criação da venda na auditoria
        if (session != null) {
            registrarCriacaoVendaNaAuditoria(session.getId(), usuario);
        }
        
        Map<String,String> resp = new HashMap<>();
        resp.put("url", session.getUrl());
        resp.put("id", session.getId());
        return ResponseEntity.ok(resp);
    }

    /**
     * Registra a criação de uma venda na auditoria
     */
    private void registrarCriacaoVendaNaAuditoria(String vendaId, Usuario usuario) {
        try {
            vendaRepository.findById(vendaId).ifPresent(venda -> {
                if (auditoriaService != null && usuario != null) {
                    auditoriaService.createAuditoriaInsertWeb(venda, usuario);
                }
            });
        } catch (Exception e) {
            System.out.println("Erro ao registrar criação da venda na auditoria: " + e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sig){
        StripeWebhookService.handleWebhook(payload,sig);
        return ResponseEntity.ok("received");
    }

    @GetMapping("/success")
    public org.springframework.web.servlet.view.RedirectView success(@RequestParam("session_id") String sessionId){
        vendaRepository.findById(sessionId).ifPresent(v -> {
            // Buscar o usuário para a auditoria
            Usuario usuario = null;
            if (v.getClienteCpf() != null) {
                usuario = usuarioService.buscarUsuarioPorCpf(v.getClienteCpf()).orElse(null);
            }
            
            // Usar o VendaService para atualizar com auditoria
            vendaService.atualizarStatusVenda(sessionId, "PAGO", usuario);
            
            // IMPORTANTE: Limpar o carrinho do usuário após pagamento confirmado
            if (usuario != null) {
                try {
                    Carrinho carrinho = carrinhoService.obterOuCriarCarrinho(usuario);
                    carrinhoService.limparCarrinho(carrinho);
                    System.out.println("✅ Carrinho limpo após pagamento confirmado para usuário: " + usuario.getLogin());
                } catch (Exception e) {
                    System.out.println("⚠️ Erro ao limpar carrinho: " + e.getMessage());
                }
            }
            
            // Buscar a venda atualizada para gerar documentos
            Venda vendaAtualizada = vendaRepository.findById(sessionId).orElse(v);
            DocumentoFiscalService.gerarNotaFiscal(vendaAtualizada);
            DocumentoFiscalService.gerarRecibo(vendaAtualizada);
        });
        return new org.springframework.web.servlet.view.RedirectView("/pedido-confirmado?session_id=" + sessionId);
    }
} 