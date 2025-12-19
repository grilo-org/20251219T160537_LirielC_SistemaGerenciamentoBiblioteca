package com.biblioteca.views.carrinho;

import com.biblioteca.model.Carrinho;
import com.biblioteca.model.LivroCarrinho;
import com.biblioteca.service.CarrinhoService;
import com.biblioteca.service.PagamentoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.biblioteca.model.TipoCompra;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.component.html.Div;

/**
 * Diálogo simples de checkout.
 */
public class CheckoutDialog extends Dialog {

    public interface CheckoutListener { void onFinished(); }

    public CheckoutDialog(Carrinho carrinho, CarrinhoService carrinhoService, CheckoutListener listener){
        setHeaderTitle("Finalizar Compra");
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        
        // Informações sobre quem está fazendo o pedido
        criarSecaoInfoPedido(content, carrinho);
        
        RadioButtonGroup<TipoCompra> tipoCompra = new RadioButtonGroup<>();
        tipoCompra.setLabel("Tipo");
        tipoCompra.setItems(TipoCompra.COMPRA, TipoCompra.ALUGUEL);
        tipoCompra.setValue(TipoCompra.COMPRA);

        VerticalLayout resumo = new VerticalLayout();
        resumo.setSpacing(false);
        resumo.setPadding(false);

        // Label prazo devolução
        com.vaadin.flow.component.html.Span prazoSpan = new com.vaadin.flow.component.html.Span();

        // Seleção fixa de forma de pagamento (card ou boleto) para evitar valores inválidos
        RadioButtonGroup<String> formaPagamento = new RadioButtonGroup<>();
        formaPagamento.setLabel("Forma de Pagamento");
        formaPagamento.setItems("card", "boleto");
        // Mostra rótulos legíveis ao usuário
        formaPagamento.setRenderer(new ComponentRenderer<>(value -> new Span("card".equals(value) ? "Cartão" : "Boleto")));
        formaPagamento.setValue("card");

        // Campo CPF - somente editável se ainda não estiver definido
        String cpfCliente = carrinho.getCliente().getCpf();
        com.vaadin.flow.component.textfield.TextField cpfField = new com.vaadin.flow.component.textfield.TextField("CPF do Cliente");
        cpfField.setPlaceholder("000.000.000-00");
        
        // Se o CPF já está definido, torna o campo somente leitura
        boolean cpfJaDefinido = cpfCliente != null && !cpfCliente.isBlank();
        if (cpfJaDefinido) {
            cpfField.setValue(formatarCpf(cpfCliente));
            cpfField.setReadOnly(true);
            cpfField.setHelperText("CPF já definido no cadastro do cliente");
        } else {
            cpfField.setValue("");
            cpfField.setReadOnly(false);
        }

        java.util.function.Consumer<Void> atualizarResumo = v -> {
            resumo.removeAll();
            boolean aluguel = tipoCompra.getValue().isAluguel();
            for(LivroCarrinho lc: carrinho.getLivros()){
                double valorItem = lc.calcularValor(aluguel);
                resumo.add(new Span(lc.getLivro().getTitulo()+" x"+lc.getQuantidade()+" - R$ "+String.format("%.2f", valorItem)));
            }
            double total = carrinho.calcularTotal(aluguel);
            resumo.add(new Span("Total: R$ "+String.format("%.2f", total)));
        };

        atualizarResumo.accept(null);

        // Exibe data limite se ALUGUEL
        java.util.function.Consumer<Void> atualizarPrazo = v -> {
            if(tipoCompra.getValue().isAluguel()){
                java.time.LocalDate limite = java.time.LocalDate.now().plusDays(7); // TODO ler do config
                prazoSpan.setText("Devolução até "+limite.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }else{
                prazoSpan.setText("");
            }
        };
        atualizarPrazo.accept(null);
        tipoCompra.addValueChangeListener(e -> {atualizarResumo.accept(null); atualizarPrazo.accept(null);} );

        content.add(tipoCompra, resumo, prazoSpan, formaPagamento, cpfField);

        Button confirmar = new Button("Confirmar");
        Button cancelar = new Button("Cancelar", e-> close());

        confirmar.addClickListener(e -> {
            try{
                // Calcula total localmente só para validar
                boolean aluguel = tipoCompra.getValue().isAluguel();
                double total = carrinho.calcularTotal(aluguel);
                if("boleto".equals(formaPagamento.getValue()) && total < 5.0){
                    Notification.show("Valor mínimo para boleto é R$ 5,00",3000, Notification.Position.TOP_CENTER);
                    return;
                }

                String cpfUsar;
                if (cpfJaDefinido) {
                    // Usa o CPF já existente no cliente (remove formatação se necessário)
                    cpfUsar = cpfCliente.replaceAll("[^0-9]", "");
                } else {
                    // CPF precisa ser informado
                    cpfUsar = cpfField.getValue();
                    if(cpfUsar==null || cpfUsar.isBlank()){
                        Notification.show("Informe o CPF",3000, Notification.Position.TOP_CENTER);
                        return;
                    }
                    // Remove formatação do CPF digitado
                    cpfUsar = cpfUsar.replaceAll("[^0-9]", "");
                    
                    // Atualiza CPF do cliente apenas se não estava definido antes
                    carrinho.getCliente().setCpf(cpfUsar);
                }

                // Cria sessão Stripe diretamente
                com.stripe.model.checkout.Session session = PagamentoService.criarSessaoCheckout(carrinho, formaPagamento.getValue(), cpfUsar, aluguel);

                if(session!=null && session.getUrl()!=null){
                    com.vaadin.flow.component.UI.getCurrent().getPage().setLocation(session.getUrl());
                    close();
                }else{
                    Notification.show("Falha ao criar sessão de pagamento",4000, Notification.Position.TOP_CENTER);
                }
            }catch(Exception ex){
                Notification.show("Erro: "+ex.getMessage(),4000, Notification.Position.TOP_CENTER);
            }
        });

        getFooter().add(confirmar, cancelar);
        add(content);
    }
    
    private void criarSecaoInfoPedido(VerticalLayout content, Carrinho carrinho) {
        // Informações do cliente
        String nomeCliente = carrinho.getCliente().getNome();
        String cpfCliente = carrinho.getCliente().getCpf();
        
        Div infoDiv = new Div();
        infoDiv.getStyle()
            .set("background", "#f8f9fa")
            .set("border", "1px solid #dee2e6")
            .set("border-radius", "5px")
            .set("padding", "10px")
            .set("margin-bottom", "15px");
        
        Span tituloInfo = new Span("👤 Cliente");
        tituloInfo.getStyle()
            .set("font-weight", "bold")
            .set("font-size", "16px")
            .set("display", "block")
            .set("margin-bottom", "8px")
            .set("color", "#495057");
        
        Span cliente = new Span(nomeCliente);
        if (cpfCliente != null && !cpfCliente.isBlank()) {
            cliente.setText(nomeCliente + " (CPF: " + formatarCpf(cpfCliente) + ")");
        }
        cliente.getStyle()
            .set("display", "block")
            .set("font-size", "14px")
            .set("color", "#28a745")
            .set("font-weight", "500");
        
        infoDiv.add(tituloInfo, cliente);
        content.add(infoDiv);
    }
    

    
    private String formatarCpf(String cpf) {
        if (cpf != null && cpf.length() == 11) {
            return cpf.substring(0,3) + "." + cpf.substring(3,6) + "." + 
                   cpf.substring(6,9) + "-" + cpf.substring(9,11);
        }
        return cpf;
    }
} 