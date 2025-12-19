package com.biblioteca.views;

import com.biblioteca.service.UsuarioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value="minha-conta", layout = MainLayout.class)
@PageTitle("Minha Conta | Sistema Biblioteca")
@RolesAllowed({"CLIENTE","GERENTE","ADMIN","FUNCIONARIO","USER"})
public class MyAccountView extends VerticalLayout {

    @Autowired
    public MyAccountView(UsuarioService usuarioService, com.biblioteca.service.EmprestimoService emprestimoService){
        setSpacing(true);
        setPadding(true);

        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        var usuarioOpt = usuarioService.buscarUsuarioByLoginWeb(login);
        if(usuarioOpt.isEmpty()){
            add(new com.vaadin.flow.component.html.Span("Usuário não encontrado."));
            return;
        }
        var usuario = usuarioOpt.get();

        TextField nomeField = new TextField("Nome");
        nomeField.setValue(usuario.getNome()!=null?usuario.getNome():"");
        nomeField.setReadOnly(true);

        EmailField emailField = new EmailField("E-mail");
        emailField.setValue(usuario.getEmail()!=null?usuario.getEmail():"");

        TextField cpfField = new TextField("CPF");
        cpfField.setPlaceholder("000.000.000-00");
        boolean cpfJaDefinido = usuario.getCpf()!=null && !usuario.getCpf().isBlank();
        cpfField.setValue(cpfJaDefinido?usuario.getCpf():"");
        cpfField.setReadOnly(cpfJaDefinido);

        TextField enderecoField = new TextField("Endereço completo");
        enderecoField.setWidthFull();
        enderecoField.setValue(usuario.getEndereco()!=null?usuario.getEndereco():"");

        TextField telefoneField = new TextField("Telefone");
        telefoneField.setPlaceholder("(99) 99999-9999");
        telefoneField.setValue(usuario.getTelefone()!=null?usuario.getTelefone():"");

        PasswordField senhaAtual = new PasswordField("Senha Atual");
        PasswordField novaSenha = new PasswordField("Nova Senha");
        PasswordField confirmaSenha = new PasswordField("Confirmar Senha");

        Button salvar = new Button("Salvar", e -> {
            String cpfParaEnviar = cpfJaDefinido?null:cpfField.getValue();
            boolean ok = usuarioService.atualizarDadosContaWeb(login,emailField.getValue(),cpfParaEnviar,enderecoField.getValue(),telefoneField.getValue());
            if(!ok){
                Notification.show("Erro ao salvar dados",3000, Notification.Position.TOP_CENTER);
                return;
            }
            if(!novaSenha.getValue().isBlank()){
                if(!novaSenha.getValue().equals(confirmaSenha.getValue())){
                    Notification.show("Senha e confirmação não conferem",3000, Notification.Position.TOP_CENTER);
                    return;
                }
                boolean senOk = usuarioService.atualizarSenhaWeb(login,novaSenha.getValue());
                if(!senOk){
                    Notification.show("Erro ao alterar senha",3000, Notification.Position.TOP_CENTER);
                    return;
                }
            }
            Notification.show("Dados atualizados!",3000, Notification.Position.TOP_CENTER);
        });

        double multas = emprestimoService.calcularMultasPendentesWeb(usuario);

        com.vaadin.flow.component.html.Span lblMulta = new com.vaadin.flow.component.html.Span("Multas pendentes: R$ "+String.format("%.2f",multas));

        add(new com.vaadin.flow.component.html.H3("Minha Conta"), nomeField,emailField,telefoneField,enderecoField,cpfField,lblMulta,senhaAtual,novaSenha,confirmaSenha,salvar);
    }
} 