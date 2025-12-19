package com.biblioteca.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("access-denied")
@PageTitle("Acesso Negado | Sistema Biblioteca")
@AnonymousAllowed
public class AccessDeniedView extends VerticalLayout {

    public AccessDeniedView(){
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        add(
            new H2("🚫 Acesso negado"),
            new Paragraph("Você não possui permissão para acessar esta página.")
        );
    }
} 