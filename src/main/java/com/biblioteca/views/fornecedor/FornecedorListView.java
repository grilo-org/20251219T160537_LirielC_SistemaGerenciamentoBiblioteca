package com.biblioteca.views.fornecedor;

import com.biblioteca.model.Fornecedor;
import com.biblioteca.service.FornecedorService;
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

@Route(value = "fornecedores", layout = com.biblioteca.views.MainLayout.class)
@PageTitle("Fornecedores | Sistema Biblioteca")
@RolesAllowed({"ADMIN", "GERENTE"})
public class FornecedorListView extends VerticalLayout {

    private final FornecedorService fornecedorService;
    private final Grid<Fornecedor> grid = new Grid<>(Fornecedor.class,false);

    private final TextField filtroNome = new TextField();
    private final Button novoBtn = new Button("Novo Fornecedor");

    @Autowired
    public FornecedorListView(FornecedorService fornecedorService){
        this.fornecedorService = fornecedorService;
        configurarComponentes();
        atualizarGrid();
    }

    private void configurarComponentes(){
        filtroNome.setPlaceholder("Buscar por nome...");
        filtroNome.setClearButtonVisible(true);
        filtroNome.setValueChangeMode(ValueChangeMode.LAZY);
        filtroNome.addValueChangeListener(e -> atualizarGrid());

        novoBtn.addClickListener(e -> abrirFormulario(new Fornecedor()));
        
        Button solicitarBtn = new Button("📧 Solicitar Reposição", e -> solicitarReposicaoManual());
        solicitarBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout topo = new HorizontalLayout(filtroNome, novoBtn, solicitarBtn);
        topo.setWidthFull();
        topo.expand(filtroNome);

        grid.addColumn(Fornecedor::getNome).setHeader("Nome").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Fornecedor::getEmail).setHeader("E-mail");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("600px");
        grid.setPageSize(20);

        grid.setItems(query -> {
                    if(filtroNome.getValue()==null || filtroNome.getValue().isBlank()){
                        return fornecedorService.listarPaginado(query.getOffset(), query.getLimit()).stream();
                    } else {
                        return fornecedorService.buscarPorNome(filtroNome.getValue()).stream()
                                .skip(query.getOffset()).limit(query.getLimit());
                    }
                },
                q -> {
                    if(filtroNome.getValue()==null || filtroNome.getValue().isBlank()) return (int) fornecedorService.contarFornecedores();
                    return fornecedorService.buscarPorNome(filtroNome.getValue()).size();
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

    private void abrirFormulario(Fornecedor fornecedor){
        FornecedorForm form = new FornecedorForm();
        form.setFornecedor(fornecedor);

        Dialog dialog = new Dialog(form);
        dialog.setModal(true);
        dialog.setDraggable(true);

        form.setSaveListener(f -> {
            if(f.getId()==null){
                fornecedorService.salvarFornecedorWeb(f.getNome(), f.getEmail());
            }else{
                fornecedorService.atualizarFornecedorWeb(f.getId(), f.getNome(), f.getEmail());
            }
            Notification.show("Fornecedor salvo",3000, Notification.Position.TOP_CENTER);
            atualizarGrid();
            dialog.close();
        });

        form.setDeleteListener(f -> {
            if(f.getId()!=null){
                fornecedorService.removerFornecedorWeb(f.getId());
                Notification.show("Fornecedor excluído",3000, Notification.Position.TOP_CENTER);
                atualizarGrid();
            }
            dialog.close();
        });

        form.setCancelListener(dialog::close);

        dialog.open();
    }
    
    private void solicitarReposicaoManual() {
        try {
            // Busca livros com estoque baixo (< 5)
            java.util.List<com.biblioteca.model.Livro> livrosEstoqueBaixo = 
                com.biblioteca.service.LivroService.listarLivros().stream()
                    .filter(l -> l.getQuantidadeEstoque() != null && l.getQuantidadeEstoque() < 5)
                    .collect(java.util.stream.Collectors.toList());
            
            if (livrosEstoqueBaixo.isEmpty()) {
                Notification.show("✅ Nenhum livro com estoque baixo encontrado!", 
                    3000, Notification.Position.TOP_CENTER);
                return;
            }
            
            // Busca fornecedores com email
            java.util.List<com.biblioteca.model.Fornecedor> fornecedoresComEmail = 
                fornecedorService.listarPaginado(0, 1000).stream()
                    .filter(f -> f.getEmail() != null && !f.getEmail().trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            
            if (fornecedoresComEmail.isEmpty()) {
                Notification.show("❌ Nenhum fornecedor com email cadastrado!", 
                    4000, Notification.Position.TOP_CENTER);
                return;
            }
            
            // Envia emails
            int emailsEnviados = 0;
            for (com.biblioteca.model.Fornecedor fornecedor : fornecedoresComEmail) {
                try {
                    com.biblioteca.service.EmailService.enviarEmailDeReposicaoDeEstoque(livrosEstoqueBaixo, fornecedor);
                    emailsEnviados++;
                } catch (Exception emailError) {
                    System.out.println("Erro ao enviar email para " + fornecedor.getEmail() + ": " + emailError.getMessage());
                }
            }
            
            Notification.show(
                "✅ Solicitação de reposição enviada!\n" +
                "📧 Fornecedores notificados: " + emailsEnviados + "\n" +
                "📚 Livros solicitados: " + livrosEstoqueBaixo.size(), 
                5000, Notification.Position.TOP_CENTER
            );
            
        } catch (Exception e) {
            Notification.show("❌ Erro ao solicitar reposição: " + e.getMessage(), 
                4000, Notification.Position.TOP_CENTER);
        }
    }
} 