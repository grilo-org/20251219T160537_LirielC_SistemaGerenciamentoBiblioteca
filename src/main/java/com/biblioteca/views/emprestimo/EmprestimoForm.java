package com.biblioteca.views.emprestimo;

import com.biblioteca.model.Livro;
import com.biblioteca.model.Usuario;
import com.biblioteca.service.UsuarioService;
import com.biblioteca.util.CpfValidator;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.List;

/**
 * Formulário para criar novo empréstimo.
 * Permite digitar CPF do usuário e selecionar livro disponível.
 */
public class EmprestimoForm extends FormLayout {

    private final TextField cpfUsuarioField = new TextField("CPF do Usuário");
    private final Button buscarUsuarioBtn = new Button("Buscar", VaadinIcon.SEARCH.create());
    private final Span infoUsuarioSpan = new Span();
    private final ComboBox<Livro> livroField = new ComboBox<>("Livro disponível");

    private final Button salvar = new Button("Salvar");
    private final Button cancelar = new Button("Cancelar");

    private final Binder<EmprestimoFormBean> binder = new Binder<>(EmprestimoFormBean.class);
    
    private final UsuarioService usuarioService;
    private Usuario usuarioSelecionado;

    private SaveListener saveListener;
    private CancelListener cancelListener;

    public interface SaveListener { void onSave(Usuario usuario, Livro livro); }
    public interface CancelListener { void onCancel(); }

    public EmprestimoForm(List<Livro> livrosDisponiveis, UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
        
        configurarCamposCpf();
        configurarLivros(livrosDisponiveis);
        configurarBotoes();
        configurarLayout();
        configurarBinder();
    }

