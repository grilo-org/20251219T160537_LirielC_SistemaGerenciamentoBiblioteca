package com.biblioteca.views;

import com.biblioteca.service.DashboardService;
import com.biblioteca.service.LivroService;
import com.biblioteca.service.UsuarioService;
import com.biblioteca.service.EmprestimoService;
import com.biblioteca.repository.VendaRepository;
import com.biblioteca.repository.AuditoriaRepository;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import jakarta.annotation.security.RolesAllowed;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import com.biblioteca.model.Livro;

/**
 * Dashboard principal da aplicação web.
 * Exibe métricas e estatísticas importantes.
 */
@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard | Sistema Biblioteca")
@RolesAllowed({"FUNCIONARIO", "GERENTE", "ADMIN", "USER"})
public class DashboardView extends VerticalLayout {
    
    private final LivroService livroService;
    private final UsuarioService usuarioService;
    private final EmprestimoService emprestimoService;
    private final DashboardService dashboardService;
    private final VendaRepository vendaRepository;
    private final AuditoriaRepository auditoriaRepository;

    @Autowired
    public DashboardView(LivroService livroService,
                         UsuarioService usuarioService,
                         EmprestimoService emprestimoService,
                         @Autowired(required = false) DashboardService dashboardService,
                         @Autowired(required = false) VendaRepository vendaRepository,
                         @Autowired(required = false) AuditoriaRepository auditoriaRepository) {
        this.livroService = livroService;
        this.usuarioService = usuarioService;
        this.emprestimoService = emprestimoService;
        this.dashboardService = dashboardService;
        this.vendaRepository = vendaRepository;
        this.auditoriaRepository = auditoriaRepository;

        setPadding(true);
        setSpacing(true);

        add(createTitle());
        add(createMetricsBoard());
        
        // Controle de acesso baseado em roles
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        boolean isGerente = hasRole(auth, "ROLE_GERENTE");
        boolean isFuncionario = hasRole(auth, "ROLE_FUNCIONARIO") || hasRole(auth, "ROLE_USER");
        
        // Gráficos básicos para todos
        add(createChartsSection());
        
        // Seção específica para GERENTE (gestão de estoque e fornecedores)
        if (isGerente) {
            add(createGerenteSection());
        }
        
        // Vendas só para ADMIN
        if (isAdmin) {
            add(createVendasSection());
        }
        
        // Auditoria só para ADMIN  
        if (isAdmin) {
            add(createAuditoriaSection());
        }
        setSizeFull();
    }
    
    private Component createTitle() {
        H2 title = new H2("📊 Dashboard do Sistema");
        title.addClassName("mb-0");
        return title;
    }
    
    private Component createMetricsBoard() {
        HorizontalLayout metricsLayout = new HorizontalLayout();
        metricsLayout.setWidthFull();
        metricsLayout.setSpacing(true);
        
        boolean admin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        metricsLayout.add(createMetricCard("📚 Total de Livros", getTotalLivros(), VaadinIcon.BOOK));

        if(admin){
            metricsLayout.add(createMetricCard("👥 Total de Usuários", getTotalUsuarios(), VaadinIcon.USERS));
            metricsLayout.add(createMetricCard("⚠️ Empréstimos em Atraso", getEmprestimosAtraso(), VaadinIcon.WARNING));
        }

        metricsLayout.add(createMetricCard("📋 Empréstimos Ativos", getEmprestimosAtivos(), VaadinIcon.HANDSHAKE));
        
        return metricsLayout;
    }
    
    private Component createMetricCard(String title, String value, VaadinIcon iconType) {
        Icon icon = iconType.create();
        icon.setSize("24px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");
        
        H3 titleElement = new H3(title);
        titleElement.addClassName("mb-0");
        
        Span valueElement = new Span(value);
        valueElement.addClassNames("text-2xl", "font-semibold");
        valueElement.getStyle().set("color", "var(--lumo-primary-text-color)");
        
        HorizontalLayout header = new HorizontalLayout(icon, titleElement);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        
        VerticalLayout card = new VerticalLayout(header, valueElement);
        card.setPadding(true);
        card.setSpacing(false);
        card.addClassName("border");
        card.getStyle()
            .set("border-radius", "var(--lumo-border-radius)")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)");
        
