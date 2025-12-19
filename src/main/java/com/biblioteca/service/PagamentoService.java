package com.biblioteca.service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.biblioteca.model.*;
import com.biblioteca.util.JPAUtil;
import com.biblioteca.util.CpfValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

@Service
public class PagamentoService {
    
    private static final Logger logger = LoggerFactory.getLogger(PagamentoService.class);
    
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;
    
    private static List<HashMap<String, Object>> historicoVendas = null;

    @PostConstruct
    public void init() {
        if (stripeSecretKey != null && !stripeSecretKey.contains("SUBSTITUA")) {
            Stripe.apiKey = stripeSecretKey;
            logger.info("Stripe configurado com sucesso");
        } else {
            logger.warn("⚠️ Stripe não configurado - usando chave de exemplo");
            Stripe.apiKey = "sk_test_exemplo_nao_funcional";
        }
    }

    private static void carregarHistoricoDoBanco() {
        if (historicoVendas == null) {
            historicoVendas = new ArrayList<>();
        } else {
            historicoVendas.clear();
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Venda> query = em.createQuery(
                "SELECT v FROM Venda v ORDER BY v.dataVenda DESC",
                Venda.class
            );
            
            List<Venda> vendas = query.getResultList();
            for (Venda venda : vendas) {
                HashMap<String, Object> vendaMap = new HashMap<>();
                vendaMap.put("sessionId", venda.getId());
                vendaMap.put("cliente", venda.getClienteNome());
                vendaMap.put("cpf", venda.getClienteCpf());
                vendaMap.put("email", venda.getClienteEmail());
                vendaMap.put("valor", venda.getValorTotal());
                vendaMap.put("tipoPagamento", venda.getTipoPagamento());
                vendaMap.put("data", venda.getDataVenda());
                vendaMap.put("status", venda.getStatus());
                historicoVendas.add(vendaMap);
            }
        } catch (Exception e) {
            logger.error("Erro ao carregar histórico de vendas", e);
        } finally {
            em.close();
        }
    }

    public static void atualizarHistorico() {
        carregarHistoricoDoBanco();
    }

    public static void salvarVendaNoBanco(Venda venda) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            
            logger.info("Salvando venda com ID: {}", venda.getId());
            
            // Sempre forçar uma nova data
            LocalDateTime agora = LocalDateTime.now();
            venda.setDataVenda(agora);
            logger.debug("Data da venda definida para: {}", agora);
            logger.debug("Número de itens: {}", venda.getItens().size());
            
            // Verificar se a venda já existe
            Venda vendaExistente = em.find(Venda.class, venda.getId());
            
            if (vendaExistente != null) {
                logger.debug("Venda já existe, atualizando...");
                // Usando SQL nativo para atualização
                String sqlUpdate = "UPDATE vendas SET cliente_nome = ?, cliente_cpf = ?, cliente_email = ?, " +
                    "cliente_endereco = ?, valor_total = ?, tipo_pagamento = ?, tipo_compra = ?, data_venda = ?, status = ? " +
                    "WHERE id = ?";
                
                em.createNativeQuery(sqlUpdate)
                    .setParameter(1, venda.getClienteNome())
                    .setParameter(2, venda.getClienteCpf())
                    .setParameter(3, venda.getClienteEmail())
                    .setParameter(4, venda.getClienteEndereco())
                    .setParameter(5, venda.getValorTotal())
                    .setParameter(6, venda.getTipoPagamento())
                    .setParameter(7, venda.getTipoCompra())
                    .setParameter(8, venda.getDataVenda())
                    .setParameter(9, venda.getStatus())
                    .setParameter(10, venda.getId())
                    .executeUpdate();
                
                // Atualiza os itens da venda
                // Primeiro remove os itens existentes
                em.createNativeQuery("DELETE FROM itens_venda WHERE venda_id = ?")
                    .setParameter(1, venda.getId())
                    .executeUpdate();
                
                // Depois insere os novos itens
                for (ItemVenda item : venda.getItens()) {
                    String sqlInsertItem = "INSERT INTO itens_venda (livro_id, quantidade, valor_unitario, venda_id) " +
                        "VALUES (?, ?, ?, ?)";
                    
                    em.createNativeQuery(sqlInsertItem)
                        .setParameter(1, item.getLivro().getId())
                        .setParameter(2, item.getQuantidade())
                        .setParameter(3, item.getLivro().getValor())
                        .setParameter(4, venda.getId())
                        .executeUpdate();
                }
            } else {
                logger.debug("Venda nova, inserindo via SQL nativo...");
                // Usando SQL nativo para inserção
                String sqlInsert = "INSERT INTO vendas (id, cliente_nome, cliente_cpf, cliente_email, " +
                    "cliente_endereco, valor_total, tipo_pagamento, tipo_compra, data_venda, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                em.createNativeQuery(sqlInsert)
                    .setParameter(1, venda.getId())
                    .setParameter(2, venda.getClienteNome())
                    .setParameter(3, venda.getClienteCpf())
                    .setParameter(4, venda.getClienteEmail())
                    .setParameter(5, venda.getClienteEndereco())
                    .setParameter(6, venda.getValorTotal())
                    .setParameter(7, venda.getTipoPagamento())
                    .setParameter(8, venda.getTipoCompra())
                    .setParameter(9, venda.getDataVenda())
                    .setParameter(10, venda.getStatus())
                    .executeUpdate();
                
                // Insere os itens da venda
                for (ItemVenda item : venda.getItens()) {
                    String sqlInsertItem = "INSERT INTO itens_venda (livro_id, quantidade, valor_unitario, venda_id) " +
                        "VALUES (?, ?, ?, ?)";
                    
                    em.createNativeQuery(sqlInsertItem)
                        .setParameter(1, item.getLivro().getId())
                        .setParameter(2, item.getQuantidade())
                        .setParameter(3, item.getLivro().getValor())
                        .setParameter(4, venda.getId())
                        .executeUpdate();
                }
            }
            
