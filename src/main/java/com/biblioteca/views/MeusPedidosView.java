package com.biblioteca.views;

import com.biblioteca.model.Venda;
import com.biblioteca.model.Usuario;
import com.biblioteca.repository.VendaRepository;
import com.biblioteca.service.UsuarioService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "meus-pedidos", layout = MainLayout.class)
@PageTitle("Meus Pedidos | Sistema Biblioteca")
@RolesAllowed({"CLIENTE", "GERENTE", "ADMIN", "FUNCIONARIO", "USER"})
public class MeusPedidosView extends VerticalLayout {

    private final VendaRepository vendaRepository;
    private final UsuarioService usuarioService;
    private final Grid<Venda> grid = new Grid<>(Venda.class, false);

    @Autowired
    public MeusPedidosView(VendaRepository vendaRepository, UsuarioService usuarioService) {
        this.vendaRepository = vendaRepository;
        this.usuarioService = usuarioService;
        
        configurarLayout();
        configurarGrid();
        carregarPedidos();
    }

    private void configurarLayout() {
        H3 titulo = new H3("📋 Meus Pedidos");
        titulo.getStyle().set("color", "#495057").set("margin-bottom", "20px");
        add(titulo);
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    private void configurarGrid() {
        // Coluna da data
        grid.addColumn(venda -> 
            venda.getDataVenda().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        ).setHeader("Data do Pedido").setAutoWidth(true);

        // Coluna do valor
        grid.addColumn(venda -> 
            "R$ " + String.format("%.2f", venda.getValorTotal())
        ).setHeader("Valor Total").setAutoWidth(true);

        // Coluna do tipo
        grid.addColumn(venda -> {
            String tipo = venda.getTipoCompra();
            return tipo != null ? tipo : "COMPRA";
        }).setHeader("Tipo").setAutoWidth(true);

        // Coluna do status
        grid.addColumn(new ComponentRenderer<>(venda -> {
            Span statusSpan = new Span(venda.getStatus());
            if ("PAGO".equals(venda.getStatus())) {
                statusSpan.getStyle().set("color", "#28a745").set("font-weight", "bold");
            } else if ("PENDENTE".equals(venda.getStatus())) {
                statusSpan.getStyle().set("color", "#ffc107").set("font-weight", "bold");
            } else {
                statusSpan.getStyle().set("color", "#dc3545").set("font-weight", "bold");
            }
            return statusSpan;
        })).setHeader("Status").setAutoWidth(true);

        // Coluna de ações (documentos)
        grid.addColumn(new ComponentRenderer<>(venda -> {
            HorizontalLayout acoes = new HorizontalLayout();
            acoes.setSpacing(true);

            if ("PAGO".equals(venda.getStatus())) {
                Anchor notaFiscal = new Anchor("/api/docs/NF_" + venda.getId() + ".pdf", "📄 Nota Fiscal");
                notaFiscal.setTarget("_blank");
                notaFiscal.getStyle().set("margin-right", "10px");

                Anchor recibo = new Anchor("/api/docs/Recibo_" + venda.getId() + ".pdf", "🧾 Recibo");
                recibo.setTarget("_blank");

                acoes.add(notaFiscal, recibo);
            } else {
                Span pendente = new Span("Pagamento pendente");
                pendente.getStyle().set("font-style", "italic").set("color", "#6c757d");
                acoes.add(pendente);
            }

            return acoes;
        })).setHeader("Documentos").setAutoWidth(true).setFlexGrow(1);

        // Informações de devolução para aluguéis
        grid.addColumn(new ComponentRenderer<>(venda -> {
            if ("ALUGUEL".equalsIgnoreCase(venda.getTipoCompra()) && "PAGO".equals(venda.getStatus())) {
                java.time.LocalDate limite = venda.getDataVenda().toLocalDate().plusDays(7);
                Span devolucao = new Span("Devolver até: " + limite.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                devolucao.getStyle().set("color", "#dc3545").set("font-weight", "bold").set("font-size", "0.875em");
                return devolucao;
            }
            return new Span("");
        })).setHeader("Devolução").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("600px");
        grid.setPageSize(20);
        
        add(grid);
    }

    private void carregarPedidos() {
        Usuario usuario = getUsuarioLogado();
        if (usuario != null && usuario.getCpf() != null) {
            List<Venda> vendas = vendaRepository.findByClienteCpf(usuario.getCpf());
            vendas.sort((v1, v2) -> v2.getDataVenda().compareTo(v1.getDataVenda())); // Mais recentes primeiro
            grid.setItems(vendas);
            
            if (vendas.isEmpty()) {
                Span mensagem = new Span("Você ainda não possui pedidos realizados.");
                mensagem.getStyle().set("font-style", "italic").set("color", "#6c757d").set("text-align", "center");
                add(mensagem);
            }
        } else {
            Span erro = new Span("Não foi possível carregar seus pedidos. Verifique se seu CPF está cadastrado.");
            erro.getStyle().set("color", "#dc3545").set("text-align", "center");
            add(erro);
        }
    }

    private Usuario getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String login = auth.getName();
        return usuarioService.buscarUsuarioByLoginWeb(login).orElse(null);
    }
} 