        return card;
    }
    
    private Component createChartsSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 title = new H3("📈 Estatísticas Visuais");

        // Criar visualização simples do estoque de livros
        Component estoqueCard = createEstoqueVisualization();
        
        // Criar visualização simples dos empréstimos
        Component emprestimosCard = createEmprestimosVisualization();

        HorizontalLayout chartsRow = new HorizontalLayout(estoqueCard, emprestimosCard);
        chartsRow.setWidthFull();
        chartsRow.setSpacing(true);

        layout.add(title, chartsRow);
        return layout;
    }

    private Component createEstoqueVisualization() {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.addClassName("border");
        card.getStyle()
            .set("border-radius", "var(--lumo-border-radius)")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)")
            .set("min-height", "200px");

        H3 title = new H3("📚 Estoque de Livros");
        title.getStyle().set("margin-top", "0");

        try {
            long totalLivros = livroService.contarLivros();
            long livrosDisponiveis = livroService.contarLivrosDisponiveis();
            
            if (totalLivros > 0) {
                double percentualDisponivel = (double) livrosDisponiveis / totalLivros;
                
                ProgressBar progressBar = new ProgressBar();
                progressBar.setValue(percentualDisponivel);
                progressBar.setWidth("100%");
                
                // Buscar a quantidade total de itens em estoque
                Long quantidadeTotal = 0L;
                try {
                    quantidadeTotal = livroService.listarTodos().stream()
                        .mapToLong(l -> l.getQuantidadeEstoque() != null ? l.getQuantidadeEstoque() : 0)
                        .sum();
                        } catch (Exception ex) {
            // Log adequado seria melhor aqui
            ex.printStackTrace();
                }
                
                Span info = new Span(String.format("Tipos disponíveis: %d de %d livros (%.1f%%)", 
                    livrosDisponiveis, totalLivros, percentualDisponivel * 100));
                info.getStyle().set("font-size", "var(--lumo-font-size-s)");

                Span quantidadeInfo = new Span(String.format("Quantidade total em estoque: %d itens", quantidadeTotal));
                quantidadeInfo.getStyle().set("font-size", "var(--lumo-font-size-s)")
                    .set("font-weight", "bold")
                    .set("color", "var(--lumo-primary-color)");

                card.add(title, progressBar, info, quantidadeInfo);
            } else {
                Span empty = new Span("Nenhum livro cadastrado");
                empty.getStyle().set("font-style", "italic").set("color", "#6c757d");
                card.add(title, empty);
            }
        } catch (Exception e) {
            Span error = new Span("Erro ao carregar dados");
            error.getStyle().set("color", "#dc3545");
            card.add(title, error);
        }

        return card;
    }

    private Component createEmprestimosVisualization() {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.addClassName("border");
        card.getStyle()
            .set("border-radius", "var(--lumo-border-radius)")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)")
            .set("min-height", "200px");

        H3 title = new H3("📋 Status dos Empréstimos");
        title.getStyle().set("margin-top", "0");

        try {
            int emprestimosAtivos = emprestimoService.listarTodosEmprestimosAtivosWeb().size();
            int emprestimosAtrasados = emprestimoService.listarEmprestimosEmAtraso().size();
            int totalEmprestimos = emprestimosAtivos + emprestimosAtrasados;

            if (totalEmprestimos > 0) {
                // Barra de progresso para empréstimos em dia
                double percentualEmDia = totalEmprestimos > 0 ? (double)(emprestimosAtivos - emprestimosAtrasados) / totalEmprestimos : 0;
                if (percentualEmDia < 0) percentualEmDia = 0;
                
                ProgressBar progressBar = new ProgressBar();
                progressBar.setValue(percentualEmDia);
                progressBar.setWidth("100%");
                
                if (percentualEmDia >= 0.8) {
                    progressBar.getStyle().set("--vaadin-progress-color", "#28a745");
                } else if (percentualEmDia >= 0.5) {
                    progressBar.getStyle().set("--vaadin-progress-color", "#ffc107");
                } else {
                    progressBar.getStyle().set("--vaadin-progress-color", "#dc3545");
                }

                Div statusDiv = new Div();
                statusDiv.add(new Span("✅ Em dia: " + (emprestimosAtivos - emprestimosAtrasados)));
                statusDiv.add(new Span("⚠️ Atrasados: " + emprestimosAtrasados));
                statusDiv.getStyle().set("display", "flex").set("justify-content", "space-between")
                    .set("font-size", "var(--lumo-font-size-s)");

                card.add(title, progressBar, statusDiv);
            } else {
                Span empty = new Span("Nenhum empréstimo ativo");
                empty.getStyle().set("font-style", "italic").set("color", "#6c757d");
                card.add(title, empty);
            }
        } catch (Exception e) {
            Span error = new Span("Erro ao carregar dados");
            error.getStyle().set("color", "#dc3545");
            card.add(title, error);
        }

        return card;
    }

    private Component createVendasSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 title = new H3("💰 Dashboard de Vendas");

        HorizontalLayout vendasCards = new HorizontalLayout();
        vendasCards.setWidthFull();
        vendasCards.setSpacing(true);

        if (vendaRepository != null) {
            try {
                // Total de vendas pagas
                Double totalVendas = vendaRepository.sumTotalPaidSales();
                long quantidadeVendas = vendaRepository.countPaidSales();
                Double ticketMedio = vendaRepository.averageSaleValue();

                // Cards de métricas de vendas
                vendasCards.add(createVendaMetricCard("💵 Total Vendas", 
                    totalVendas != null ? String.format("R$ %.2f", totalVendas) : "R$ 0,00", 
                    VaadinIcon.MONEY));

                vendasCards.add(createVendaMetricCard("📦 Quantidade", 
                    String.valueOf(quantidadeVendas) + " vendas", 
                    VaadinIcon.CART));

                vendasCards.add(createVendaMetricCard("🎯 Ticket Médio", 
                    ticketMedio != null ? String.format("R$ %.2f", ticketMedio) : "R$ 0,00", 
                    VaadinIcon.TRENDING_UP));

                // Vendas por tipo de pagamento
                HorizontalLayout pagamentoLayout = new HorizontalLayout();
                pagamentoLayout.setWidthFull();
                pagamentoLayout.setSpacing(true);

                Component cartaoCard = createPagamentoCard("Cartão", "card");
                Component boletoCard = createPagamentoCard("Boleto", "boleto");

                pagamentoLayout.add(cartaoCard, boletoCard);

                layout.add(title, vendasCards, pagamentoLayout);
            } catch (Exception e) {
                Span error = new Span("Erro ao carregar dados de vendas: " + e.getMessage());
                error.getStyle().set("color", "#dc3545");
                layout.add(title, error);
            }
        } else {
            Span noData = new Span("Sistema de vendas não disponível");
            noData.getStyle().set("font-style", "italic").set("color", "#6c757d");
            layout.add(title, noData);
        }

        return layout;
    }

    private Component createVendaMetricCard(String titulo, String valor, VaadinIcon iconType) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.addClassName("border");
        card.getStyle()
            .set("border-radius", "var(--lumo-border-radius)")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)")
            .set("text-align", "center")
            .set("min-width", "200px");

        Icon icon = iconType.create();
        icon.setSize("32px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");

        H3 titleElement = new H3(titulo);
        titleElement.getStyle().set("margin", "10px 0").set("font-size", "1rem");

        Span valueElement = new Span(valor);
        valueElement.getStyle().set("font-size", "1.5rem").set("font-weight", "bold")
            .set("color", "var(--lumo-primary-text-color)");

        card.add(icon, titleElement, valueElement);
        card.setAlignItems(FlexComponent.Alignment.CENTER);

        return card;
    }

    private Component createPagamentoCard(String tipoPagamento, String tipo) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.addClassName("border");
        card.getStyle()
            .set("border-radius", "var(--lumo-border-radius)")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)");

        H3 title = new H3("💳 " + tipoPagamento);
        title.getStyle().set("margin-top", "0");

        try {
            if (vendaRepository != null) {
                long quantidade = vendaRepository.countPaidSalesByPaymentType(tipo);
                Double valor = vendaRepository.sumPaidSalesByPaymentType(tipo);

                Span quantidadeSpan = new Span("Quantidade: " + quantidade);
                Span valorSpan = new Span("Total: " + (valor != null ? String.format("R$ %.2f", valor) : "R$ 0,00"));

                card.add(title, quantidadeSpan, valorSpan);
            }
        } catch (Exception e) {
            Span error = new Span("Erro ao carregar dados");
            error.getStyle().set("color", "#dc3545");
            card.add(title, error);
        }

        return card;
    }

    private Component createAuditoriaSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 title = new H3("🔍 Status da Auditoria");

        if (auditoriaRepository != null) {
            try {
                long totalAuditorias = auditoriaRepository.count();
                java.time.LocalDateTime umDiaAtras = java.time.LocalDateTime.now().minusDays(1);
                List<com.biblioteca.model.Auditoria> auditorias24h = auditoriaRepository.findRecentAudits(umDiaAtras);

                HorizontalLayout auditoriaCards = new HorizontalLayout();
                auditoriaCards.setWidthFull();
                auditoriaCards.setSpacing(true);

                auditoriaCards.add(createVendaMetricCard("📋 Total Registros", 
                    String.valueOf(totalAuditorias), VaadinIcon.CLIPBOARD_TEXT));

                auditoriaCards.add(createVendaMetricCard("🕒 Últimas 24h", 
                    String.valueOf(auditorias24h.size()) + " ações", VaadinIcon.CLOCK));

                // Últimas ações
                VerticalLayout ultimasAcoes = new VerticalLayout();
                ultimasAcoes.setPadding(true);
                ultimasAcoes.setSpacing(false);
                ultimasAcoes.addClassName("border");
                ultimasAcoes.getStyle()
                    .set("border-radius", "var(--lumo-border-radius)")
                    .set("background", "var(--lumo-base-color)")
                    .set("box-shadow", "var(--lumo-box-shadow-xs)")
                    .set("max-height", "200px")
                    .set("overflow-y", "auto");

                H3 ultimasAcoesTitle = new H3("🔄 Últimas Ações");
                ultimasAcoesTitle.getStyle().set("margin-top", "0");
                ultimasAcoes.add(ultimasAcoesTitle);

                List<com.biblioteca.model.Auditoria> ultimasAcoesLista = auditoriaRepository.findAll()
                    .stream()
                    .sorted((a1, a2) -> a2.getData().compareTo(a1.getData()))
                    .limit(5)
                    .toList();

                for (com.biblioteca.model.Auditoria auditoria : ultimasAcoesLista) {
                    String usuario = auditoria.getUsuario() != null ? auditoria.getUsuario().getLogin() : "Sistema";
                    String acao = String.format("%s - %s (ID: %s) por %s", 
                        auditoria.getAcao(), 
                        auditoria.getNomeEntidade(), 
                        auditoria.getIdEntidade(), 
                        usuario);
                    
                    Span acaoSpan = new Span(acao);
                    acaoSpan.getStyle().set("font-size", "0.875rem")
                        .set("margin-bottom", "5px")
                        .set("display", "block");
                    ultimasAcoes.add(acaoSpan);
                }

                layout.add(title, auditoriaCards, ultimasAcoes);
            } catch (Exception e) {
                Span error = new Span("Erro ao carregar dados de auditoria: " + e.getMessage());
                error.getStyle().set("color", "#dc3545");
                layout.add(title, error);
            }
        } else {
            Span noData = new Span("✅ Sistema de auditoria funcionando (dados não disponíveis na interface web)");
            noData.getStyle().set("color", "#28a745");
            layout.add(title, noData);
        }

        return layout;
    }
    
    // Métodos para buscar métricas
    private String getTotalLivros() {
        try {
            return String.valueOf(livroService.contarLivros());
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    private String getTotalUsuarios() {
        try {
            return String.valueOf(usuarioService.listarUsuariosWeb().size());
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    private String getEmprestimosAtivos() {
        try {
            return String.valueOf(emprestimoService.listarTodosEmprestimosAtivosWeb().size());
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    private String getEmprestimosAtraso() {
        try {
            return String.valueOf(emprestimoService.listarEmprestimosEmAtraso().size());
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    private Component createGerenteSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        H3 title = new H3("🔧 Gestão - Área do Gerente");

        HorizontalLayout gerenteCards = new HorizontalLayout();
        gerenteCards.setWidthFull();
        gerenteCards.setSpacing(true);

        try {
            // Card de estoque zerado (livros sem estoque)
            long totalLivros = livroService.contarLivros();
            long livrosDisponiveis = livroService.contarLivrosDisponiveis();
            long livrosSemEstoque = totalLivros - livrosDisponiveis;
            
            gerenteCards.add(createVendaMetricCard("⚠️ Sem Estoque", 
                String.valueOf(livrosSemEstoque) + " livros", VaadinIcon.WARNING));

            // Card de estoque baixo (< 5 unidades)
            List<Livro> livrosEstoqueBaixo = livroService.listarTodos().stream()
                .filter(l -> l.getQuantidadeEstoque() != null && l.getQuantidadeEstoque() > 0 && l.getQuantidadeEstoque() < 5)
                .toList();
            
            Component cardEstoqueBaixo = createEstoqueBaixoCard(livrosEstoqueBaixo);
            gerenteCards.add(cardEstoqueBaixo);

            // Card de empréstimos ativos  
            long emprestimosAtivos = emprestimoService.listarTodosEmprestimosAtivosWeb().size();
            gerenteCards.add(createVendaMetricCard("📋 Empréstimos Ativos", 
                String.valueOf(emprestimosAtivos), VaadinIcon.HANDSHAKE));

            layout.add(title, gerenteCards);
            
            // Seção de alertas detalhados se houver estoque baixo
            if (!livrosEstoqueBaixo.isEmpty()) {
                Component alertasDetalhados = createAlertasEstoqueBaixo(livrosEstoqueBaixo);
                layout.add(alertasDetalhados);
            }
            
        } catch (Exception e) {
            Span error = new Span("Erro ao carregar dados de gestão: " + e.getMessage());
            error.getStyle().set("color", "#dc3545");
            layout.add(title, error);
        }

        return layout;
    }
    
    private Component createEstoqueBaixoCard(List<Livro> livrosEstoqueBaixo) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(true);
        card.addClassName("border");
        card.getStyle()
            .set("border-radius", "var(--lumo-border-radius)")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)")
            .set("border-left", "4px solid #ffc107"); // Borda amarela para alerta

        Icon icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
        icon.setSize("24px");
        icon.getStyle().set("color", "#ffc107"); // Amarelo para alerta

        H3 titleElement = new H3("🚨 Estoque Baixo");
        titleElement.getStyle().set("margin", "10px 0").set("font-size", "1rem").set("color", "#ffc107");

        String valor = livrosEstoqueBaixo.size() + " livros";
        Span valueElement = new Span(valor);
        valueElement.getStyle().set("font-size", "1.5rem").set("font-weight", "bold")
            .set("color", "#ffc107");

        card.add(icon, titleElement, valueElement);
        card.setAlignItems(FlexComponent.Alignment.CENTER);

        return card;
    }
    
    private Component createAlertasEstoqueBaixo(List<Livro> livrosEstoqueBaixo) {
        VerticalLayout alertSection = new VerticalLayout();
        alertSection.setPadding(true);
        alertSection.setSpacing(true);
        alertSection.addClassName("border");
        alertSection.getStyle()
            .set("border-radius", "var(--lumo-border-radius)")
            .set("background", "#fff3cd") // Fundo amarelo claro
            .set("border", "1px solid #ffc107")
            .set("margin-top", "15px");

        H3 alertTitle = new H3("🚨 Atenção: Livros com Estoque Baixo (< 5 unidades)");
        alertTitle.getStyle()
            .set("color", "#856404")
            .set("margin-top", "0")
            .set("margin-bottom", "10px");

        VerticalLayout livrosList = new VerticalLayout();
        livrosList.setSpacing(false);
        livrosList.setPadding(false);

        for (Livro livro : livrosEstoqueBaixo.stream().limit(5).toList()) { // Máximo 5 para não poluir
            HorizontalLayout livroItem = new HorizontalLayout();
            livroItem.setAlignItems(FlexComponent.Alignment.CENTER);
            livroItem.setWidthFull();

            Span tituloSpan = new Span("📚 " + livro.getTitulo());
            tituloSpan.getStyle().set("font-weight", "500").set("flex", "1");

            Span estoqueSpan = new Span("Estoque: " + livro.getQuantidadeEstoque());
            estoqueSpan.getStyle()
                .set("color", "#dc3545")
                .set("font-weight", "bold")
                .set("background", "#f8d7da")
                .set("padding", "2px 8px")
                .set("border-radius", "12px")
                .set("font-size", "0.875rem");

            livroItem.add(tituloSpan, estoqueSpan);
            livrosList.add(livroItem);
        }

        if (livrosEstoqueBaixo.size() > 5) {
            Span maisLivros = new Span("... e mais " + (livrosEstoqueBaixo.size() - 5) + " livros");
            maisLivros.getStyle()
                .set("font-style", "italic")
                .set("color", "#856404")
                .set("text-align", "center")
                .set("margin-top", "10px");
            livrosList.add(maisLivros);
        }

        // Botão para ação rápida
        com.vaadin.flow.component.button.Button solicitarBtn = new com.vaadin.flow.component.button.Button(
            "📧 Solicitar Reposição aos Fornecedores", 
            e -> solicitarReposicaoRapida(livrosEstoqueBaixo)
        );
        solicitarBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        solicitarBtn.getStyle()
            .set("margin-top", "15px")
            .set("background", "#ffc107")
            .set("color", "#000");

        alertSection.add(alertTitle, livrosList, solicitarBtn);
        return alertSection;
    }
    
    private void solicitarReposicaoRapida(List<Livro> livrosEstoqueBaixo) {
        try {
            // Busca fornecedores com email
            List<com.biblioteca.model.Fornecedor> fornecedores = com.biblioteca.service.FornecedorService.listarFornecedores()
                .stream()
                .filter(f -> f.getEmail() != null && !f.getEmail().trim().isEmpty())
                .toList();
            
            if (fornecedores.isEmpty()) {
                com.vaadin.flow.component.notification.Notification.show(
                    "❌ Nenhum fornecedor com email cadastrado encontrado", 
                    4000, com.vaadin.flow.component.notification.Notification.Position.TOP_CENTER
                );
                return;
            }
            
            // Envia email para todos os fornecedores
            int emailsEnviados = 0;
            for (com.biblioteca.model.Fornecedor fornecedor : fornecedores) {
                try {
                    com.biblioteca.service.EmailService.enviarEmailDeReposicaoDeEstoque(livrosEstoqueBaixo, fornecedor);
                    emailsEnviados++;
                } catch (Exception emailError) {
                    System.out.println("Erro ao enviar email para " + fornecedor.getEmail() + ": " + emailError.getMessage());
                }
            }
            
            com.vaadin.flow.component.notification.Notification.show(
                "✅ Solicitação enviada para " + emailsEnviados + " fornecedor(es)! " +
                "Total de " + livrosEstoqueBaixo.size() + " livros solicitados.", 
                5000, com.vaadin.flow.component.notification.Notification.Position.TOP_CENTER
            );
            
        } catch (Exception e) {
            com.vaadin.flow.component.notification.Notification.show(
                "❌ Erro ao enviar solicitação: " + e.getMessage(), 
                4000, com.vaadin.flow.component.notification.Notification.Position.TOP_CENTER
            );
        }
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }
} 