    private void configurarCamposCpf() {
        // Campo CPF com formatação automática
        cpfUsuarioField.setPlaceholder("000.000.000-00");
        cpfUsuarioField.setMaxLength(14);
        cpfUsuarioField.setWidth("200px");
        cpfUsuarioField.setPrefixComponent(VaadinIcon.USER.create());
        
        // Formatação automática do CPF
        cpfUsuarioField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value != null && !value.isEmpty()) {
                String numbers = value.replaceAll("[^0-9]", "");
                
                if (numbers.length() > 0) {
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < numbers.length() && i < 11; i++) {
                        if (i == 3 || i == 6) {
                            formatted.append(".");
                        } else if (i == 9) {
                            formatted.append("-");
                        }
                        formatted.append(numbers.charAt(i));
                    }
                    
                    if (!formatted.toString().equals(value)) {
                        cpfUsuarioField.setValue(formatted.toString());
                    }
                }
            }
            
            // Limpa usuário selecionado se CPF mudou
            if (usuarioSelecionado != null) {
                usuarioSelecionado = null;
                infoUsuarioSpan.setText("");
                atualizarEstadoBotaoSalvar();
            }
        });

        // Botão buscar usuário
        buscarUsuarioBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buscarUsuarioBtn.addClickListener(e -> buscarUsuario());
        
        // Permitir busca com Enter no campo CPF
        cpfUsuarioField.addKeyPressListener(Key.ENTER, e -> buscarUsuario());

        // Span para mostrar informações do usuário encontrado
        infoUsuarioSpan.getStyle()
            .set("font-weight", "bold")
            .set("color", "#28a745")
            .set("display", "block")
            .set("margin-top", "5px");
    }

    private void configurarLivros(List<Livro> livrosDisponiveis) {
        livroField.setItems(livrosDisponiveis);
        livroField.setItemLabelGenerator(livro -> 
            livro.getTitulo() + " - " + livro.getAutor() + 
            " (Estoque: " + livro.getQuantidadeEstoque() + ")");
        livroField.setWidthFull();
    }

    private void configurarBotoes() {
        salvar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        salvar.addClickShortcut(Key.ENTER);
        salvar.addClickListener(e -> validateAndSave());
        salvar.setEnabled(false); // Desabilitado até selecionar usuário

        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelar.addClickListener(e -> { 
            if(cancelListener != null) cancelListener.onCancel();
        });
    }

    private void configurarLayout() {
        // Layout da busca de usuário
        HorizontalLayout buscaUsuarioLayout = new HorizontalLayout(cpfUsuarioField, buscarUsuarioBtn);
        buscaUsuarioLayout.setAlignItems(Alignment.END);
        buscaUsuarioLayout.setSpacing(true);

        // Layout dos botões
        HorizontalLayout botoesLayout = new HorizontalLayout(salvar, cancelar);
        botoesLayout.setSpacing(true);

        add(buscaUsuarioLayout, infoUsuarioSpan, livroField, botoesLayout);
        setResponsiveSteps(new ResponsiveStep("0", 1));
    }

    private void configurarBinder() {
        binder.forField(livroField)
            .asRequired("Livro é obrigatório")
            .bind(EmprestimoFormBean::getLivro, EmprestimoFormBean::setLivro);
    }

    private void buscarUsuario() {
        String cpf = cpfUsuarioField.getValue();
        
        if (cpf == null || cpf.trim().isEmpty()) {
            Notification.show("Digite um CPF para buscar", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        // Remove formatação
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        
        if (cpfLimpo.length() != 11) {
            Notification.show("CPF deve ter 11 dígitos", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        if (!CpfValidator.isValid(cpfLimpo)) {
            Notification.show("CPF inválido", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        try {
            Usuario usuario = usuarioService.buscarUsuarioPorCpf(cpfLimpo).orElse(null);
            
            if (usuario != null) {
                usuarioSelecionado = usuario;
                String cpfFormatado = CpfValidator.format(cpfLimpo);
                infoUsuarioSpan.setText("✅ Usuário: " + usuario.getNome() + 
                    " (" + usuario.getRole() + ") - CPF: " + cpfFormatado);
                atualizarEstadoBotaoSalvar();
                
                Notification.show("Usuário encontrado: " + usuario.getNome(), 
                    3000, Notification.Position.MIDDLE);
            } else {
                usuarioSelecionado = null;
                infoUsuarioSpan.setText("❌ Usuário não encontrado com este CPF");
                infoUsuarioSpan.getStyle().set("color", "#dc3545");
                atualizarEstadoBotaoSalvar();
                
                Notification.show("Usuário não encontrado com este CPF", 
                    3000, Notification.Position.MIDDLE);
            }
            
        } catch (Exception e) {
            Notification.show("Erro ao buscar usuário: " + e.getMessage(), 
                3000, Notification.Position.MIDDLE);
            usuarioSelecionado = null;
            infoUsuarioSpan.setText("");
            atualizarEstadoBotaoSalvar();
        }
    }

    private void atualizarEstadoBotaoSalvar() {
        boolean temUsuario = usuarioSelecionado != null;
        salvar.setEnabled(temUsuario);
        
        if (temUsuario) {
            infoUsuarioSpan.getStyle().set("color", "#28a745");
        }
    }

    private void validateAndSave() {
        if (usuarioSelecionado == null) {
            Notification.show("Busque e selecione um usuário válido", 
                3000, Notification.Position.TOP_CENTER);
            return;
        }
        
        try {
            EmprestimoFormBean bean = new EmprestimoFormBean();
            binder.writeBean(bean);
            
            if (saveListener != null) {
                saveListener.onSave(usuarioSelecionado, bean.getLivro());
            }
        } catch (ValidationException ex) {
            Notification.show("Por favor, selecione um livro", 
                3000, Notification.Position.TOP_CENTER);
        }
    }

    public void setSaveListener(SaveListener listener) { 
        this.saveListener = listener; 
    }
    
    public void setCancelListener(CancelListener listener) { 
        this.cancelListener = listener; 
    }

    /**
     * Bean interno para binder (não é entidade).
     */
    public static class EmprestimoFormBean {
        private Livro livro;
        
        public Livro getLivro() { 
            return livro; 
        }
        
        public void setLivro(Livro livro) { 
            this.livro = livro; 
        }
    }
} 