package com.biblioteca.views.multas;

import com.biblioteca.model.Emprestimo;
import com.biblioteca.model.Usuario;
import com.biblioteca.service.EmprestimoService;
import com.biblioteca.service.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "multas", layout = com.biblioteca.views.MainLayout.class)
@PageTitle("Gestão de Multas | Sistema Biblioteca")
@RolesAllowed({"FUNCIONARIO", "GERENTE", "ADMIN", "USER"})
public class MultasView extends VerticalLayout {

    private final EmprestimoService emprestimoService;
    private final UsuarioService usuarioService;
    private final Grid<Usuario> grid = new Grid<>(Usuario.class, false);

    @Autowired
    public MultasView(EmprestimoService emprestimoService, UsuarioService usuarioService) {
        this.emprestimoService = emprestimoService;
        this.usuarioService = usuarioService;
        
        configurarLayout();
        configurarGrid();
        carregarDados();
    }

    private void configurarLayout() {
        H3 titulo = new H3("💰 Gestão de Multas");
        titulo.getStyle().set("color", "#dc3545").set("margin-bottom", "20px");
        
        Span descricao = new Span("Usuários com multas pendentes por atraso na devolução de livros");
        descricao.getStyle().set("color", "#6c757d").set("margin-bottom", "20px");
        
        add(titulo, descricao);
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    private void configurarGrid() {
        // Coluna do nome do usuário
        grid.addColumn(Usuario::getNome)
                .setHeader("Nome do Usuário")
                .setAutoWidth(true)
                .setFlexGrow(1);

        // Coluna do email
        grid.addColumn(usuario -> usuario.getEmail() != null ? usuario.getEmail() : "Não informado")
                .setHeader("E-mail")
                .setAutoWidth(true);

        // Coluna do telefone
        grid.addColumn(usuario -> usuario.getTelefone() != null ? usuario.getTelefone() : "Não informado")
                .setHeader("Telefone")
                .setAutoWidth(true);

        // Coluna do valor da multa
        grid.addColumn(new ComponentRenderer<>(usuario -> {
            double multaTotal = emprestimoService.calcularMultaTotalUsuario(usuario);
            Span valorSpan = new Span("R$ " + String.format("%.2f", multaTotal));
            if (multaTotal > 0) {
                valorSpan.getStyle().set("color", "#dc3545").set("font-weight", "bold");
            } else {
                valorSpan.getStyle().set("color", "#28a745");
            }
            return valorSpan;
        })).setHeader("Valor Total da Multa").setAutoWidth(true);

        // Coluna de ações
        grid.addColumn(new ComponentRenderer<>(usuario -> {
            Button detalhesBtn = new Button("Ver Detalhes", VaadinIcon.INFO_CIRCLE.create());
            detalhesBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            detalhesBtn.addClickListener(e -> mostrarDetalhesMulta(usuario));

            Button contatoBtn = new Button("Contatar", VaadinIcon.ENVELOPE.create());
            contatoBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
            contatoBtn.addClickListener(e -> contatarUsuario(usuario));
            contatoBtn.setEnabled(usuario.getEmail() != null && !usuario.getEmail().trim().isEmpty());

            HorizontalLayout acoes = new HorizontalLayout(detalhesBtn, contatoBtn);
            acoes.setSpacing(true);
            return acoes;
        })).setHeader("Ações").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("600px");
        grid.setPageSize(20);

        add(grid);
    }

    private void carregarDados() {
        List<Usuario> usuariosComMultas = emprestimoService.buscarUsuariosComMultas();
        
        // Filtrar apenas usuários que realmente têm multas > 0
        List<Usuario> usuariosComMultasAtivas = usuariosComMultas.stream()
                .filter(usuario -> emprestimoService.calcularMultaTotalUsuario(usuario) > 0)
                .toList();
                
        grid.setItems(usuariosComMultasAtivas);
        
        if (usuariosComMultasAtivas.isEmpty()) {
            Span mensagem = new Span("🎉 Nenhum usuário com multas pendentes no momento!");
            mensagem.getStyle()
                    .set("font-size", "1.2em")
                    .set("color", "#28a745")
                    .set("text-align", "center")
                    .set("padding", "20px");
            add(mensagem);
        }
    }

    private void mostrarDetalhesMulta(Usuario usuario) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("800px");
        dialog.setHeight("600px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        H4 titulo = new H4("Detalhes das Multas - " + usuario.getNome());
        titulo.getStyle().set("color", "#dc3545");

        List<Emprestimo> emprestimosComMulta = emprestimoService.buscarEmprestimosComMultaPorUsuario(usuario);
        
        Grid<Emprestimo> gridDetalhes = new Grid<>(Emprestimo.class, false);
        
        gridDetalhes.addColumn(emprestimo -> emprestimo.getLivro().getTitulo())
                .setHeader("Livro").setAutoWidth(true);
                
        gridDetalhes.addColumn(emprestimo -> 
                emprestimo.getDataEmprestimo().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setHeader("Data Empréstimo").setAutoWidth(true);
                
        gridDetalhes.addColumn(emprestimo -> 
                emprestimo.getDataPrevista().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setHeader("Data Prevista").setAutoWidth(true);
                
        gridDetalhes.addColumn(new ComponentRenderer<>(emprestimo -> {
            Span statusSpan;
            if (emprestimo.isDevolvido()) {
                statusSpan = new Span("Devolvido em " + 
                    emprestimo.getDataDevolucao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                statusSpan.getStyle().set("color", "#28a745");
            } else {
                statusSpan = new Span("EM ATRASO");
                statusSpan.getStyle().set("color", "#dc3545").set("font-weight", "bold");
            }
            return statusSpan;
        })).setHeader("Status").setAutoWidth(true);
        
        gridDetalhes.addColumn(new ComponentRenderer<>(emprestimo -> {
            double multa = emprestimo.isDevolvido() 
                ? (emprestimo.getMultaTotal() != null ? emprestimo.getMultaTotal() : 0.0)
                : emprestimo.calcularMulta();
            Span multaSpan = new Span("R$ " + String.format("%.2f", multa));
            multaSpan.getStyle().set("color", "#dc3545").set("font-weight", "bold");
            return multaSpan;
        })).setHeader("Multa").setAutoWidth(true);

        gridDetalhes.setItems(emprestimosComMulta);
        gridDetalhes.setHeight("300px");

        double multaTotal = emprestimoService.calcularMultaTotalUsuario(usuario);
        Span totalSpan = new Span("Total de Multas: R$ " + String.format("%.2f", multaTotal));
        totalSpan.getStyle()
                .set("font-size", "1.2em")
                .set("font-weight", "bold")
                .set("color", "#dc3545")
                .set("text-align", "center")
                .set("padding", "10px");

        Button fecharBtn = new Button("Fechar", e -> dialog.close());
        fecharBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        content.add(titulo, gridDetalhes, totalSpan, fecharBtn);
        dialog.add(content);
        dialog.open();
    }

    private void contatarUsuario(Usuario usuario) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setWidth("400px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        H4 titulo = new H4("Contato - " + usuario.getNome());
        
        if (usuario.getEmail() != null && !usuario.getEmail().trim().isEmpty()) {
            Span email = new Span("📧 E-mail: " + usuario.getEmail());
            content.add(email);
        }
        
        if (usuario.getTelefone() != null && !usuario.getTelefone().trim().isEmpty()) {
            Span telefone = new Span("📞 Telefone: " + usuario.getTelefone());
            content.add(telefone);
        }

        double multaTotal = emprestimoService.calcularMultaTotalUsuario(usuario);
        Span multa = new Span("💰 Multa Total: R$ " + String.format("%.2f", multaTotal));
        multa.getStyle().set("color", "#dc3545").set("font-weight", "bold");
        
        Span sugestao = new Span("💡 Sugestão: Entre em contato para resolver a situação das multas pendentes.");
        sugestao.getStyle().set("font-style", "italic").set("color", "#6c757d");

        Button fecharBtn = new Button("Fechar", e -> dialog.close());
        fecharBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        content.add(titulo, multa, sugestao, fecharBtn);
        dialog.add(content);
        dialog.open();
    }
} 