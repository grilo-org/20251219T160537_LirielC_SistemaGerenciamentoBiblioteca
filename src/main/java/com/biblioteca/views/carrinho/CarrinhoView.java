package com.biblioteca.views.carrinho;

import com.biblioteca.model.Carrinho;
import com.biblioteca.model.Livro;
import com.biblioteca.model.LivroCarrinho;
import com.biblioteca.model.Usuario;
import com.biblioteca.service.CarrinhoService;
import com.biblioteca.service.LivroService;
import com.biblioteca.service.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.button.ButtonVariant;

import java.util.List;

@Route(value = "carrinho", layout = com.biblioteca.views.MainLayout.class)
@PageTitle("Carrinho | Sistema Biblioteca")
@RolesAllowed({"CLIENTE","ADMIN","GERENTE","FUNCIONARIO","USER"})
public class CarrinhoView extends VerticalLayout {

    private final CarrinhoService carrinhoService;
    private final LivroService livroService;
    private final UsuarioService usuarioService;

    private Carrinho carrinho;
    private Usuario clienteSelecionado; // Cliente para quem está fazendo o pedido

    private final Grid<LivroCarrinho> gridCarrinho = new Grid<>(LivroCarrinho.class,false);
    private final Button finalizarBtn = new Button("Finalizar Compra");
    
    // Componentes para seleção de cliente (funcionários)
    private RadioButtonGroup<String> modoOperacao;
    private TextField cpfClienteField;
    private Button buscarClienteBtn;
    private Span infoClienteSpan;

    @Autowired
    public CarrinhoView(CarrinhoService carrinhoService,
                         LivroService livroService,
                         UsuarioService usuarioService){
        this.carrinhoService = carrinhoService;
        this.livroService = livroService;
        this.usuarioService = usuarioService;
        init();
    }

    private void init(){
        Usuario usuarioLogado = getUsuarioLogado();
        if(usuarioLogado==null){
            Notification.show("Usuário não encontrado",3000, Notification.Position.TOP_CENTER);
            return;
        }
        
        // Verifica se é funcionário (não cliente)
        boolean isFuncionario = isUsuarioFuncionario(usuarioLogado);
        
        if (isFuncionario) {
            configurarModoFuncionario();
        } else {
            // Cliente normal - usa o próprio carrinho
            clienteSelecionado = usuarioLogado;
            carrinho = carrinhoService.obterOuCriarCarrinho(usuarioLogado);
        }

        configurarGridCarrinho();
        gridCarrinho.setSelectionMode(Grid.SelectionMode.MULTI);
        configurarBotoes();

        // Layout principal
        H3 titulo = new H3(isFuncionario ? "🛒 Carrinho - Modo Funcionário" : "🛒 Seu Carrinho");
        add(titulo);
        
        if (isFuncionario) {
            add(criarSecaoSelecionarCliente());
        }
        
        add(gridCarrinho, finalizarBtn);
        setSizeFull();
    }

    private boolean isUsuarioFuncionario(Usuario usuario) {
        String role = usuario.getRole();
        return "ADMIN".equals(role) || "GERENTE".equals(role) || 
               "FUNCIONARIO".equals(role) || "USER".equals(role);
    }

    private void configurarModoFuncionario() {
        modoOperacao = new RadioButtonGroup<>();
        modoOperacao.setLabel("Modo de Operação");
        modoOperacao.setItems("proprio", "cliente");
        modoOperacao.setItemLabelGenerator(item -> 
            "proprio".equals(item) ? "🔸 Pedido Próprio" : "👤 Pedido para Cliente");
        modoOperacao.setValue("proprio");
        
        modoOperacao.addValueChangeListener(e -> {
            if ("proprio".equals(e.getValue())) {
                clienteSelecionado = getUsuarioLogado();
                carrinho = carrinhoService.obterOuCriarCarrinho(clienteSelecionado);
                limparSelecaoCliente();
            } else {
                clienteSelecionado = null;
                carrinho = null;
                limparSelecaoCliente();
            }
            refreshGrid();
            atualizarEstadoBotoes();
        });
        
        // Inicializa no modo próprio
        clienteSelecionado = getUsuarioLogado();
        carrinho = carrinhoService.obterOuCriarCarrinho(clienteSelecionado);
    }

