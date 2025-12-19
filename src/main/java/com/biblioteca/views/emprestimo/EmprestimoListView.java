package com.biblioteca.views.emprestimo;

import com.biblioteca.model.Emprestimo;
import com.biblioteca.model.Livro;
import com.biblioteca.model.Usuario;
import com.biblioteca.service.EmprestimoService;
import com.biblioteca.service.LivroService;
import com.biblioteca.service.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "emprestimos", layout = com.biblioteca.views.MainLayout.class)
@PageTitle("Empréstimos | Sistema Biblioteca")
@RolesAllowed({"FUNCIONARIO","ADMIN","GERENTE","USER"})
public class EmprestimoListView extends VerticalLayout {

    private final EmprestimoService emprestimoService;
    private final UsuarioService usuarioService;
    private final LivroService livroService;

    private final Grid<Emprestimo> grid = new Grid<>(Emprestimo.class,false);

    private final TextField filtroUsuario = new TextField();
    private final Button novoBtn = new Button("Novo Empréstimo");

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    public EmprestimoListView(EmprestimoService emprestimoService,
                               UsuarioService usuarioService,
                               LivroService livroService){
        this.emprestimoService = emprestimoService;
        this.usuarioService = usuarioService;
        this.livroService = livroService;
        configurarComponentes();
        atualizarGrid();
    }

    private void configurarComponentes(){
        filtroUsuario.setPlaceholder("Buscar por usuário...");
        filtroUsuario.setClearButtonVisible(true);
        filtroUsuario.setValueChangeMode(ValueChangeMode.LAZY);
        filtroUsuario.addValueChangeListener(e -> atualizarGrid());

        novoBtn.addClickListener(e -> abrirFormularioNovoEmprestimo());

        HorizontalLayout topo = new HorizontalLayout(filtroUsuario, novoBtn);
        topo.setWidthFull();
        topo.expand(filtroUsuario);

        grid.addColumn(e -> e.getUsuario()!=null ? e.getUsuario().getNome() : "").setHeader("Usuário").setAutoWidth(true);
        grid.addColumn(e -> e.getLivro()!=null ? e.getLivro().getTitulo() : "").setHeader("Livro").setAutoWidth(true);
        grid.addColumn(e -> e.getDataEmprestimo()!=null ? e.getDataEmprestimo().format(fmt) : "").setHeader("Data Empréstimo");
        grid.addColumn(e -> e.getDataPrevista()!=null ? e.getDataPrevista().format(fmt) : "").setHeader("Prev. Devolução");
        grid.addColumn(e -> e.isDevolvido() ? "Sim" : "Não").setHeader("Devolvido");
        grid.addColumn(Emprestimo::getStatus).setHeader("Status");
        grid.addColumn(emp -> {
            double m = emp.getMultaTotal()!=null?emp.getMultaTotal():0.0;
            return String.format("%.2f", m);
        }).setHeader("Multa (R$)").setClassNameGenerator(emp -> (emp.getMultaTotal()!=null && emp.getMultaTotal()>0)?"text-error":"");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("600px");
        grid.setPageSize(20);

        // Lazy loading
        grid.setItems(query -> {
                    List<Emprestimo> base;
                    if(filtroUsuario.getValue()==null || filtroUsuario.getValue().isBlank()){
                        base = emprestimoService.listarPaginado(query.getOffset(), query.getLimit());
                    } else {
                        base = emprestimoService.listarTodosEmprestimosWeb().stream()
                                .filter(emp -> emp.getUsuario()!=null && emp.getUsuario().getNome()!=null && emp.getUsuario().getNome().toLowerCase().contains(filtroUsuario.getValue().toLowerCase()))
                                .toList();
                        int from = query.getOffset();
                        int to = Math.min(from+query.getLimit(), base.size());
                        base = base.subList(from, to);
                    }
                    return base.stream();
                },
                q -> {
                    if(filtroUsuario.getValue()==null || filtroUsuario.getValue().isBlank()) return (int) emprestimoService.contarEmprestimos();
                    return (int) emprestimoService.listarTodosEmprestimosWeb().stream()
                            .filter(emp -> emp.getUsuario()!=null && emp.getUsuario().getNome()!=null && emp.getUsuario().getNome().toLowerCase().contains(filtroUsuario.getValue().toLowerCase()))
                            .count();
                });

        // ação devolver
        grid.addComponentColumn(emp -> {
            if(!emp.isDevolvido()){
                Button devolver = new Button("Devolver");
                devolver.addClickListener(ev -> {
                    try{
                        double multa = emprestimoService.registrarDevolucaoWeb(emp.getId());
                        Notification.show("Devolvido! Multa: R$ "+String.format("%.2f", multa), 4000, Notification.Position.TOP_CENTER);
                        atualizarGrid();
                    } catch (Exception ex){
                        Notification.show("Erro: "+ex.getMessage(), 4000, Notification.Position.TOP_CENTER);
                    }
                });
                return devolver;
            }
            return new com.vaadin.flow.component.html.Span("-");
        }).setHeader("Ações").setAutoWidth(true);

        add(topo, grid);
        setSizeFull();
    }

    private void abrirFormularioNovoEmprestimo(){
        List<Livro> livros = livroService.listarTodos().stream()
                .filter(l -> l.getQuantidadeEstoque()!=null && l.getQuantidadeEstoque()>0)
                .toList();
        EmprestimoForm form = new EmprestimoForm(livros, usuarioService);

        Dialog dialog = new Dialog(form);
        dialog.setModal(true);
        dialog.setDraggable(true);

        form.setSaveListener((u,l) -> {
            try{
                emprestimoService.realizarEmprestimoWeb(u,l);
                Notification.show("Empréstimo registrado",3000, Notification.Position.TOP_CENTER);
                atualizarGrid();
                dialog.close();
            }catch(Exception ex){
                Notification.show("Erro: "+ex.getMessage(),4000, Notification.Position.TOP_CENTER);
            }
        });

        form.setCancelListener(dialog::close);

        dialog.open();
    }

    private void atualizarGrid(){
        grid.getLazyDataView().refreshAll();
    }
} 