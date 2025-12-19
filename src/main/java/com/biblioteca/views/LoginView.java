package com.biblioteca.views;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

/**
 * Tela de login com LoginForm do Vaadin.
 */
@Route("login")
@PageTitle("Login | Sistema Biblioteca")
@PermitAll
public class LoginView extends VerticalLayout {

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        LoginForm loginForm = new LoginForm();
        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false); // Esconde o botão padrão

        // Link "Esqueci minha senha" sempre visível
        Anchor forgotPassword = new Anchor("/recuperar-senha", "Esqueci minha senha");
        forgotPassword.getStyle().set("margin-top", "10px");
        forgotPassword.getStyle().set("color", "var(--lumo-primary-color)");
        forgotPassword.getStyle().set("text-decoration", "none");
        forgotPassword.getStyle().set("font-size", "var(--lumo-font-size-s)");

        add(loginForm, forgotPassword);
    }
} 