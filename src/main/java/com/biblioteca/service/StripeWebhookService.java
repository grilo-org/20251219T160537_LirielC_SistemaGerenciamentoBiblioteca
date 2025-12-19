package com.biblioteca.service;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import com.biblioteca.model.Venda;
import jakarta.persistence.EntityManager;
import com.biblioteca.util.JPAUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeWebhookService {

    private static String webhookSecret;

    public StripeWebhookService(@Value("${stripe.webhook-secret}") String secret){
        webhookSecret = secret;
    }

    public static void handleWebhook(String payload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);

            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted((Session) stripeObject);
                    break;
                case "payment_intent.succeeded":
                    
                    break;
                default:
                    System.out.println("Evento não tratado: " + event.getType());
            }
        } catch (SignatureVerificationException e) {
            System.out.println("Assinatura do webhook inválida!");
            throw new RuntimeException("Assinatura do webhook inválida", e);
        } catch (Exception e) {
            System.out.println("Erro ao processar webhook: " + e.getMessage());
            throw new RuntimeException("Erro ao processar webhook", e);
        }
    }

    private static void handleCheckoutSessionCompleted(Session session) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            
            Venda venda = em.find(Venda.class, session.getId());
            if (venda != null) {
                
                venda.setStatus("PAGO");
                em.merge(venda);
                em.getTransaction().commit();

                
                DocumentoFiscalService.gerarNotaFiscal(venda);
                DocumentoFiscalService.gerarRecibo(venda);
            } else {
                throw new RuntimeException("Venda não encontrada para o ID: " + session.getId());
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Erro ao processar pagamento confirmado", e);
        } finally {
            em.close();
        }
    }
} 