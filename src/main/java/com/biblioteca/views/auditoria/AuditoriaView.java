package com.biblioteca.views.auditoria;

import com.biblioteca.model.Auditoria;
import com.biblioteca.repository.AuditoriaRepository;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "auditoria", layout = com.biblioteca.views.MainLayout.class)
@PageTitle("Auditoria | Sistema Biblioteca")
@RolesAllowed({"ADMIN"})
public class AuditoriaView extends VerticalLayout {

    private final AuditoriaRepository auditoriaRepository;
    private final Grid<Auditoria> grid = new Grid<>(Auditoria.class,false);
    private final TextField filtro = new TextField();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    public AuditoriaView(AuditoriaRepository repo){
        this.auditoriaRepository = repo;
        configure();
    }

    private void configure(){
        filtro.setPlaceholder("Filtrar por entidade ou usuário...");
        filtro.setClearButtonVisible(true);
        filtro.setValueChangeMode(ValueChangeMode.LAZY);
        filtro.addValueChangeListener(e -> refresh());

        grid.addColumn(Auditoria::getNomeEntidade).setHeader("Entidade");
        grid.addColumn(Auditoria::getIdEntidade).setHeader("ID");
        grid.addColumn(Auditoria::getAcao).setHeader("Ação");
        grid.addColumn(a -> a.getUsuario()!=null? a.getUsuario().getLogin():"-").setHeader("Usuário");
        grid.addColumn(a -> a.getData()!=null? a.getData().format(fmt):"").setHeader("Data");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("600px");

        add(filtro, grid);
        setSizeFull();
        refresh();
    }

    private void refresh(){
        List<Auditoria> all = auditoriaRepository.findAll();
        String f = filtro.getValue();
        if(f!=null && !f.isBlank()){
            String fv = f.toLowerCase();
            all = all.stream()
                    .filter(a -> a.getNomeEntidade()!=null && a.getNomeEntidade().toLowerCase().contains(fv)
                        || (a.getUsuario()!=null && a.getUsuario().getLogin()!=null && a.getUsuario().getLogin().toLowerCase().contains(fv)))
                    .toList();
        }
        grid.setItems(all);
    }
} 