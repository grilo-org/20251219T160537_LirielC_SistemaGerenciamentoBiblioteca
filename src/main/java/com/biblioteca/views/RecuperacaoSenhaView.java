package com.biblioteca.views;

import com.biblioteca.service.EmailService;
import com.biblioteca.service.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Autowired;

@Route("recuperar-senha")
@PageTitle("Recuperar Senha | Sistema Biblioteca")
@AnonymousAllowed
public class RecuperacaoSenhaView extends VerticalLayout {

    private final EmailService emailService;
    private final UsuarioService usuarioService;

    @Autowired
    public RecuperacaoSenhaView(EmailService emailService, UsuarioService usuarioService) {
        this.emailService = emailService;
        this.usuarioService = usuarioService;
        
        configurarLayout();
    }

    private void configurarLayout() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setPadding(true);

        // Título
        H2 titulo = new H2("🔐 Recuperar Senha");
        titulo.getStyle().set("color", "var(--lumo-primary-color)");

        // Descrição
        Paragraph descricao = new Paragraph(
            "Digite seu email cadastrado no sistema. " +
            "Enviaremos um link para você redefinir sua senha."
        );
        descricao.getStyle().set("text-align", "center");
        descricao.getStyle().set("max-width", "400px");

        // Campo de email
        EmailField emailField = new EmailField("Email");
        emailField.setPlaceholder("seu@email.com");
        emailField.setRequired(true);
        emailField.setWidth("300px");

        // Botão de enviar
        Button enviarBtn = new Button("Enviar Link de Recuperação");
        enviarBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        enviarBtn.setWidth("300px");

        // Botão de voltar
        Button voltarBtn = new Button("Voltar ao Login");
        voltarBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY);
        voltarBtn.setWidth("300px");

        // Ação do botão enviar
        enviarBtn.addClickListener(e -> {
            String email = emailField.getValue();
            
            if (email == null || email.trim().isEmpty()) {
                Notification.show("Por favor, digite um email válido", 3000, Notification.Position.TOP_CENTER);
                return;
            }

            // Verifica se o email existe no sistema
            if (!usuarioService.existeEmail(email)) {
                // Por segurança, não informamos se o email existe ou não
                Notification.show("Se o email estiver cadastrado, você receberá um link de recuperação", 5000, Notification.Position.TOP_CENTER);
                return;
            }

            // Gera token de recuperação
            String token = com.biblioteca.service.AuthenticationService.generateAuthenticationToken(email);
            
            // Persistir o token usando Spring Data (garante visibilidade pelo Repository)
            usuarioService.atualizarTokenRecuperacaoWeb(email, token);

            // Envia email (mockado)
            boolean enviado = emailService.enviarEmailRecuperacaoSenhaWeb(email, token);
            
            if (enviado) {
                Notification.show("✅ Link de recuperação enviado! Copie o link: http://localhost:8080/alterar-senha/" + token, 10000, Notification.Position.TOP_CENTER);
                emailField.clear();
            } else {
                Notification.show("❌ Erro ao enviar email. Tente novamente.", 3000, Notification.Position.TOP_CENTER);
            }
        });

        // Ação do botão voltar
        voltarBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });

        // Layout
        VerticalLayout card = new VerticalLayout(titulo, descricao, emailField, enviarBtn, voltarBtn);
        card.setWidth("400px");
        card.setPadding(true);
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");
        card.setSpacing(true);
        card.setAlignItems(Alignment.CENTER);

        add(card);
    }
} 