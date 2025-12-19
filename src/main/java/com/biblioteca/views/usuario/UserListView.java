package com.biblioteca.views.usuario;

import com.biblioteca.model.Usuario;
import com.biblioteca.service.UsuarioService;
import com.biblioteca.util.CpfValidator;
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

@Route(value = "usuarios", layout = com.biblioteca.views.MainLayout.class)
@PageTitle("Usuários | Sistema Biblioteca")
@RolesAllowed({"ADMIN", "GERENTE"})
public class UserListView extends VerticalLayout {

    private final UsuarioService usuarioService;
    private final Grid<Usuario> grid = new Grid<>(Usuario.class,false);

    private final TextField filtroNome = new TextField();
    private final Button novoBtn = new Button("Novo Usuário");

    @Autowired
    public UserListView(UsuarioService usuarioService){
        this.usuarioService = usuarioService;
        configurarComponentes();
        atualizarGrid();
    }

    private void configurarComponentes(){
        filtroNome.setPlaceholder("Buscar por nome...");
        filtroNome.setClearButtonVisible(true);
        filtroNome.setValueChangeMode(ValueChangeMode.LAZY);
        filtroNome.addValueChangeListener(e -> atualizarGrid());

        novoBtn.addClickListener(e -> abrirFormulario(new Usuario()));

        HorizontalLayout topo = new HorizontalLayout(filtroNome, novoBtn);
        topo.setWidthFull();
        topo.expand(filtroNome);

        grid.addColumn(Usuario::getNome).setHeader("Nome").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Usuario::getLogin).setHeader("Login");
        grid.addColumn(Usuario::getEmail).setHeader("E-mail");
        grid.addColumn(u -> formatarCpf(u.getCpf())).setHeader("CPF");
        grid.addColumn(Usuario::getRole).setHeader("Perfil");
        grid.addColumn(u -> u.getStatus()!=null && u.getStatus() ? "Ativo" : "Inativo").setHeader("Status");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("600px");
        grid.setPageSize(20);

        grid.setItems(query -> {
                    if (filtroNome.getValue()==null || filtroNome.getValue().isBlank()){
                        return usuarioService.listarPaginado(query.getOffset(), query.getLimit()).stream();
                    } else {
                        return usuarioService.listarUsuariosWeb().stream()
                                .filter(u -> u.getNome()!=null && u.getNome().toLowerCase().contains(filtroNome.getValue().toLowerCase()))
                                .skip(query.getOffset()).limit(query.getLimit());
                    }
                },
                q -> {
                    if (filtroNome.getValue()==null || filtroNome.getValue().isBlank()) return (int) usuarioService.contarUsuarios();
                    return (int) usuarioService.listarUsuariosWeb().stream()
                            .filter(u -> u.getNome()!=null && u.getNome().toLowerCase().contains(filtroNome.getValue().toLowerCase()))
                            .count();
                });

        grid.asSingleSelect().addValueChangeListener(e -> {
            if(e.getValue()!=null) abrirFormulario(e.getValue());
        });

        add(topo, grid);
        setSizeFull();
    }

    private void atualizarGrid(){
        grid.getLazyDataView().refreshAll();
    }

    private void abrirFormulario(Usuario usuario){
        UserForm form = new UserForm();
        form.setUsuario(usuario);

        Dialog dialog = new Dialog(form);
        dialog.setModal(true);
        dialog.setDraggable(true);

        form.setSaveListener(u -> {
            try {
                if(u.getId()==null){
                    usuarioService.cadastrarUsuarioWeb(u);
                } else {
                    usuarioService.cadastrarUsuarioWeb(u); // save updates as well
                }
                Notification.show("Usuário salvo com sucesso!", 3000, Notification.Position.TOP_CENTER);
                atualizarGrid();
                dialog.close();
            } catch (IllegalArgumentException e) {
                Notification.show("Erro: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
            } catch (Exception e) {
                Notification.show("Erro inesperado ao salvar usuário. Tente novamente.", 4000, Notification.Position.TOP_CENTER);
            }
        });

        form.setDeleteListener(u -> {
            if(u.getId()!=null){
                usuarioService.removerUsuarioWeb(u.getId());
                Notification.show("Usuário excluído",3000, Notification.Position.TOP_CENTER);
                atualizarGrid();
            }
            dialog.close();
        });

        form.setCancelListener(dialog::close);

        dialog.open();
    }

    private String formatarCpf(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return "-";
        }
        return CpfValidator.format(cpf);
    }
} 