package com.biblioteca.views.livro;

import com.biblioteca.model.Livro;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.biblioteca.util.IsbnValidator;

/**
 * Formulário reutilizável para criar/editar livros.
 */
public class LivroForm extends FormLayout {

    private final TextField titulo = new TextField("Título");
    private final TextField autor = new TextField("Autor");
    private final TextField isbn = new TextField("ISBN");
    private final NumberField valor = new NumberField("Valor (R$)");
    private final IntegerField quantidade = new IntegerField("Estoque");
    private final TextField urlImagem = new TextField("URL da Imagem");
    private final Image previewImagem = new Image("https://via.placeholder.com/120x180/cccccc/999999?text=Preview", "Preview da capa");

    private final Button salvar = new Button("Salvar");
    private final Button excluir = new Button("Excluir");
    private final Button cancelar = new Button("Cancelar");

    private final Binder<Livro> binder = new Binder<>(Livro.class);

    private Livro livro;

    public interface SaveListener { void onSave(Livro livro); }
    public interface DeleteListener { void onDelete(Livro livro); }
    public interface CancelListener { void onCancel(); }

    private SaveListener saveListener;
    private DeleteListener deleteListener;
    private CancelListener cancelListener;

    public LivroForm() {
        configureForm();
    }

    private void configureForm() {
        valor.setPrefixComponent(new com.vaadin.flow.component.html.Span("R$"));
        valor.setStep(0.1);
        valor.setMin(0);

        quantidade.setMin(0);
        
        // Configurar campo ISBN
        isbn.setPlaceholder("Ex: 978-0-123-45678-9 ou 0-123-45678-X");
        isbn.setHelperText("ISBN-10 ou ISBN-13 (com ou sem hífens)");
        
        // Validação do ISBN em tempo real
        isbn.addValueChangeListener(e -> {
            String isbnValue = e.getValue();
            if (isbnValue != null && !isbnValue.trim().isEmpty()) {
                if (IsbnValidator.isValidIsbn(isbnValue)) {
                    isbn.setInvalid(false);
                    isbn.setErrorMessage(null);
                    // Formatar o ISBN automaticamente
                    String formatted = IsbnValidator.formatIsbn(isbnValue);
                    if (!formatted.equals(isbnValue)) {
                        isbn.setValue(formatted);
                    }
                } else {
                    isbn.setInvalid(true);
                    isbn.setErrorMessage("ISBN inválido. Use formato ISBN-10 ou ISBN-13.");
                }
            } else {
                isbn.setInvalid(false);
                isbn.setErrorMessage(null);
            }
        });
        
        // Configurar preview da imagem
        previewImagem.setWidth("120px");
        previewImagem.setHeight("180px");
        previewImagem.getElement().getStyle().set("border", "1px solid #ccc");
        previewImagem.getElement().getStyle().set("border-radius", "4px");
        
        // Atualizar preview quando URL da imagem mudar
        urlImagem.addValueChangeListener(e -> {
            String url = e.getValue();
            if (url != null && !url.trim().isEmpty()) {
                previewImagem.setSrc(url);
                // Adicionar tratamento de erro
                previewImagem.getElement().setAttribute("onerror", 
                    "this.src='https://via.placeholder.com/120x180/ffcccc/999999?text=URL+Inválida'");
            } else {
                previewImagem.setSrc("https://via.placeholder.com/120x180/cccccc/999999?text=Preview");
            }
        });

        salvar.addClickListener(e -> validateAndSave());
        salvar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        salvar.addClickShortcut(Key.ENTER);

        cancelar.addClickListener(e -> {
            if (cancelListener != null) cancelListener.onCancel();
        });
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        excluir.addThemeVariants(ButtonVariant.LUMO_ERROR);
        excluir.addClickShortcut(Key.DELETE, KeyModifier.ALT);
        excluir.addClickListener(e -> {
            if (deleteListener != null) deleteListener.onDelete(livro);
        });

        binder.forField(titulo)
                .withValidator(new StringLengthValidator(
                        "Título deve ter entre 1 e 100 caracteres", 1, 100))
                .bind(Livro::getTitulo, Livro::setTitulo);

        binder.forField(autor)
                .withValidator(new StringLengthValidator(
                        "Autor deve ter entre 1 e 100 caracteres", 1, 100))
                .bind(Livro::getAutor, Livro::setAutor);

        binder.forField(isbn)
                .withValidator(value -> value == null || value.trim().isEmpty() || IsbnValidator.isValidIsbn(value),
                        "ISBN inválido")
                .bind(Livro::getIsbn, Livro::setIsbn);

        binder.forField(valor)
                .withNullRepresentation(0.0)
                .withValidator(v -> v != null && v >= 0, "Valor deve ser ≥ 0")
                .bind(Livro::getValor, Livro::setValor);

        binder.forField(quantidade)
                .withNullRepresentation(0)
                .withValidator(q -> q == null || q >= 0, "Estoque não pode ser negativo")
                .bind(Livro::getQuantidadeEstoque, Livro::setQuantidadeEstoque);

        binder.forField(urlImagem)
                .bind(Livro::getUrlImagem, Livro::setUrlImagem);

        // Layout para organizar o formulário com preview
        HorizontalLayout imagemLayout = new HorizontalLayout();
        imagemLayout.add(urlImagem, previewImagem);
        imagemLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
        
        add(titulo, autor, isbn, valor, quantidade, imagemLayout, new com.vaadin.flow.component.orderedlayout.HorizontalLayout(salvar, excluir, cancelar));
    }

    private void validateAndSave() {
        try {
            binder.writeBean(livro);
            if (saveListener != null) saveListener.onSave(livro);
        } catch (ValidationException ex) {
            Notification.show("Campos inválidos");
        }
    }

    public void setLivro(Livro livro) {
        this.livro = livro;
        binder.readBean(livro);
        
        // Atualizar preview da imagem ao carregar livro
        String url = livro.getUrlImagem();
        if (url != null && !url.trim().isEmpty()) {
            previewImagem.setSrc(url);
            previewImagem.getElement().setAttribute("onerror", 
                "this.src='https://via.placeholder.com/120x180/ffcccc/999999?text=URL+Inválida'");
        } else {
            previewImagem.setSrc("https://via.placeholder.com/120x180/cccccc/999999?text=Preview");
        }
    }

    public void setSaveListener(SaveListener listener) { this.saveListener = listener; }
    public void setDeleteListener(DeleteListener listener) { this.deleteListener = listener; }
    public void setCancelListener(CancelListener listener) { this.cancelListener = listener; }
} 