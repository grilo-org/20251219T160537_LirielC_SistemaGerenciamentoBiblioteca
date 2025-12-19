package com.biblioteca.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.Component;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import com.biblioteca.security.AuthenticationEventListener;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Layout principal da aplicação web.
 * Contém header, menu lateral e área de conteúdo.
 */
public class MainLayout extends AppLayout {
    
    @Autowired(required = false)
    private AuthenticationEventListener authEventListener;
    
    public MainLayout() {
        createHeader();
        createDrawer();
    }
    
    private void createHeader() {
        H1 logo = new H1("📚 Sistema Biblioteca");
        logo.addClassNames("text-l", "m-m");
        
        Button logoutButton = new Button("Sair", new Icon(VaadinIcon.SIGN_OUT));
        logoutButton.addClickListener(e -> {
            // Registrar logout na auditoria
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && authEventListener != null) {
                authEventListener.registrarLogout(auth.getName());
            }
            
            getUI().ifPresent(ui -> ui.navigate("login"));
        });
        
        HorizontalLayout header = new HorizontalLayout(
            new DrawerToggle(), 
            logo,
            logoutButton
        );
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");
        
        addToNavbar(header);
    }
    
    private void createDrawer() {
        addToDrawer(new VerticalLayout(
            createNavigation()
        ));
    }
    
    private Tabs createNavigation() {
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL);

        addIfAuthorized(tabs, VaadinIcon.DASHBOARD, "Dashboard", DashboardView.class);
        addIfAuthorized(tabs, VaadinIcon.BOOK, "Livros", com.biblioteca.views.livro.LivroListView.class);
        addIfAuthorized(tabs, VaadinIcon.USERS, "Usuários", com.biblioteca.views.usuario.UserListView.class);
        addIfAuthorized(tabs, VaadinIcon.HANDSHAKE, "Empréstimos", com.biblioteca.views.emprestimo.EmprestimoListView.class);
        addIfAuthorized(tabs, VaadinIcon.CART, "Carrinho", com.biblioteca.views.carrinho.CarrinhoView.class);
        addIfAuthorized(tabs, VaadinIcon.RECORDS, "Meus Pedidos", com.biblioteca.views.MeusPedidosView.class);
        addIfAuthorized(tabs, VaadinIcon.SEARCH, "Pesquisar Pedidos", com.biblioteca.views.PesquisaPedidosView.class);
        addIfAuthorized(tabs, VaadinIcon.TRUCK, "Fornecedores", com.biblioteca.views.fornecedor.FornecedorListView.class);
        addIfAuthorized(tabs, VaadinIcon.DOLLAR, "Multas", com.biblioteca.views.multas.MultasView.class);
        addIfAuthorized(tabs, VaadinIcon.CLIPBOARD_TEXT, "Auditoria", com.biblioteca.views.auditoria.AuditoriaView.class);
        tabs.add(createTab(VaadinIcon.USER, "Minha Conta", com.biblioteca.views.MyAccountView.class));

        return tabs;
    }
    
    private Tab createTab(VaadinIcon viewIcon, String viewName, Class<? extends Component> navigationTarget) {
        Icon icon = viewIcon.create();
        icon.getStyle().set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("margin-inline-start", "var(--lumo-space-xs)")
                .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink();
        link.add(icon, new Span(viewName));
        link.setRoute(navigationTarget);
        link.setTabIndex(-1);

        return new Tab(link);
    }

    private Tab createTabPlaceholder(VaadinIcon viewIcon, String viewName) {
        Icon icon = viewIcon.create();
        icon.getStyle().set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("margin-inline-start", "var(--lumo-space-xs)")
                .set("padding", "var(--lumo-space-xs)");

        Span text = new Span(viewName);
        text.getStyle().set("font-style", "italic");
        
        HorizontalLayout layout = new HorizontalLayout(icon, text);
        layout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Tab tab = new Tab(layout);
        tab.setEnabled(false); // Desabilita a tab, já que a view não existe ainda
        return tab;
    }

    private void addIfAuthorized(Tabs tabs, VaadinIcon icon, String label, Class<? extends Component> view){
        if(hasAccess(view)){
            tabs.add(createTab(icon, label, view));
        }
    }

    private boolean hasAccess(Class<?> view){
        RolesAllowed rolesAllowed = view.getAnnotation(RolesAllowed.class);
        if(rolesAllowed==null) return true; // sem restrição

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth==null) return false;

        for(String role : rolesAllowed.value()){
            String springRole = "ROLE_"+role;
            for(GrantedAuthority ga : auth.getAuthorities()){
                if(springRole.equals(ga.getAuthority())){
                    return true;
                }
            }
        }
        return false;
    }
} 