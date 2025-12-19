package com.biblioteca.views.fornecedor;

import com.biblioteca.model.Fornecedor;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;

/**
 * Formulário reutilizável para criar/editar fornecedores.
 */
public class FornecedorForm extends FormLayout {

    private final TextField nome = new TextField("Nome");
    private final EmailField email = new EmailField("E-mail");

    private final Button salvar = new Button("Salvar");
    private final Button excluir = new Button("Excluir");
    private final Button cancelar = new Button("Cancelar");

    private final Binder<Fornecedor> binder = new Binder<>(Fornecedor.class);

    private Fornecedor fornecedor;

    public interface SaveListener { void onSave(Fornecedor fornecedor); }
    public interface DeleteListener { void onDelete(Fornecedor fornecedor); }
    public interface CancelListener { void onCancel(); }

    private SaveListener saveListener;
    private DeleteListener deleteListener;
    private CancelListener cancelListener;

    public FornecedorForm(){
        configureForm();
    }

    private void configureForm(){
        salvar.addClickListener(e -> validateAndSave());
        salvar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        salvar.addClickShortcut(Key.ENTER);

        cancelar.addClickListener(e -> { if(cancelListener!=null) cancelListener.onCancel(); });
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        excluir.addThemeVariants(ButtonVariant.LUMO_ERROR);
        excluir.addClickShortcut(Key.DELETE);
        excluir.addClickListener(e -> { if(deleteListener!=null) deleteListener.onDelete(fornecedor); });

        binder.forField(nome)
                .withValidator(new StringLengthValidator("Nome obrigatório",1,100))
                .bind(Fornecedor::getNome, Fornecedor::setNome);

        binder.forField(email)
                .withValidator(v -> v!=null && !v.isBlank(),"E-mail obrigatório")
                .bind(Fornecedor::getEmail, Fornecedor::setEmail);

        add(nome, email, new com.vaadin.flow.component.orderedlayout.HorizontalLayout(salvar, excluir, cancelar));
    }

    private void validateAndSave(){
        try{
            binder.writeBean(fornecedor);
            if(saveListener!=null) saveListener.onSave(fornecedor);
        }catch(ValidationException ex){
            Notification.show("Campos inválidos");
        }
    }

    public void setFornecedor(Fornecedor f){
        this.fornecedor = f;
        binder.readBean(f);
        excluir.setVisible(f.getId()!=null);
    }

    public void setSaveListener(SaveListener l){ this.saveListener = l; }
    public void setDeleteListener(DeleteListener l){ this.deleteListener = l; }
    public void setCancelListener(CancelListener l){ this.cancelListener = l; }
} 