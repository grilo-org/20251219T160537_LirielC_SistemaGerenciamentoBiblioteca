package com.biblioteca.service;

import com.biblioteca.model.*;
import com.biblioteca.repository.CarrinhoRepository;
import com.biblioteca.repository.LivroRepository;
import com.biblioteca.repository.VendaRepository;
import com.biblioteca.service.DocumentoFiscalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Service para operações de Carrinho e fechamento de Venda.
 */
@Service
@Transactional
public class CarrinhoService {

    @Autowired(required = false)
    private CarrinhoRepository carrinhoRepository;

    @Autowired(required = false)
    private LivroRepository livroRepository;

    @Autowired(required = false)
    private VendaRepository vendaRepository;
    
    @Autowired(required = false)
    private AuditoriaService auditoriaService;
    
    @Autowired(required = false)
    private UsuarioService usuarioService;

    /** Obtém o carrinho ativo do cliente ou cria um novo */
    public Carrinho obterOuCriarCarrinho(Usuario cliente){
        if(carrinhoRepository!=null){
            Carrinho car = carrinhoRepository.findByCliente(cliente).orElseGet(() -> {
                Carrinho c = new Carrinho();
                c.setCliente(cliente);

                // Preenche dados do cliente para evitar campos NOT NULL nulos
                String nome = cliente.getNome()!=null ? cliente.getNome() : "";
                String cpf = cliente.getCpf()!=null ? cliente.getCpf() : "";
                String email = cliente.getEmail()!=null ? cliente.getEmail() : "";
                String endereco = cliente.getEndereco()!=null ? cliente.getEndereco() : "";
                c.setDadosCliente(new DadosCliente(nome, cpf, email, endereco));

                c.setTotal(0.0);
                Carrinho carrinhoSalvo = carrinhoRepository.save(c);
                
                // Registrar auditoria de criação do carrinho
                if (auditoriaService != null) {
                    auditoriaService.registrarOperacao(cliente, "Carrinho", carrinhoSalvo.getId(), "CRIAR_CARRINHO", 
                        "Carrinho criado para o cliente: " + cliente.getNome());
                }
                
                return carrinhoSalvo;
            });
            // Inicializa coleção para uso fora da transação
            car.getLivros().size();
            return car;
        }
        // fallback memória
        Carrinho c = new Carrinho();
        c.setCliente(cliente);
        c.setTotal(0.0);
        return c;
    }

    /** Adiciona livro ao carrinho */
    public Carrinho adicionarLivro(Carrinho carrinho, Livro livro, int quantidade){
        // atualiza quantidade estoque
        if(livro.getQuantidadeEstoque()!=null && livro.getQuantidadeEstoque() < quantidade){
            throw new RuntimeException("Estoque insuficiente");
        }
        carrinho.adicionarLivro(livro, quantidade);
        carrinho.calcularTotal();
        if(carrinhoRepository!=null){
            carrinho = carrinhoRepository.save(carrinho);
            
            // Registrar auditoria de adição de livro
            if (auditoriaService != null) {
                auditoriaService.registrarOperacao(carrinho.getCliente(), "Carrinho", carrinho.getId(), "ADICIONAR_LIVRO", 
                    String.format("Adicionado %d unidade(s) do livro '%s' ao carrinho", quantidade, livro.getTitulo()));
            }
        }
        return carrinho;
    }

    /** Remove livro ou quantidade */
    public Carrinho removerLivro(Carrinho carrinho, Livro livro, int quantidade){
        carrinho.removerLivro(livro.getTitulo(), quantidade);
        carrinho.calcularTotal();
        if(carrinhoRepository!=null){
            carrinho = carrinhoRepository.save(carrinho);
            
            // Registrar auditoria de remoção de livro
            if (auditoriaService != null) {
                auditoriaService.registrarOperacao(carrinho.getCliente(), "Carrinho", carrinho.getId(), "REMOVER_LIVRO", 
                    String.format("Removido %d unidade(s) do livro '%s' do carrinho", quantidade, livro.getTitulo()));
            }
        }
        return carrinho;
    }

    /** Limpa completamente o carrinho do usuário */
    public void limparCarrinho(Carrinho carrinho) {
        if (carrinho != null) {
            if (carrinhoRepository != null) {
                // Registrar auditoria antes de limpar
                if (auditoriaService != null) {
                    auditoriaService.registrarOperacao(carrinho.getCliente(), "Carrinho", carrinho.getId(), "LIMPAR_CARRINHO", 
                        "Carrinho completamente limpo");
                }
                
                // Remove o carrinho do banco de dados
                carrinhoRepository.delete(carrinho);
            } else {
                // Se não tem repository, limpa os itens em memória
                carrinho.getLivros().clear();
                carrinho.setTotal(0.0);
            }
        }
    }

    /** Finaliza compra: gera Venda, atualiza estoque, remove carrinho */
    public Venda finalizarCarrinho(Carrinho carrinho, String tipoPagamento){
        if(carrinho.getLivros().isEmpty()){
            throw new RuntimeException("Carrinho vazio");
        }
        // Recalcula total com base nos itens e tipo (COMPRA padrão)
        carrinho.calcularTotal("ALUGUEL".equalsIgnoreCase(tipoPagamento));

        // Cria venda
        Venda venda = new Venda();
        venda.setId(java.util.UUID.randomUUID().toString());
        venda.setClienteNome(carrinho.getCliente().getNome());
        venda.setClienteCpf(carrinho.getCliente().getCpf());
        venda.setClienteEmail(carrinho.getCliente().getEmail());
        venda.setClienteEndereco(carrinho.getCliente().getEndereco());
        venda.setValorTotal(carrinho.getTotal());
        venda.setDataVenda(java.time.LocalDateTime.now());
        venda.setTipoPagamento(tipoPagamento);
        venda.setStatus("PAGO");

        // Converte itens
        for(LivroCarrinho lc: carrinho.getLivros()){
            ItemVenda iv = new ItemVenda(lc.getLivro(), lc.getQuantidade());
            venda.addItem(iv);
            // diminuir estoque
            Livro livro = lc.getLivro();
            if(livroRepository!=null){
                Optional<Livro> opt = livroRepository.findById(livro.getId());
                if(opt.isPresent()){
                    Livro l = opt.get();
                    l.setQuantidadeEstoque(l.getQuantidadeEstoque()==null?null:l.getQuantidadeEstoque()-lc.getQuantidade());
                    livroRepository.save(l);
                }
            }
        }

        if(vendaRepository!=null){
            vendaRepository.save(venda);
        }
        
        // Registrar auditoria da finalização da compra
        if (auditoriaService != null) {
            auditoriaService.registrarOperacao(carrinho.getCliente(), "Venda", null, "FINALIZAR_COMPRA", 
                String.format("Compra finalizada. Venda ID: %s, Total: R$ %.2f, Tipo: %s", 
                    venda.getId(), venda.getValorTotal(), tipoPagamento));
        }

        // Gera documentos fiscais (pdf) de forma assíncrona simples
        new Thread(() -> {
            try {
                DocumentoFiscalService.gerarNotaFiscal(venda);
                DocumentoFiscalService.gerarRecibo(venda);
            } catch (Exception ignored) {}
        }).start();

        // Remove carrinho
        if(carrinhoRepository!=null){
            carrinhoRepository.delete(carrinho);
        }

        return venda;
    }
} 