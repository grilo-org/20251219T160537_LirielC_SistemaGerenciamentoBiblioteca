package com.biblioteca.views;

import com.biblioteca.model.Usuario;
import com.biblioteca.service.AuthenticationService;
import com.biblioteca.service.EmailService;
import com.biblioteca.service.UsuarioService;
import com.biblioteca.service.AuditoriaService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Autowired;

@Route("alterar-senha")
@PageTitle("Alterar Senha | Sistema Biblioteca")
@AnonymousAllowed
public class AlterarSenhaView extends VerticalLayout implements HasUrlParameter<String> {

    private final EmailService emailService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;
    private String tokenParam;

    @Autowired
    public AlterarSenhaView(EmailService emailService, UsuarioService usuarioService, 
                           @Autowired(required = false) AuditoriaService auditoriaService) {
        this.emailService = emailService;
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
        
        configurarLayout();
    }

    private void configurarLayout() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setPadding(true);

        // Título
        H2 titulo = new H2("🔑 Alterar Senha");
        titulo.getStyle().set("color", "var(--lumo-primary-color)");

        // Descrição
        Paragraph descricao = new Paragraph(
            "Digite sua nova senha. Ela deve ter pelo menos 6 caracteres."
        );
        descricao.getStyle().set("text-align", "center");
        descricao.getStyle().set("max-width", "400px");

        // Campo de nova senha
        PasswordField novaSenhaField = new PasswordField("Nova Senha");
        novaSenhaField.setPlaceholder("Digite a nova senha");
        novaSenhaField.setRequired(true);
        novaSenhaField.setWidth("300px");

        // Campo de confirmação
        PasswordField confirmarSenhaField = new PasswordField("Confirmar Senha");
        confirmarSenhaField.setPlaceholder("Digite novamente a senha");
        confirmarSenhaField.setRequired(true);
        confirmarSenhaField.setWidth("300px");

        // Botão de alterar
        Button alterarBtn = new Button("Alterar Senha");
        alterarBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        alterarBtn.setWidth("300px");

        // Botão de voltar
        Button voltarBtn = new Button("Voltar ao Login");
        voltarBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY);
        voltarBtn.setWidth("300px");

        // Ação do botão alterar
        alterarBtn.addClickListener(e -> {
            String novaSenha = novaSenhaField.getValue();
            String confirmarSenha = confirmarSenhaField.getValue();
            
            // Validações
            if (novaSenha == null || novaSenha.trim().isEmpty()) {
                Notification.show("Por favor, digite uma nova senha", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            
            if (novaSenha.length() < 6) {
                Notification.show("A senha deve ter pelo menos 6 caracteres", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            
            if (!novaSenha.equals(confirmarSenha)) {
                Notification.show("As senhas não coincidem", 3000, Notification.Position.TOP_CENTER);
                return;
            }

            // Busca o token da URL
            String token = tokenParam;
            if (token == null || token.trim().isEmpty()) {
                Notification.show("Token inválido. Solicite um novo link de recuperação.", 5000, Notification.Position.TOP_CENTER);
                return;
            }

            // Busca usuário pelo token
            Usuario usuario = buscarUsuarioPorToken(token);
            if (usuario == null) {
                Notification.show("Token expirado ou inválido. Solicite um novo link de recuperação.", 5000, Notification.Position.TOP_CENTER);
                return;
            }

            // Atualiza a senha
            boolean sucesso = usuarioService.atualizarSenhaWeb(usuario.getLogin(), novaSenha);
            
            if (sucesso) {
                // Registra na auditoria
                if (auditoriaService != null) {
                    auditoriaService.registrarMudancaSenha(usuario, 
                        "Senha alterada via recuperação por token. Email: " + usuario.getEmail());
                }
                
                // Limpa o token de recuperação
                usuarioService.limparTokenRecuperacao(usuario.getEmail());
                
                // Envia email de confirmação
                emailService.enviarEmailConfirmacaoAlteracaoWeb(usuario.getEmail());
                
                Notification.show("✅ Senha alterada com sucesso! Você pode fazer login com a nova senha.", 5000, Notification.Position.TOP_CENTER);
                
                // Redireciona para login
                getUI().ifPresent(ui -> ui.navigate(LoginView.class));
            } else {
                Notification.show("❌ Erro ao alterar senha. Tente novamente.", 3000, Notification.Position.TOP_CENTER);
            }
        });

        // Ação do botão voltar
        voltarBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });

        // Layout
        VerticalLayout card = new VerticalLayout(titulo, descricao, novaSenhaField, confirmarSenhaField, alterarBtn, voltarBtn);
        card.setWidth("400px");
        card.setPadding(true);
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");
        card.setSpacing(true);
        card.setAlignItems(Alignment.CENTER);

        add(card);
    }

    private Usuario buscarUsuarioPorToken(String token) {
        // Busca usuário que tem este token de recuperação
        return usuarioService.buscarUsuarioPorTokenRecuperacao(token);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String token) {
        this.tokenParam = token;
    }
} 