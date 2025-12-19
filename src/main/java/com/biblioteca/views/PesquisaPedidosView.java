package com.biblioteca.views;

import com.biblioteca.model.Venda;
import com.biblioteca.repository.VendaRepository;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "pesquisa-pedidos", layout = MainLayout.class)
@PageTitle("Pesquisa de Pedidos | Sistema Biblioteca")
@RolesAllowed({"FUNCIONARIO", "GERENTE", "ADMIN", "USER"})
public class PesquisaPedidosView extends VerticalLayout {

    private final VendaRepository vendaRepository;
    private final Grid<Venda> pedidosGrid;
    private final TextField cpfField;

    @Autowired
    public PesquisaPedidosView(VendaRepository vendaRepository) {
        this.vendaRepository = vendaRepository;
        this.pedidosGrid = new Grid<>(Venda.class, false);
        this.cpfField = new TextField("CPF do Cliente");

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(createTitle());
        add(createSearchSection());
        add(createGrid());
    }

    private Component createTitle() {
        H2 title = new H2("🔍 Pesquisa de Pedidos");
        title.getStyle().set("margin-bottom", "20px");
        return title;
    }

    private Component createSearchSection() {
        cpfField.setPlaceholder("Digite o CPF do cliente (ex: 12345678901 ou 123.456.789-01)");
        cpfField.setWidthFull();
        cpfField.setMaxLength(14); // Para permitir formatação com pontos e hífen
        cpfField.setPrefixComponent(VaadinIcon.USER.create());
        
        // Adiciona listener para formatar automaticamente o CPF enquanto digita
        cpfField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value != null && !value.isEmpty()) {
                // Remove tudo que não é número
                String numbers = value.replaceAll("[^0-9]", "");
                
                // Formata o CPF automaticamente
                if (numbers.length() > 0) {
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < numbers.length() && i < 11; i++) {
                        if (i == 3 || i == 6) {
                            formatted.append(".");
                        } else if (i == 9) {
                            formatted.append("-");
                        }
                        formatted.append(numbers.charAt(i));
                    }
                    
                    // Atualiza o campo sem triggerar novamente o listener
                    if (!formatted.toString().equals(value)) {
                        cpfField.setValue(formatted.toString());
                    }
                }
            }
        });

        Button pesquisarBtn = new Button("Pesquisar", VaadinIcon.SEARCH.create());
        pesquisarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        pesquisarBtn.addClickListener(e -> pesquisarPedidos());

        Button limparBtn = new Button("Limpar", VaadinIcon.REFRESH.create());
        limparBtn.addClickListener(e -> limparPesquisa());
        
        Button listarTodosBtn = new Button("Ver Todas", VaadinIcon.LIST.create());
        listarTodosBtn.addClickListener(e -> listarTodasVendas());
        listarTodosBtn.getStyle().set("margin-left", "10px");

        HorizontalLayout searchLayout = new HorizontalLayout(cpfField, pesquisarBtn, limparBtn, listarTodosBtn);
        searchLayout.setWidthFull();
        searchLayout.setVerticalComponentAlignment(Alignment.END, pesquisarBtn, limparBtn, listarTodosBtn);
        
        return searchLayout;
    }

    private Component createGrid() {
        pedidosGrid.addColumn(venda -> 
            venda.getDataVenda().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
            .setHeader("Data da Compra")
            .setSortable(true);

        pedidosGrid.addColumn(venda -> 
            venda.getClienteNome() != null ? venda.getClienteNome() : "N/A")
            .setHeader("Cliente")
            .setSortable(true);

        pedidosGrid.addColumn(venda -> 
            venda.getClienteCpf() != null ? formatarCpf(venda.getClienteCpf()) : "N/A")
            .setHeader("CPF")
            .setSortable(true);

        pedidosGrid.addColumn(venda -> 
            String.format("R$ %.2f", venda.getValorTotal()))
            .setHeader("Valor Total")
            .setSortable(true);

        pedidosGrid.addColumn(venda -> 
            venda.getTipoCompra() != null ? venda.getTipoCompra().toUpperCase() : "N/A")
            .setHeader("Tipo")
            .setSortable(true);

        pedidosGrid.addComponentColumn(this::createStatusBadge)
            .setHeader("Status")
            .setSortable(true);

        pedidosGrid.addColumn(venda -> 
            venda.getItens() != null ? venda.getItens().size() + " livros" : "0 livros")
            .setHeader("Itens")
            .setSortable(true);

        pedidosGrid.setSizeFull();
        return pedidosGrid;
    }

    private Component createStatusBadge(Venda venda) {
        Span badge = new Span(venda.getStatus());
        badge.getStyle().set("padding", "4px 8px")
                       .set("border-radius", "12px")
                       .set("font-size", "0.75rem")
                       .set("font-weight", "bold")
                       .set("text-transform", "uppercase");

        switch (venda.getStatus().toLowerCase()) {
            case "pago":
                badge.getStyle().set("background-color", "#d4edda")
                               .set("color", "#155724");
                break;
            case "pendente":
                badge.getStyle().set("background-color", "#fff3cd")
                               .set("color", "#856404");
                break;
            case "cancelado":
                badge.getStyle().set("background-color", "#f8d7da")
                               .set("color", "#721c24");
                break;
            default:
                badge.getStyle().set("background-color", "#e2e3e5")
                               .set("color", "#6c757d");
        }

        return badge;
    }

    private void pesquisarPedidos() {
        String cpf = cpfField.getValue();
        
        if (cpf == null || cpf.trim().isEmpty()) {
            Notification.show("Por favor, digite um CPF para pesquisar", 3000, 
                Notification.Position.MIDDLE);
            return;
        }

        // Remove caracteres não numéricos (pontos, hífens, espaços)
        cpf = cpf.replaceAll("[^0-9]", "");
        
        if (cpf.length() < 11) {
            Notification.show("CPF incompleto. Digite todos os 11 dígitos.", 3000, 
                Notification.Position.MIDDLE);
            return;
        }
        
        if (cpf.length() > 11) {
            Notification.show("CPF deve conter exatamente 11 dígitos", 3000, 
                Notification.Position.MIDDLE);
            return;
        }

        try {
            List<Venda> vendas = buscarVendasPorCpf(cpf);
            
            if (vendas.isEmpty()) {
                long totalVendas = vendaRepository.count();
                Notification.show("Nenhum pedido encontrado para este CPF. Total de vendas no sistema: " + totalVendas, 5000, 
                    Notification.Position.MIDDLE);
            } else {
                String nomeCliente = vendas.get(0).getClienteNome();
                Notification.show(String.format("Encontrados %d pedidos para %s", 
                    vendas.size(), nomeCliente), 3000, 
                    Notification.Position.MIDDLE);
            }
            
            pedidosGrid.setItems(vendas);
            
        } catch (Exception e) {
            Notification.show("Erro ao pesquisar pedidos: " + e.getMessage(), 5000, 
                Notification.Position.MIDDLE);
            pedidosGrid.setItems();
        }
    }

    private void limparPesquisa() {
        cpfField.clear();
        pedidosGrid.setItems();
    }
    
    private void listarTodasVendas() {
        try {
            List<Venda> todasVendas = vendaRepository.findAll();
            pedidosGrid.setItems(todasVendas);
            Notification.show("Listando todas as " + todasVendas.size() + " vendas do sistema", 3000, 
                Notification.Position.MIDDLE);
        } catch (Exception e) {
            Notification.show("Erro ao listar vendas: " + e.getMessage(), 5000, 
                Notification.Position.MIDDLE);
        }
    }
    
    private String formatarCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9, 11)
        );
    }
    
    /**
     * Busca vendas por CPF tentando diferentes formatos
     */
    private List<Venda> buscarVendasPorCpf(String cpf) {
        // Primeiro tenta busca exata com CPF apenas números
        List<Venda> vendas = vendaRepository.findByClienteCpf(cpf);
        
        if (!vendas.isEmpty()) {
            return vendas;
        }
        
        // Tenta busca com CPF formatado (com pontos e hífen)
        String cpfFormatado = formatarCpf(cpf);
        vendas = vendaRepository.findByClienteCpf(cpfFormatado);
        
        if (!vendas.isEmpty()) {
            return vendas;
        }
        
        // Tenta busca parcial caso ainda não encontre
        vendas = vendaRepository.findByClienteCpfContaining(cpf);
        
        if (!vendas.isEmpty()) {
            return vendas;
        }
        
        // Se ainda não encontrou, tenta buscar todas as vendas e comparar manualmente
        List<Venda> todasVendas = vendaRepository.findAll();
        return todasVendas.stream()
            .filter(venda -> {
                String cpfVenda = venda.getClienteCpf();
                if (cpfVenda == null) return false;
                
                // Remove formatação do CPF da venda e compara
                String cpfVendaLimpo = cpfVenda.replaceAll("[^0-9]", "");
                return cpf.equals(cpfVendaLimpo);
            })
            .toList();
    }
} 