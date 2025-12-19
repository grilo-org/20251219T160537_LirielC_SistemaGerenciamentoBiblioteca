package com.biblioteca.views.livro;

import com.biblioteca.model.Livro;
import com.biblioteca.service.LivroService;
import com.biblioteca.service.CarrinhoService;
import com.biblioteca.service.UsuarioService;
import com.biblioteca.model.TipoCompra;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import com.vaadin.flow.component.html.Image;

@Route(value = "livros", layout = com.biblioteca.views.MainLayout.class)
@PageTitle("Livros | Sistema Biblioteca")
@RolesAllowed({"FUNCIONARIO", "GERENTE", "ADMIN", "CLIENTE", "USER"})
public class LivroListView extends VerticalLayout {

    private final LivroService livroService;
    private final CarrinhoService carrinhoService;
    private final UsuarioService usuarioService;
    private final Grid<Livro> grid = new Grid<>(Livro.class, false);

    private final TextField filtroTitulo = new TextField();
    private final TextField filtroAutor = new TextField();
    private final TextField filtroIsbn = new TextField();
    private final Button novoBtn = new Button("Novo Livro");

    @Autowired
    public LivroListView(LivroService livroService, CarrinhoService carrinhoService, UsuarioService usuarioService) {
        this.livroService = livroService;
        this.carrinhoService = carrinhoService;
        this.usuarioService = usuarioService;
        configurarComponentes();
        atualizarGrid();
    }

    private void configurarComponentes() {
        // Filtros
        filtroTitulo.setPlaceholder("Buscar por título...");
        filtroTitulo.setClearButtonVisible(true);
        filtroTitulo.setValueChangeMode(ValueChangeMode.LAZY);
        filtroTitulo.addValueChangeListener(e -> atualizarGrid());
        
        filtroAutor.setPlaceholder("Buscar por autor...");
        filtroAutor.setClearButtonVisible(true);
        filtroAutor.setValueChangeMode(ValueChangeMode.LAZY);
        filtroAutor.addValueChangeListener(e -> atualizarGrid());
        
        filtroIsbn.setPlaceholder("Buscar por ISBN...");
        filtroIsbn.setClearButtonVisible(true);
        filtroIsbn.setValueChangeMode(ValueChangeMode.LAZY);
        filtroIsbn.addValueChangeListener(e -> atualizarGrid());

        // Botão novo
        boolean podeEditar = podeEditar();
        novoBtn.setVisible(podeEditar);
        novoBtn.addClickListener(e -> abrirFormulario(new Livro()));

        HorizontalLayout filtros = new HorizontalLayout(filtroTitulo, filtroAutor, filtroIsbn);
        filtros.setWidthFull();
        filtros.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
        
        HorizontalLayout topo = new HorizontalLayout(filtros, novoBtn);
        topo.setWidthFull();
        topo.expand(filtros);

        // Grid
        grid.addComponentColumn(l -> {
            String urlImagem = l.getUrlImagem();
            // Se não tiver URL ou URL for vazia, usar placeholder
            if (urlImagem == null || urlImagem.trim().isEmpty()) {
                urlImagem = "https://via.placeholder.com/60x90/cccccc/999999?text=Sem+Imagem";
            }
            Image img = new Image(urlImagem, "Capa do livro: " + l.getTitulo());
            img.setWidth("60px");
            img.setHeight("90px");
            // Adicionar tratamento de erro de carregamento
            img.getElement().setAttribute("onerror", 
                "this.src='https://via.placeholder.com/60x90/cccccc/999999?text=Erro+Imagem'");
            return img;
        }).setHeader("Capa").setAutoWidth(false).setWidth("80px");

        grid.addColumn(Livro::getTitulo).setHeader("Título").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Livro::getAutor).setHeader("Autor").setAutoWidth(true);
        grid.addColumn(Livro::getIsbn).setHeader("ISBN").setAutoWidth(true);
        grid.addColumn(Livro::getValor).setHeader("Preço");

