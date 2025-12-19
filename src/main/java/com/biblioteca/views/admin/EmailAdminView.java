package com.biblioteca.views.admin;

import com.biblioteca.service.EmailService;import com.vaadin.flow.component.button.Button;import com.vaadin.flow.component.button.ButtonVariant;import com.vaadin.flow.component.grid.Grid;import com.vaadin.flow.component.html.H2;import com.vaadin.flow.component.html.Span;import com.vaadin.flow.component.notification.Notification;import com.vaadin.flow.component.orderedlayout.HorizontalLayout;import com.vaadin.flow.component.orderedlayout.VerticalLayout;import com.vaadin.flow.component.textfield.TextField;import com.vaadin.flow.router.PageTitle;import com.vaadin.flow.router.Route;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.security.access.annotation.Secured;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * View administrativa para gerenciar e monitorar emails enviados
 */
@Route("admin/emails")@PageTitle("Administração de Emails - Sistema Biblioteca")@Secured({"ROLE_ADMIN", "ROLE_GERENTE"})
public class EmailAdminView extends VerticalLayout {
    
    @Autowired
    private EmailService emailService;
    
    private Grid<Map<String, Object>> emailGrid;
    private Span totalEmailsSpan;
    private Span emailsRecuperacaoSpan;
    private Span emailsReposicaoSpan;
    private Span emailsEmprestimoSpan;
    
    public EmailAdminView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        criarHeader();
        criarEstatisticas();
        criarSecaoTeste();
        criarGridEmails();
        criarBotoesAcao();
        
        atualizarDados();
    }
    
    private void criarHeader() {
        H2 titulo = new H2("📧 Administração de Emails");
        titulo.getStyle().set("margin-bottom", "0");
        
        Span descricao = new Span("Monitore e teste o serviço de email do sistema");
        descricao.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        add(titulo, descricao);
    }
    
    private void criarEstatisticas() {
        H2 tituloStats = new H2("📊 Estatísticas");
        
        totalEmailsSpan = new Span("Total de emails: 0");
        emailsRecuperacaoSpan = new Span("Recuperação de senha: 0");
        emailsReposicaoSpan = new Span("Reposição de estoque: 0");
        emailsEmprestimoSpan = new Span("Confirmação de empréstimo: 0");
        
        // Styling das estatísticas
        String statStyle = "padding: 10px; margin: 5px; background: var(--lumo-contrast-5pct); border-radius: 5px;";
        totalEmailsSpan.getStyle().set("cssText", statStyle + "font-weight: bold;");
        emailsRecuperacaoSpan.getStyle().set("cssText", statStyle);
        emailsReposicaoSpan.getStyle().set("cssText", statStyle);
        emailsEmprestimoSpan.getStyle().set("cssText", statStyle);
        
        HorizontalLayout statsLayout = new HorizontalLayout(
            totalEmailsSpan, emailsRecuperacaoSpan, 
            emailsReposicaoSpan, emailsEmprestimoSpan
        );
        statsLayout.setWidthFull();
        
        add(tituloStats, statsLayout);
    }
    
    private void criarSecaoTeste() {
        H2 tituloTeste = new H2("🧪 Teste de Email");
        
        TextField emailField = new TextField("Email");
        emailField.setPlaceholder("teste@exemplo.com");
        emailField.setWidth("300px");
        
        Button testeRecuperacaoBtn = new Button("Teste Recuperação", e -> {
            String email = emailField.getValue();
            if (!email.isEmpty()) {
                String token = "TEST" + System.currentTimeMillis();
                boolean sucesso = emailService.enviarEmailRecuperacaoSenhaWeb(email, token);
                if (sucesso) {
                    Notification.show("✅ Email de teste enviado para: " + email, 3000, Notification.Position.TOP_CENTER);
                    atualizarDados();
                } else {
                    Notification.show("❌ Erro ao enviar email", 3000, Notification.Position.TOP_CENTER);
                }
            } else {
                Notification.show("⚠️ Digite um email", 3000, Notification.Position.TOP_CENTER);
            }
        });
        testeRecuperacaoBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        HorizontalLayout testeLayout = new HorizontalLayout(emailField, testeRecuperacaoBtn);
        testeLayout.setAlignItems(Alignment.END);
        
        add(tituloTeste, testeLayout);
    }
    
    private void criarGridEmails() {
        H2 tituloGrid = new H2("📋 Histórico de Emails");
        
        emailGrid = new Grid<>();
        emailGrid.setHeight("400px");
        
        emailGrid.addColumn(data -> data.get("tipo")).setHeader("Tipo").setAutoWidth(true);
        emailGrid.addColumn(data -> data.get("destinatario")).setHeader("Destinatário").setAutoWidth(true);
        emailGrid.addColumn(data -> data.get("assunto")).setHeader("Assunto").setAutoWidth(true);
        emailGrid.addColumn(data -> {
            LocalDateTime dataEnvio = (LocalDateTime) data.get("dataEnvio");
            return dataEnvio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }).setHeader("Data/Hora").setAutoWidth(true);
        emailGrid.addColumn(data -> data.get("status")).setHeader("Status").setAutoWidth(true);
        
        add(tituloGrid, emailGrid);
    }
    
    private void criarBotoesAcao() {
        Button atualizarBtn = new Button("🔄 Atualizar", e -> atualizarDados());
        atualizarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button limparBtn = new Button("🗑️ Limpar Histórico", e -> {
            emailService.limparHistoricoEmails();
            Notification.show("🗑️ Histórico de emails limpo", 3000, Notification.Position.TOP_CENTER);
            atualizarDados();
        });
        limparBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        
        HorizontalLayout botoesLayout = new HorizontalLayout(atualizarBtn, limparBtn);
        add(botoesLayout);
    }
    
    private void atualizarDados() {
        if (emailService != null) {
            // Atualizar estatísticas
            var emails = emailService.listarEmailsEnviados();
            var contadores = emailService.contarEmailsPorTipo();
            
            totalEmailsSpan.setText("Total de emails: " + emails.size());
            emailsRecuperacaoSpan.setText("Recuperação: " + contadores.getOrDefault("RECUPERACAO_SENHA", 0L));
            emailsReposicaoSpan.setText("Reposição: " + contadores.getOrDefault("REPOSICAO_ESTOQUE", 0L));
            emailsEmprestimoSpan.setText("Empréstimo: " + contadores.getOrDefault("CONFIRMACAO_EMPRESTIMO", 0L));
            
            // Atualizar grid
            emailGrid.setItems(emails);
        }
    }
} 