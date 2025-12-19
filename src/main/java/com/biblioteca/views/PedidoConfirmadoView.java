package com.biblioteca.views;

import com.biblioteca.model.Venda;
import com.biblioteca.repository.VendaRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Route(value = "pedido-confirmado", layout = MainLayout.class)
@PageTitle("Pedido Confirmado | Sistema Biblioteca")
@RolesAllowed({"CLIENTE","GERENTE","ADMIN","FUNCIONARIO","USER"})
public class PedidoConfirmadoView extends VerticalLayout implements BeforeEnterObserver {

    @Autowired
    private VendaRepository vendaRepository;

    private final H3 titulo = new H3("Compra concluída com sucesso!");

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        add(titulo);
        String sessionId = eventoSessionId(event);
        if(sessionId==null || sessionId.isBlank()){
            add(new Span("ID de sessão não informado."));
            return;
        }
        vendaRepository.findById(sessionId).ifPresentOrElse(venda -> {
            // Informações sobre retirada na biblioteca
            criarSecaoRetirada(venda);
            
            // Informações da compra
            if("ALUGUEL".equalsIgnoreCase(venda.getTipoCompra())){
                java.time.LocalDate limite = venda.getDataVenda().toLocalDate().plusDays(7); // prazo padrão
                add(new Span("Devolução até "+limite.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            }
            add(new Span("Valor Total: R$ "+String.format("%.2f", venda.getValorTotal())));
            
            // Links para documentos
            Anchor nf = criarLink("NF_"+venda.getId()+".pdf", "Baixar Nota Fiscal");
            Anchor recibo = criarLink("Recibo_"+venda.getId()+".pdf", "Baixar Recibo");
            add(nf, recibo);
        }, () -> add(new Span("Venda não encontrada.")));
    }

    private void criarSecaoRetirada(Venda venda) {
        Div secaoRetirada = new Div();
        secaoRetirada.getStyle().set("background-color", "#f8f9fa")
                                .set("border", "1px solid #dee2e6")
                                .set("border-radius", "8px")
                                .set("padding", "20px")
                                .set("margin", "20px 0");

        H4 tituloRetirada = new H4("📚 Retirada dos Livros");
        tituloRetirada.getStyle().set("color", "#495057").set("margin-bottom", "15px");

        Span instrucao = new Span("Seus livros estão reservados! Compareça à biblioteca para retirá-los.");
        instrucao.getStyle().set("font-weight", "bold").set("color", "#28a745").set("display", "block").set("margin-bottom", "20px");

        // Endereço da biblioteca
        H4 tituloEndereco = new H4("📍 Endereço da Biblioteca");
        tituloEndereco.getStyle().set("color", "#495057").set("margin-top", "15px").set("margin-bottom", "10px");

        Div endereco = new Div();
        endereco.getStyle().set("line-height", "1.6");
        endereco.add(createSpanWithMargin("Biblioteca Central Universitária"));
        endereco.add(createSpanWithMargin("Rua das Letras, 123 - Centro"));
        endereco.add(createSpanWithMargin("São Paulo - SP, CEP: 01234-567"));
        endereco.add(createSpanWithMargin("Telefone: (11) 3456-7890"));
        
        // Horários de funcionamento
        H4 tituloHorarios = new H4("🕒 Horários de Funcionamento");
        tituloHorarios.getStyle().set("color", "#495057").set("margin-top", "15px").set("margin-bottom", "10px");

        Div horarios = new Div();
        horarios.getStyle().set("line-height", "1.6");
        horarios.add(createSpanWithMargin("Segunda a Sexta: 08:00 às 22:00"));
        horarios.add(createSpanWithMargin("Sábado: 09:00 às 18:00"));
        horarios.add(createSpanWithMargin("Domingo: 14:00 às 20:00"));

        // Documentos necessários
        H4 tituloDocumentos = new H4("📋 Documentos Necessários");
        tituloDocumentos.getStyle().set("color", "#495057").set("margin-top", "15px").set("margin-bottom", "10px");

        Div documentos = new Div();
        documentos.getStyle().set("line-height", "1.8");
        documentos.add(createSpanWithMargin("• Documento de identidade (RG ou CNH)"));
        documentos.add(createSpanWithMargin("• Nota fiscal (disponível para download abaixo)"));
        documentos.add(createSpanWithMargin("• Comprovante de pagamento"));

        // Aviso importante sobre nota fiscal
        Div avisoNota = new Div();
        avisoNota.getStyle().set("background-color", "#fff3cd")
                            .set("border", "1px solid #ffeaa7")
                            .set("border-radius", "5px")
                            .set("padding", "10px")
                            .set("margin-top", "15px");
        Span textoAviso = new Span("⚠️ IMPORTANTE: Não se esqueça de levar a nota fiscal impressa ou no celular!");
        textoAviso.getStyle().set("font-weight", "bold").set("color", "#856404");
        avisoNota.add(textoAviso);

        // Prazo para retirada
        Span prazoRetirada = new Span("⏰ Prazo para retirada: 7 dias úteis a partir da confirmação do pagamento");
        prazoRetirada.getStyle().set("font-weight", "bold").set("color", "#dc3545").set("margin-top", "15px").set("display", "block");

        secaoRetirada.add(tituloRetirada, instrucao, tituloEndereco, endereco, 
                         tituloHorarios, horarios, tituloDocumentos, documentos, avisoNota, prazoRetirada);

        add(secaoRetirada);
    }

    private String eventoSessionId(BeforeEnterEvent event){
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        return params.getOrDefault("session_id", List.of("")).get(0);
    }

    private Anchor criarLink(String fileName, String label){
        Anchor a = new Anchor("/api/docs/"+fileName, label);
        a.setTarget("_blank");
        return a;
    }

    private Span createSpanWithMargin(String text) {
        Span span = new Span(text);
        span.getStyle().set("display", "block").set("margin-bottom", "5px");
        return span;
    }
} 