        if(podeEditar()){
            grid.addColumn(l -> l.getQuantidadeEstoque() != null ? l.getQuantidadeEstoque() : 0)
                .setHeader("Estoque");
        }

        grid.addComponentColumn(l -> {
            Button add = new Button("Adicionar", e -> {
                var usuario = usuarioService.buscarUsuarioByLoginWeb(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()).orElse(null);
                if(usuario!=null){
                    var carrinho = carrinhoService.obterOuCriarCarrinho(usuario);
                    carrinhoService.adicionarLivro(carrinho, l, 1);
                    Notification.show("Adicionado ao carrinho",2000, Notification.Position.TOP_CENTER);
                }
            });
            add.setEnabled(l.getQuantidadeEstoque()!=null && l.getQuantidadeEstoque()>0);
            return add;
        }).setHeader("");

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("600px");
        grid.setPageSize(20);

        grid.setItems(query -> {
                    String titulo = filtroTitulo.getValue();
                    String autor = filtroAutor.getValue();
                    String isbn = filtroIsbn.getValue();
                    
                    // Se todos os filtros estão vazios, listar todos paginados
                    if ((titulo == null || titulo.isBlank()) && 
                        (autor == null || autor.isBlank()) && 
                        (isbn == null || isbn.isBlank())) {
                        return livroService.listarPaginado(query.getOffset(), query.getLimit()).stream();
                    } else {
                        // Usar busca por múltiplos critérios
                        return livroService.buscarPorMultiplosCriterios(titulo, autor, isbn).stream()
                                .skip(query.getOffset())
                                .limit(query.getLimit());
                    }
                },
                q -> {
                    String titulo = filtroTitulo.getValue();
                    String autor = filtroAutor.getValue();
                    String isbn = filtroIsbn.getValue();
                    
                    if ((titulo == null || titulo.isBlank()) && 
                        (autor == null || autor.isBlank()) && 
                        (isbn == null || isbn.isBlank())) {
                        return (int) livroService.contarLivros();
                    }
                    return livroService.buscarPorMultiplosCriterios(titulo, autor, isbn).size();
                });

        if(podeEditar){
            grid.asSingleSelect().addValueChangeListener(evt -> {
                if (evt.getValue() != null) abrirFormulario(evt.getValue());
            });
        }

        add(topo, grid);
        setSizeFull();
    }

    private void abrirFormulario(Livro livro) {
        LivroForm form = new LivroForm();
        form.setLivro(livro);

        Dialog dialog = new Dialog(form);
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.setResizable(false);

        form.setSaveListener(l -> {
            if (l.getId() == null) {
                // Para novos livros, vamos usar o salvarLivro completo
                Livro novoLivro = livroService.salvarLivro(l.getTitulo(), l.getValor(), l.getQuantidadeEstoque());
                if (l.getUrlImagem() != null && !l.getUrlImagem().trim().isEmpty()) {
                    novoLivro.setUrlImagem(l.getUrlImagem());
                    livroService.salvarLivro(novoLivro);
                }
            } else {
                // Para livros existentes, usar o método que salva a URL da imagem
                livroService.atualizarLivroCompletoWeb(l);
            }
            dialog.close();
            Notification.show("Livro salvo", 3000, Notification.Position.TOP_CENTER);
            atualizarGrid();
        });

        form.setDeleteListener(l -> {
            if (l.getId() != null) {
                livroService.removerLivroWeb(l.getId());
                Notification.show("Livro excluído", 3000, Notification.Position.TOP_CENTER);
                atualizarGrid();
                dialog.close();
            }
        });

        form.setCancelListener(dialog::close);

        dialog.open();
    }

    private void atualizarGrid() {
        grid.getLazyDataView().refreshAll();
        // DataProvider usa os filtros automaticamente
    }

    private boolean podeEditar(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth==null) return false;
        for(GrantedAuthority ga : auth.getAuthorities()){
            String r = ga.getAuthority();
            if("ROLE_ADMIN".equals(r) || "ROLE_GERENTE".equals(r)) return true;
        }
        return false;
    }
} 