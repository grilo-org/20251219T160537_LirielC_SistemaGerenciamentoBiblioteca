package com.biblioteca.views.usuario;

import com.biblioteca.model.Usuario;
import com.biblioteca.util.CpfValidator;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserForm extends FormLayout {

    private final TextField nome = new TextField("Nome");
    private final TextField login = new TextField("Login");
    private final EmailField email = new EmailField("E-mail");
    private final TextField cpf = new TextField("CPF");
    private final TextField endereco = new TextField("Endereço");
    private final TextField telefone = new TextField("Telefone");
    private final PasswordField senha = new PasswordField("Senha");
    private final ComboBox<String> role = new ComboBox<>("Perfil");
    private final Checkbox ativo = new Checkbox("Ativo");

    private final Button salvar = new Button("Salvar");
    private final Button excluir = new Button("Excluir");
    private final Button cancelar = new Button("Cancelar");

    private final Binder<Usuario> binder = new Binder<>(Usuario.class);
    private Usuario usuario;

    public interface SaveListener { void onSave(Usuario u); }
    public interface DeleteListener { void onDelete(Usuario u); }
    public interface CancelListener { void onCancel(); }

    private SaveListener saveListener;
    private DeleteListener deleteListener;
    private CancelListener cancelListener;

    public UserForm() {
        configurarCampos();
        configurarRolesPorPermissao();
        configureBinder();
        configureButtons();
        
        // Layout do formulário
        add(nome, login, email, cpf, endereco, telefone, senha, role, ativo,
                new com.vaadin.flow.component.orderedlayout.HorizontalLayout(salvar, excluir, cancelar));
        setResponsiveSteps(new ResponsiveStep("0",1), new ResponsiveStep("500px", 2));
    }

    private void configurarCampos() {
        // Campo CPF com formatação automática
        cpf.setPlaceholder("000.000.000-00");
        cpf.setMaxLength(14); // Para permitir formatação
        cpf.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value != null && !value.isEmpty()) {
                // Remove tudo que não é número
                String numbers = value.replaceAll("[^0-9]", "");
                
                // Formata o CPF automaticamente
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
                    
                    // Atualiza o campo sem triggerar novamente o listener
                    if (!formatted.toString().equals(value)) {
                        cpf.setValue(formatted.toString());
                    }
                }
            }
        });

        // Campo telefone com placeholder
        telefone.setPlaceholder("(99) 99999-9999");
        
        // Campo endereço mais largo
        endereco.setWidthFull();
        
        // Configurações dos outros campos
        nome.setRequired(true);
        login.setRequired(true);
        email.setRequired(true);
        cpf.setRequired(true);
        senha.setRequired(true);
        role.setRequired(true);
        
        // Checkbox ativo marcado por padrão
        ativo.setValue(true);
    }

    /**
     * Configura os roles disponíveis baseado no role do usuário logado
     */
    private void configurarRolesPorPermissao() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.getAuthorities() != null) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
            
            boolean isGerente = auth.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_GERENTE"));
            
            if (isAdmin) {
                // ADMIN pode criar qualquer tipo de usuário
                role.setItems("ADMIN", "GERENTE", "FUNCIONARIO", "CLIENTE");
            } else if (isGerente) {
                // GERENTE só pode criar FUNCIONARIO e CLIENTE
                role.setItems("FUNCIONARIO", "CLIENTE");
            } else {
                // Outros roles (se houver) só podem criar CLIENTE
                role.setItems("CLIENTE");
            }
        } else {
            // Fallback: se não conseguir determinar o role, permite apenas CLIENTE
            role.setItems("CLIENTE");
        }
    }

    private void configureBinder() {
        binder.forField(nome)
                .withValidator(new StringLengthValidator("Nome é obrigatório", 1, 100))
                .bind(Usuario::getNome, Usuario::setNome);

        binder.forField(login)
                .withValidator(new StringLengthValidator("Login é obrigatório", 1, 50))
                .bind(Usuario::getLogin, Usuario::setLogin);

        binder.forField(email)
                .withValidator(new EmailValidator("E-mail inválido"))
                .bind(Usuario::getEmail, Usuario::setEmail);

        binder.forField(cpf)
                .withValidator(value -> {
                    if (value == null || value.trim().isEmpty()) {
                        return false; // CPF é obrigatório
                    }
                    if (!CpfValidator.isValid(value)) {
                        return false; // CPF deve ser válido
                    }
                    return true;
                }, "CPF é obrigatório e deve ser válido")
                .bind(Usuario::getCpf, Usuario::setCpf);

        binder.forField(endereco)
                .bind(Usuario::getEndereco, Usuario::setEndereco);

        binder.forField(telefone)
                .bind(Usuario::getTelefone, Usuario::setTelefone);

        binder.forField(senha)
                .withValidator(new StringLengthValidator("Senha é obrigatória", 1, 255))
                .bind(Usuario::getSenha, Usuario::setSenha);

        binder.forField(role)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Perfil é obrigatório")
                .bind(Usuario::getRole, Usuario::setRole);

        binder.forField(ativo)
                .bind(Usuario::getStatus, Usuario::setStatus);
    }

    private void configureButtons() {
        salvar.addClickShortcut(Key.ENTER);
        salvar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        salvar.addClickListener(e -> {
            try {
                // Normaliza o CPF antes de salvar (remove formatação)
                if (cpf.getValue() != null && !cpf.getValue().trim().isEmpty()) {
                    String cpfLimpo = cpf.getValue().replaceAll("[^0-9]", "");
                    cpf.setValue(cpfLimpo);
                }
                
                binder.writeBean(usuario);
                if(saveListener!=null) saveListener.onSave(usuario);
            } catch (ValidationException ex) {
                Notification.show("Por favor, corrija os campos inválidos", 4000, Notification.Position.TOP_CENTER);
            }
        });

        excluir.addThemeVariants(ButtonVariant.LUMO_ERROR);
        excluir.addClickShortcut(Key.DELETE, KeyModifier.ALT);
        excluir.addClickListener(e -> { if(deleteListener!=null) deleteListener.onDelete(usuario); });

        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelar.addClickListener(e -> { if(cancelListener!=null) cancelListener.onCancel(); });
    }

    public void setUsuario(Usuario u){
        this.usuario = u;
        
        // Se é um usuário existente, formata o CPF para exibição
        if (u != null && u.getCpf() != null && !u.getCpf().isEmpty()) {
            String cpfFormatado = CpfValidator.format(u.getCpf());
            u.setCpf(cpfFormatado);
        }
        
        binder.readBean(u);
    }

    public void setSaveListener(SaveListener l){this.saveListener=l;}
    public void setDeleteListener(DeleteListener l){this.deleteListener=l;}
    public void setCancelListener(CancelListener l){this.cancelListener=l;}
} 