    private VerticalLayout criarSecaoSelecionarCliente() {
        VerticalLayout secao = new VerticalLayout();
        secao.setSpacing(true);
        secao.setPadding(false);
        
        // Modo de operação
        secao.add(modoOperacao);
        
        // Busca de cliente
        HorizontalLayout buscaLayout = new HorizontalLayout();
        cpfClienteField = new TextField("CPF do Cliente");
        cpfClienteField.setPlaceholder("000.000.000-00");
        cpfClienteField.setWidth("200px");
        
        buscarClienteBtn = new Button("Buscar", VaadinIcon.SEARCH.create());
        buscarClienteBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buscarClienteBtn.addClickListener(e -> buscarCliente());
        
        buscaLayout.add(cpfClienteField, buscarClienteBtn);
        buscaLayout.setAlignItems(Alignment.END);
        
        // Info do cliente selecionado
        infoClienteSpan = new Span();
        infoClienteSpan.getStyle().set("font-weight", "bold").set("color", "#28a745");
        
        secao.add(buscaLayout, infoClienteSpan);
        
        // Inicialmente esconde a busca (modo próprio)
        cpfClienteField.setVisible(false);
        buscarClienteBtn.setVisible(false);
        
        // Mostra/esconde baseado no modo
        modoOperacao.addValueChangeListener(e -> {
            boolean isCliente = "cliente".equals(e.getValue());
            cpfClienteField.setVisible(isCliente);
            buscarClienteBtn.setVisible(isCliente);
            if (!isCliente) {
                infoClienteSpan.setText("");
            }
        });
        
        return secao;
    }