            em.getTransaction().commit();
            logger.info("Venda salva com sucesso via SQL nativo!");
        } catch (Exception e) {
            logger.error("Erro detalhado ao salvar venda: {}", e.getMessage(), e);
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public static void adicionarVendaAoHistorico(HashMap<String, Object> vendaMap) {
        // Garante que a lista está inicializada
        if (historicoVendas == null) {
            historicoVendas = new ArrayList<>();
        }
        historicoVendas.add(vendaMap);
    }

    public static Session criarSessaoCheckout(Carrinho carrinho, String tipoPagamento, String cpf, boolean aluguel) {
        try {
            // Valida o CPF
            if (!CpfValidator.isValid(cpf)) {
                throw new IllegalArgumentException("CPF inválido: " + cpf);
            }
            
            // Formata o CPF
            cpf = CpfValidator.format(cpf);

            // Valida o valor total do carrinho (considerando aluguel ou compra)
            double valorTotal = carrinho.calcularTotal(aluguel);
            if (valorTotal <= 0) {
                throw new IllegalArgumentException("O valor total do carrinho deve ser maior que zero");
            }
           
            if (carrinho.getDadosCliente() == null) {
                DadosCliente dadosCliente = new DadosCliente();
                dadosCliente.setNome(carrinho.getCliente().getNome());
                dadosCliente.setCpf(cpf);
                dadosCliente.setEmail(carrinho.getCliente().getNome().toLowerCase().replace(" ", ".") + "@email.com");
                dadosCliente.setEndereco("Endereço não informado");
                carrinho.setDadosCliente(dadosCliente);
            }

            List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
            
            for (LivroCarrinho livroCarrinho : carrinho.getLivros()) {
                double valorLivro = livroCarrinho.getLivro().getValor();
                if (valorLivro <= 0) {
                    throw new IllegalArgumentException("O livro " + livroCarrinho.getLivro().getTitulo() + " tem valor inválido");
                }

                if(aluguel){
                    valorLivro = valorLivro * 0.10; // 10% do valor para aluguel
                }

                SessionCreateParams.LineItem.PriceData.ProductData productData = 
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(livroCarrinho.getLivro().getTitulo())
                        .build();

                // Garante que o valor em centavos seja um número inteiro maior que zero
                long valorCentavos = Math.max(1, Math.round(valorLivro * 100));

                SessionCreateParams.LineItem.PriceData priceData = 
                    SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("brl")
                        .setUnitAmount(valorCentavos)
                        .setProductData(productData)
                        .build();

                SessionCreateParams.LineItem lineItem = 
                    SessionCreateParams.LineItem.builder()
                        .setPriceData(priceData)
                        .setQuantity((long)livroCarrinho.getQuantidade())
                        .build();

                lineItems.add(lineItem);
            }

            
            List<SessionCreateParams.PaymentMethodType> paymentMethodTypes = new ArrayList<>();
            if ("boleto".equals(tipoPagamento)) {
                paymentMethodTypes.add(SessionCreateParams.PaymentMethodType.BOLETO);
            } else {
                paymentMethodTypes.add(SessionCreateParams.PaymentMethodType.CARD);
            }

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:8080/api/stripe/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("http://localhost:8080/carrinho")
                .addAllLineItem(lineItems)
                .addAllPaymentMethodType(paymentMethodTypes);

            if ("boleto".equals(tipoPagamento)) {
                paramsBuilder.setPaymentIntentData(
                    SessionCreateParams.PaymentIntentData.builder()
                        .putMetadata("cpf", cpf)
                        .build()
                );
            }

            // Cria a sessão do Stripe
            Session session = Session.create(paramsBuilder.build());
            System.out.println("Sessão Stripe criada com ID: " + session.getId());

            // Cria a venda no banco
            Venda venda = new Venda();
            venda.setId(session.getId());
            venda.setClienteNome(carrinho.getCliente().getNome());
            venda.setClienteCpf(cpf);
            venda.setClienteEmail(carrinho.getDadosCliente().getEmail());
            venda.setClienteEndereco(carrinho.getDadosCliente().getEndereco());
            venda.setValorTotal(carrinho.calcularTotal(aluguel));
            venda.setTipoPagamento(tipoPagamento);
            venda.setTipoCompra(aluguel?"ALUGUEL":"COMPRA");
            
            // Garantir que a data seja definida corretamente
            LocalDateTime dataAtual = LocalDateTime.now();
            venda.setDataVenda(dataAtual);
            System.out.println("Data da venda definida na criação da sessão: " + dataAtual);
            
            venda.setStatus("PENDENTE");

            // Adiciona os itens da venda
            for (LivroCarrinho livroCarrinho : carrinho.getLivros()) {
                ItemVenda item = new ItemVenda(livroCarrinho.getLivro(), livroCarrinho.getQuantidade());
                venda.addItem(item);
            }

            HashMap<String, Object> vendaMap = new HashMap<>();
            vendaMap.put("sessionId", venda.getId());
            vendaMap.put("cliente", venda.getClienteNome());
            vendaMap.put("cpf", venda.getClienteCpf());
            vendaMap.put("email", venda.getClienteEmail());
            vendaMap.put("valor", venda.getValorTotal());
            vendaMap.put("tipoPagamento", venda.getTipoPagamento());
            vendaMap.put("data", venda.getDataVenda());
            vendaMap.put("status", venda.getStatus());
            adicionarVendaAoHistorico(vendaMap);
            
            // Persiste no banco
            salvarVendaNoBanco(venda);

            return session;
        } catch (Exception e) {
            System.out.println("Erro ao criar sessão de checkout: " + e.getMessage());
            return null;
        }
    }

    public static void processarPagamento(Carrinho carrinho, String tipoPagamento, String cpf) {
        Session session = criarSessaoCheckout(carrinho, tipoPagamento, cpf, false);
        if (session != null) {
            System.out.println("\n=== Informações do Pagamento ===");
            System.out.println("ID da Sessão: " + session.getId());
            System.out.println("Status: " + session.getStatus());
            System.out.println("Valor Total: R$" + String.format("%.2f", carrinho.calcularTotal()));
            System.out.println("Cliente: " + carrinho.getCliente().getNome());
            System.out.println("CPF: " + cpf);
            System.out.println("Tipo de Pagamento: " + tipoPagamento);
            System.out.println("Link de Pagamento: " + session.getUrl());
            System.out.println("==============================");

            EntityManager em = JPAUtil.getEntityManager();
            try {
                em.getTransaction().begin();
                
                Venda venda = em.find(Venda.class, session.getId());
                if (venda != null) {
                    venda.setStatus("PAGO");
                    em.merge(venda);
                    em.getTransaction().commit();
                    
                    for (HashMap<String, Object> vendaMap : historicoVendas) {
                        if (vendaMap.get("sessionId").equals(session.getId())) {
                            vendaMap.put("status", "PAGO");
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Erro ao atualizar status da venda: " + e.getMessage());
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            } finally {
                em.close();
            }
        }
    }

    public static List<HashMap<String, Object>> getHistoricoVendas() {
        if (historicoVendas == null) {
            carregarHistoricoDoBanco();
        }
        return new ArrayList<>(historicoVendas);
    }

    public static double calcularTotalVendas() {
        if (historicoVendas == null) {
            carregarHistoricoDoBanco();
        }
        return historicoVendas.stream()
            .filter(venda -> "PAGO".equals(venda.get("status")))
            .mapToDouble(venda -> {
                Object valorObj = venda.get("valor");
                if (valorObj instanceof Double) {
                    return (Double) valorObj;
                } else if (valorObj instanceof Number) {
                    return ((Number) valorObj).doubleValue();
                }
                return 0.0;
            })
            .sum();
    }

    public static HashMap<String, Integer> contarVendasPorTipoPagamento() {
        if (historicoVendas == null) {
            carregarHistoricoDoBanco();
        }
        HashMap<String, Integer> contagem = new HashMap<>();
        for (HashMap<String, Object> venda : historicoVendas) {
            if ("PAGO".equals(venda.get("status"))) {
                String tipo = (String)venda.get("tipoPagamento");
                contagem.put(tipo, contagem.getOrDefault(tipo, 0) + 1);
            }
        }
        return contagem;
    }
}