    private void buscarCliente() {
        String cpf = cpfClienteField.getValue();
        if (cpf == null || cpf.trim().isEmpty()) {
            Notification.show("Digite um CPF para buscar", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        // Remove formatação
        cpf = cpf.replaceAll("[^0-9]", "");
        
        if (cpf.length() != 11) {
            Notification.show("CPF deve ter 11 dígitos", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        try {
            Usuario cliente = usuarioService.buscarUsuarioPorCpf(cpf).orElse(null);
            if (cliente != null) {
                clienteSelecionado = cliente;
                carrinho = carrinhoService.obterOuCriarCarrinho(cliente);
                infoClienteSpan.setText("✅ Cliente: " + cliente.getNome() + " (CPF: " + formatarCpf(cpf) + ")");
                refreshGrid();
                atualizarEstadoBotoes();
                Notification.show("Cliente encontrado: " + cliente.getNome(), 3000, Notification.Position.MIDDLE);
            } else {
                Notification.show("Cliente não encontrado com esse CPF", 3000, Notification.Position.MIDDLE);
                clienteSelecionado = null;
                carrinho = null;
                refreshGrid();
                atualizarEstadoBotoes();
            }
        } catch (Exception e) {
            Notification.show("Erro ao buscar cliente: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private String formatarCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0,3) + "." + cpf.substring(3,6) + "." + 
                   cpf.substring(6,9) + "-" + cpf.substring(9,11);
        }
        return cpf;
    }

    private void limparSelecaoCliente() {
        if (cpfClienteField != null) {
            cpfClienteField.clear();
        }
        if (infoClienteSpan != null) {
            infoClienteSpan.setText("");
        }
    }

    private void atualizarEstadoBotoes() {
        boolean temCarrinho = carrinho != null && clienteSelecionado != null;
        finalizarBtn.setEnabled(temCarrinho);
        
        if (!temCarrinho && modoOperacao != null && "cliente".equals(modoOperacao.getValue())) {
            finalizarBtn.setText("Selecione um Cliente");
        } else {
            finalizarBtn.setText("Finalizar Compra");
        }
    }

    private void configurarGridCarrinho(){
        gridCarrinho.removeAllColumns();
        gridCarrinho.addColumn(lc -> lc.getLivro().getTitulo()).setHeader("Título");
        gridCarrinho.addComponentColumn(lc -> {
            com.vaadin.flow.component.orderedlayout.HorizontalLayout layout = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
            Span quantidade = new Span(String.valueOf(lc.getQuantidade()));
            quantidade.getStyle().set("min-width","24px").set("text-align","center");
            Button menos = new Button("-");
            Button mais = new Button("+");
            Button excluir = new Button("Excluir");

            menos.addClickListener(ev -> {
                if (carrinho != null && clienteSelecionado != null) {
                    carrinhoService.removerLivro(carrinho, lc.getLivro(), 1);
                    carrinho = carrinhoService.obterOuCriarCarrinho(clienteSelecionado);
                    refreshGrid();
                }
            });

            mais.addClickListener(ev -> {
                if (carrinho != null && clienteSelecionado != null) {
                    carrinhoService.adicionarLivro(carrinho, lc.getLivro(), 1);
                    carrinho = carrinhoService.obterOuCriarCarrinho(clienteSelecionado);
                    refreshGrid();
                }
            });

            excluir.addClickListener(ev -> {
                if (carrinho != null && clienteSelecionado != null) {
                    carrinhoService.removerLivro(carrinho, lc.getLivro(), lc.getQuantidade());
                    carrinho = carrinhoService.obterOuCriarCarrinho(clienteSelecionado);
                    refreshGrid();
                }
            });

            layout.add(quantidade, menos, mais, excluir);
            layout.setSpacing(true);
            layout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
            return layout;
        }).setHeader("Quantidade");

        gridCarrinho.addColumn(lc -> String.format("%.2f", lc.calcularValor())).setHeader("Total (R$)");
        gridCarrinho.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        gridCarrinho.setHeight("300px");

        refreshGrid();
    }

    private void configurarBotoes(){
        finalizarBtn.addClickListener(e -> {
            if (carrinho == null || clienteSelecionado == null) {
                Notification.show("Selecione um cliente primeiro", 3000, Notification.Position.TOP_CENTER);
                return;
            }
            
            java.util.Set<LivroCarrinho> selecionados = gridCarrinho.getSelectedItems();
            if(selecionados.isEmpty()){
                Notification.show("Selecione pelo menos um item",3000, Notification.Position.TOP_CENTER);
                return;
            }

            // Cria carrinho temporário com itens selecionados
            Carrinho carrinhoTemp = new Carrinho();
            carrinhoTemp.setCliente(clienteSelecionado);
            for(LivroCarrinho lc: selecionados){
                carrinhoTemp.getLivros().add(lc);
            }

            CheckoutDialog dialog = new CheckoutDialog(carrinhoTemp, carrinhoService, () -> {
                // Recarrega o carrinho completo (pode ter sido limpo pelo pagamento)
                try {
                    carrinho = carrinhoService.obterOuCriarCarrinho(clienteSelecionado);
                    refreshGrid();
                    Notification.show("✅ Pedido finalizado com sucesso!", 3000, Notification.Position.TOP_CENTER);
                } catch (Exception ex) {
                    Notification.show("Erro ao recarregar carrinho: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER);
                }
            });
            dialog.open();
        });
        
        atualizarEstadoBotoes();
    }

    private void refreshGrid(){
        if (carrinho != null) {
            gridCarrinho.setItems(carrinho.getLivros());
        } else {
            gridCarrinho.setItems();
        }
    }

    private Usuario getUsuarioLogado(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth==null) return null;
        String login = auth.getName();
        return usuarioService.buscarUsuarioByLoginWeb(login).orElse(null);
    }
} 