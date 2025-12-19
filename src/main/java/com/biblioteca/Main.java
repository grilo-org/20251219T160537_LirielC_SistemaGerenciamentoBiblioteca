package com.biblioteca;

import com.biblioteca.model.*;
import com.biblioteca.service.*;
import com.biblioteca.strategy.MenuStrategySelector;
import com.biblioteca.util.CpfValidator;
import com.biblioteca.util.JPAUtil;
import com.biblioteca.util.DatabaseUtil;
import com.stripe.model.checkout.Session;

import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.EntityManager;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final ArrayList<Livro> livros = new ArrayList<>();
    private static final ArrayList<Usuario> usuarios = new ArrayList<>();
    private static final ArrayList<Fornecedor> fornecedores = new ArrayList<>();
    private static Carrinho carrinhoAtual = null;
    public static Usuario usuarioAtivo = null;

    public static void main(String[] args) {
        DatabaseUtil.criarTabelasSeNaoExistirem();
        inicializarDados();
        // Ensure scanner closes on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(scanner::close));
        while (true) {
            MenuStrategySelector.exibirMenu(usuarioAtivo);
            MenuStrategySelector.gerenciarOpcoes(usuarioAtivo);
        }
    }

    public static void recuperarSenha() {
        System.out.println("\n=== RECUPERAR SENHA ===");
        System.out.println("Email: ");
        String email = scanner.next();
        scanner.nextLine();
        String token = AuthenticationService.generateAuthenticationToken(email);
        EmailService.enviarEmailDeRecuperacaoDeSenhaConsole(email, token);
        System.out.println("Insira o token de recuperação: ");
        String inputToken = scanner.next();
        scanner.nextLine();
        Usuario usuario = UsuarioService.buscarUsuarioByEmail(email);
        atualizarSenha(inputToken, usuario);
    }

    private static void atualizarSenha(String inputToken, Usuario usuario) {
        if (AuthenticationService.matchPassword(inputToken, usuario.getTokenRecuperacao())) {
            System.out.println("Nova senha: ");
            String novaSenha = scanner.next();
            UsuarioService.atualizarSenha(usuario.getLogin(), AuthenticationService.encryptPassword(novaSenha));
            UsuarioService.atualizarTokenRecuperacao(usuario.getEmail(), null);
            System.out.println("Senha atualizada com sucesso!");
        } else {
            System.out.println("Token de recuperação inválido");
        }
    }

    public static void registrarUsuario() {
        Usuario novoUsuario = new Usuario();

        //clearScreen();
        System.out.println("\n=== REGISTRAR-SE ===");
        System.out.println("Nome: ");
        novoUsuario.setNome(scanner.nextLine());
        System.out.println("Email: ");
        novoUsuario.setEmail(scanner.nextLine());
        System.out.println("Login: ");
        novoUsuario.setLogin(scanner.nextLine());
        System.out.println("Senha: ");
        novoUsuario.setSenha(AuthenticationService.encryptPassword(scanner.nextLine()));
        System.out.println("Cpf: ");
        novoUsuario.setCpf(scanner.nextLine());
        System.out.println("Endereço: ");
        novoUsuario.setEndereco(scanner.nextLine());
        novoUsuario.setRole("CLIENTE");
        UsuarioService.cadastrarUsuario(novoUsuario);
    }

    public static void efetuarLogin() {
        //clearScreen();
        System.out.println("\n=== EFETUAR LOGIN ===");
        System.out.println("Login: ");
        String login = scanner.nextLine();
        System.out.println("Senha: ");
        String senha = scanner.nextLine();
        scanner.nextLine();
        Optional<Usuario> usuario = AuthenticationService.authenticateUsuario(login, senha);
        usuario.ifPresent(value -> usuarioAtivo = value);
    }

    public static void gerenciarCarrinho() {
        if (carrinhoAtual == null) {
            System.out.println("\nCriando novo carrinho...");
            
            // Coletar dados do cliente
            System.out.println("\n=== DADOS DO CLIENTE ===");
            scanner.nextLine(); // Limpar buffer
            System.out.print("CPF do cliente (apenas números): ");
            String cpf;
            boolean cpfValido = false;
            do {
                cpf = scanner.nextLine();
                try {
                    if (!CpfValidator.isValid(cpf)) {
                        System.out.println("CPF inválido! Por favor, digite novamente: ");
                    } else {
                        cpfValido = true;
                    }
                } catch (Exception e) {
                    System.out.println("CPF inválido! Por favor, digite apenas números: ");
                }
            } while (!cpfValido);

            Cliente clienteExistente = ClienteService.buscarPorCpf(cpf);
            if (clienteExistente != null) {
                System.out.println("\nCliente encontrado!");
                System.out.println("Nome: " + clienteExistente.getNome());
                System.out.println("Email: " + clienteExistente.getEmail());
                System.out.println("Endereço: " + clienteExistente.getEnderecoCompleto());
                
                System.out.println("\nDeseja atualizar os dados? (S/N)");
                String atualizar = scanner.nextLine();
                if (!atualizar.equalsIgnoreCase("S")) {
                    carrinhoAtual = new Carrinho(1L, new Usuario(clienteExistente.getNome()));
                    carrinhoAtual.setDadosCliente(new DadosCliente(
                        clienteExistente.getNome(),
                        clienteExistente.getCpf(),
                        clienteExistente.getEmail(),
                        clienteExistente.getEnderecoCompleto()
                    ));
                    return;
                }
            } else {
                Cliente novoCliente = coletarDadosCliente(cpf);
                carrinhoAtual = new Carrinho(1L, new Usuario(novoCliente.getNome()));
                carrinhoAtual.setDadosCliente(new DadosCliente(
                    novoCliente.getNome(),
                    novoCliente.getCpf(),
                    novoCliente.getEmail(),
                    novoCliente.getEnderecoCompleto()
                ));
            }
        }

        while (true) {
            System.out.println("\n=== GERENCIAR CARRINHO ===");
            System.out.println("1. Adicionar Livro");
            System.out.println("2. Remover Livro");
            System.out.println("3. Ver Carrinho");
            System.out.println("4. Voltar");
            System.out.print("Escolha uma opção: ");

            int opcao = scanner.nextInt();
            scanner.nextLine(); // Limpar o buffer

            switch (opcao) {
                case 1:
                    adicionarLivroAoCarrinho();
                    break;
                case 2:
                    removerLivroDoCarrinho();
                    break;
                case 3:
                    verCarrinho();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Opção inválida!");
            }
        }
    }

    private static Cliente coletarDadosCliente(String cpf) {
        System.out.print("Nome do cliente: ");
        String nomeCliente = scanner.nextLine();
        
        System.out.print("Email do cliente: ");
        String email = scanner.nextLine();

        System.out.println("\n=== ENDEREÇO DO CLIENTE ===");
        System.out.print("Rua: ");
        String rua = scanner.nextLine();
        System.out.print("Número: ");
        String numero = scanner.nextLine();
        System.out.print("Complemento: ");
        String complemento = scanner.nextLine();
        System.out.print("Bairro: ");
        String bairro = scanner.nextLine();
        System.out.print("Cidade: ");
        String cidade = scanner.nextLine();
        System.out.print("Estado: ");
        String estado = scanner.nextLine();
        System.out.print("CEP: ");
        String cep = scanner.nextLine();

        
        Cliente cliente = new Cliente(nomeCliente, cpf, email, rua, numero, 
            complemento, bairro, cidade, estado, cep);
        return ClienteService.salvarCliente(cliente);
    }

    private static void adicionarLivroAoCarrinho() {
        System.out.println("\nLivros disponíveis:");
        listarLivros();
        System.out.print("Digite o ID do livro: ");
        long livroId = scanner.nextLong();
        Livro livro = buscarLivro(livroId);
        
        if (livro != null) {
            System.out.print("Digite a quantidade: ");
            int quantidade = scanner.nextInt();
            carrinhoAtual.adicionarLivro(livro, quantidade);
            System.out.println("Livro adicionado ao carrinho!");
        } else {
            System.out.println("Livro não encontrado!");
        }
    }

    private static void removerLivroDoCarrinho() {
        if (carrinhoAtual.getLivros().isEmpty()) {
            System.out.println("Carrinho vazio!");
            return;
        }

        verCarrinho();
        System.out.print("Digite o título do livro para remover: ");
        String titulo = scanner.nextLine();
        
        System.out.print("Digite a quantidade para remover: ");
        int quantidade = scanner.nextInt();
        
        carrinhoAtual.removerLivro(titulo, quantidade);
        System.out.println("Operação concluída!");
    }

    private static void verCarrinho() {
        if (carrinhoAtual.getLivros().isEmpty()) {
            System.out.println("Carrinho vazio!");
            return;
        }

        System.out.println("\n=== CARRINHO ATUAL ===");
        for (LivroCarrinho lc : carrinhoAtual.getLivros()) {
            System.out.printf("Livro: %s - Quantidade: %d - Valor unitário: R$%.2f - Total: R$%.2f\n",
                lc.getLivro().getTitulo(),
                lc.getQuantidade(),
                lc.getLivro().getValor(),
                lc.calcularValor());
        }
        System.out.printf("Total do carrinho: R$%.2f\n", carrinhoAtual.calcularTotal());
    }

    public static void realizarCompra() {
        if (carrinhoAtual == null || carrinhoAtual.getLivros().isEmpty()) {
            System.out.println("Carrinho vazio! Adicione itens antes de finalizar a compra.");
            return;
        }

        System.out.println("\n=== FINALIZAR COMPRA ===");
        verCarrinho();

        System.out.println("\nFornecedores disponíveis:");
        for (Fornecedor f : fornecedores) {
            System.out.println(f.getId() + " - " + f.getNome());
        }

        System.out.print("Digite o ID do fornecedor: ");
        long fornecedorId = scanner.nextLong();
        Fornecedor fornecedor = buscarFornecedor(fornecedorId);

        if (fornecedor == null) {
            System.out.println("Fornecedor não encontrado!");
            return;
        }

        Compra compra = new Compra(1L, carrinhoAtual, fornecedor, carrinhoAtual.getCliente(), LocalDate.now());
        compra.printCompra();

        
        System.out.println("\n=== DADOS DO CLIENTE ===");
        scanner.nextLine(); // Limpar buffer
        System.out.print("Nome do cliente: ");
        String nomeCliente = scanner.nextLine();
        
        String cpf;
        boolean cpfValido = false;
        do {
            System.out.print("CPF do cliente (apenas números): ");
            cpf = scanner.nextLine();
            try {
                if (!CpfValidator.isValid(cpf)) {
                    System.out.println("CPF inválido! Por favor, digite novamente.");
                } else {
                    cpfValido = true;
                }
            } catch (Exception e) {
                System.out.println("CPF inválido! Por favor, digite apenas números.");
            }
        } while (!cpfValido);

        
        Cliente clienteExistente = ClienteService.buscarPorCpf(cpf);
        if (clienteExistente != null) {
            System.out.println("\nCliente já cadastrado! Deseja atualizar os dados? (S/N)");
            String atualizar = scanner.nextLine();
            if (!atualizar.equalsIgnoreCase("S")) {
                nomeCliente = clienteExistente.getNome();
                System.out.println("Email: " + clienteExistente.getEmail());
                System.out.println("Endereço: " + clienteExistente.getEnderecoCompleto());
                
                processarVendaEGerarNota(fornecedor, clienteExistente);
                return;
            }
        }
        
        System.out.print("Email do cliente: ");
        String email = scanner.nextLine();

        System.out.println("\n=== ENDEREÇO DO CLIENTE ===");
        System.out.print("Rua: ");
        String rua = scanner.nextLine();
        System.out.print("Número: ");
        String numero = scanner.nextLine();
        System.out.print("Complemento: ");
        String complemento = scanner.nextLine();
        System.out.print("Bairro: ");
        String bairro = scanner.nextLine();
        System.out.print("Cidade: ");
        String cidade = scanner.nextLine();
        System.out.print("Estado: ");
        String estado = scanner.nextLine();
        System.out.print("CEP: ");
        String cep = scanner.nextLine();

        Cliente cliente = new Cliente(nomeCliente, cpf, email, rua, numero, 
            complemento, bairro, cidade, estado, cep);
        cliente = ClienteService.salvarCliente(cliente);

        processarVendaEGerarNota(fornecedor, cliente);
    }

    private static void processarVendaEGerarNota(Fornecedor fornecedor, Cliente cliente) {
        System.out.println("\n=== FORMA DE PAGAMENTO ===");
        System.out.println("1. Cartão de Crédito");
        System.out.println("2. Boleto Bancário");
        System.out.print("Escolha a forma de pagamento (1 ou 2): ");
        int opcaoPagamento = scanner.nextInt();
        scanner.nextLine(); 

        String tipoPagamento = opcaoPagamento == 1 ? "card" : "boleto";

        System.out.println("\nProcessando pagamento...");
        Session sessaoPagamento = PagamentoService.criarSessaoCheckout(carrinhoAtual, tipoPagamento, cliente.getCpf(), false);
        
        if (sessaoPagamento != null) {
            System.out.println("\n=== INFORMAÇÕES DO PAGAMENTO ===");
            System.out.println("Link para pagamento: " + sessaoPagamento.getUrl());
            System.out.println("\nPor favor, acesse o link acima para finalizar o pagamento.");
            System.out.println("Após a confirmação do pagamento, os documentos fiscais serão");
            System.out.println("gerados e enviados para o email: " + cliente.getEmail());
            
            System.out.print("\nPressione ENTER após realizar o pagamento para continuar...");
            scanner.nextLine();
            
            Venda venda = new Venda();
            venda.setId(sessaoPagamento.getId());
            venda.setClienteNome(cliente.getNome());
            venda.setClienteCpf(cliente.getCpf());
            venda.setClienteEmail(cliente.getEmail());
            venda.setClienteEndereco(cliente.getEnderecoCompleto());
            venda.setValorTotal(carrinhoAtual.calcularTotal());
            venda.setTipoPagamento(tipoPagamento);
            venda.setDataVenda(LocalDateTime.now());
            venda.setStatus("PAGO");
            
            for (LivroCarrinho livroCarrinho : carrinhoAtual.getLivros()) {
                ItemVenda item = new ItemVenda();
                item.setLivro(livroCarrinho.getLivro());
                item.setQuantidade(livroCarrinho.getQuantidade());
                item.setValorUnitario(Double.valueOf(livroCarrinho.getLivro().getValor()));
                item.setValorTotal(livroCarrinho.calcularValor());
                venda.addItem(item);
            }
            
            DocumentoFiscalService.gerarNotaFiscal(venda);
            DocumentoFiscalService.gerarRecibo(venda);
            
            PagamentoService.salvarVendaNoBanco(venda);
            
            HashMap<String, Object> vendaMap = new HashMap<>();
            vendaMap.put("sessionId", venda.getId());
            vendaMap.put("cliente", venda.getClienteNome());
            vendaMap.put("cpf", venda.getClienteCpf());
            vendaMap.put("email", venda.getClienteEmail());
            vendaMap.put("valor", venda.getValorTotal());
            vendaMap.put("tipoPagamento", venda.getTipoPagamento());
            vendaMap.put("data", venda.getDataVenda());
            vendaMap.put("status", venda.getStatus());
            PagamentoService.adicionarVendaAoHistorico(vendaMap);
            
            System.out.println("\nDocumentos fiscais gerados e enviados para: " + cliente.getEmail());
        } else {
            System.out.println("Erro ao processar o pagamento. Por favor, tente novamente.");
        }

        carrinhoAtual = null; 
    }

    public static void listarLivros() {
        System.out.println("\n=== LIVROS DISPONÍVEIS ===");
        for (Livro l : livros) {
            System.out.printf("ID: %d - Título: %s - Valor: R$%.2f\n",
                l.getId(),
                l.getTitulo(),
                l.getValor());
        }
    }

    private static Livro buscarLivro(long id) {
        return livros.stream()
            .filter(l -> l.getId() == id)
            .findFirst()
            .orElse(null);
    }

    private static Usuario buscarUsuario(long id) {
        return usuarios.stream()
            .filter(u -> u.getId() == id)
            .findFirst()
            .orElse(null);
    }

    private static Fornecedor buscarFornecedor(long id) {
        return fornecedores.stream()
            .filter(f -> f.getId() == id)
            .findFirst()
            .orElse(null);
    }

    private static void inicializarDados() {
        // Primeiro, cria o usuário admin se não existir
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            // Busca pelo admin usando getResultList para evitar exceção
            List<Usuario> admins = em.createQuery("SELECT u FROM Usuario u WHERE u.nome = :nome", Usuario.class)
                    .setParameter("nome", "admin")
                    .getResultList();
            Usuario admin;
            if (admins.isEmpty()) {
                admin = new Usuario();
                admin.setNome("admin");
                admin.setLogin("admin");
                admin.setEmail("admin@biblioteca.com");
                admin.setCpf("00000000000");
                admin.setEndereco(""); // adiciona chamada a admin.setEndereco("")
                admin.setSenha(AuthenticationService.encryptPassword("admin123"));
                admin.setRole("ADMIN");
                admin.setStatus(true);
                em.persist(admin);
            } else {
                admin = admins.get(0);
                admin.setLogin("admin");
                admin.setEmail("admin@biblioteca.com");
                admin.setCpf("00000000000");
                admin.setEndereco(""); // adiciona chamada a admin.setEndereco("")
                admin.setSenha(AuthenticationService.encryptPassword("admin123"));
                admin.setRole("ADMIN");
                admin.setStatus(true);
            }
            em.getTransaction().commit();
            System.out.println("Usuário administrador garantido no banco! Login: admin | Senha: admin123");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Erro ao criar usuário admin: " + e.getMessage());
            e.printStackTrace();
        } finally {
            em.close();
        }

        // Agora inicializa os outros dados
        em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            if (em.createQuery("SELECT COUNT(l) FROM Livro l").getSingleResult().equals(0L)) {
                Livro livro1 = new Livro("Java: Como Programar");
                livro1.setValor(150.00);
                livro1.setQuantidadeEstoque(6);
                em.persist(livro1);
                livros.add(livro1);

                Livro livro2 = new Livro("Clean Code");
                livro2.setValor(120.00);
                livro2.setQuantidadeEstoque(4);
                em.persist(livro2);
                livros.add(livro2);

                Livro livro3 = new Livro("Padrões de Projeto");
                livro3.setValor(180.00);
                livro3.setQuantidadeEstoque(5);
                em.persist(livro3);
                livros.add(livro3);

                Usuario user1 = new Usuario("João Silva");
                em.persist(user1);
                usuarios.add(user1);

                Usuario user2 = new Usuario("Maria Santos");
                em.persist(user2);
                usuarios.add(user2);

                // Adiciona fornecedores
                Fornecedor forn1 = new Fornecedor("Livraria Central");
                forn1.setEmail("livrariaCentral@email.com");
                em.persist(forn1);
                fornecedores.add(forn1);

                Fornecedor forn2 = new Fornecedor("Distribuidora de Livros SA");
                forn2.setEmail("distribuidoraSA@email.com");
                em.persist(forn2);
                fornecedores.add(forn2);
            } else {
                // Atualiza a lista de livros do banco
                List<Livro> livrosBanco = em.createQuery("SELECT l FROM Livro l", Livro.class).getResultList();
                livros.clear();
                livros.addAll(livrosBanco);
                
                // Verifica e corrige valores zerados
                for (Livro livro : livros) {
                    Double valor = livro.getValor();
                    if (valor == null || valor.equals(0.0)) {
                        switch (livro.getTitulo()) {
                            case "Java: Como Programar":
                                livro.setValor(150.00);
                                break;
                            case "Clean Code":
                                livro.setValor(120.00);
                                break;
                            case "Padrões de Projeto":
                                livro.setValor(180.00);
                                break;
                            default:
                                livro.setValor(100.00); // Valor padrão para outros livros
                        }
                        em.merge(livro);
                    }
                }
                
                usuarios.addAll(em.createQuery("SELECT u FROM Usuario u", Usuario.class).getResultList());
                fornecedores.addAll(em.createQuery("SELECT f FROM Fornecedor f", Fornecedor.class).getResultList());
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("Erro ao inicializar dados: " + e.getMessage());
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public static void exibirDashboard() {
        DashboardUI.mostrarDashboard();
    }

    public static void gerenciarEmprestimos() {
        while (true) {
            System.out.println("\n=== GERENCIAR EMPRÉSTIMOS ===");
            System.out.println("1. Realizar Empréstimo");
            System.out.println("2. Devolver Livro");
            System.out.println("3. Listar Empréstimos Ativos");
            System.out.println("4. Voltar");
            System.out.print("Escolha uma opção: ");

            int opcao = scanner.nextInt();
            scanner.nextLine(); // Limpar o buffer

            switch (opcao) {
                case 1:
                    realizarEmprestimo();
                    break;
                case 2:
                    devolverLivro();
                    break;
                case 3:
                    listarEmprestimosAtivos();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Opção inválida!");
            }
        }
    }

    private static void realizarEmprestimo() {
        System.out.println("\n=== REALIZAR EMPRÉSTIMO ===");
        
        // Selecionar usuário do sistema (para controle interno)
        System.out.println("Digite o ID do usuário do sistema:");
        long usuarioId = scanner.nextInt();
        scanner.nextLine(); // Limpar buffer
        Usuario usuario = buscarUsuario(usuarioId);
        if (usuario == null) {
            System.out.println("Usuário não encontrado!");
            return;
        }

        if (!EmprestimoService.usuarioEmSituacaoRegular(usuario)) {
            System.out.println("Usuário possui pendências e não pode realizar novos empréstimos!");
            return;
        }

       
        System.out.println("\nLivros disponíveis:");
        listarLivros();
        System.out.print("Digite o ID do livro: ");
        long livroId = scanner.nextLong();
        scanner.nextLine(); // Limpar buffer (tava dando erro)
        Livro livro = buscarLivro(livroId);
        
        if (livro == null) {
            System.out.println("Livro não encontrado!");
            return;
        }

        double valorEmprestimo = livro.getValor() * 0.10;
        System.out.printf("\nValor do empréstimo: R$%.2f (10%% do valor do livro)\n", valorEmprestimo);
        
        System.out.println("\n=== DADOS DO CLIENTE ===");
        System.out.print("Nome do cliente: ");
        String nomeCliente = scanner.nextLine();
        
        String cpf;
        boolean cpfValido = false;
        do {
            System.out.print("CPF do cliente (apenas números): ");
            cpf = scanner.nextLine();
            try {
                if (!CpfValidator.isValid(cpf)) {
                    System.out.println("CPF inválido! Por favor, digite novamente.");
                } else {
                    cpfValido = true;
                }
            } catch (Exception e) {
                System.out.println("CPF inválido! Por favor, digite apenas números.");
            }
        } while (!cpfValido);
        
        Cliente clienteExistente = ClienteService.buscarPorCpf(cpf);
        if (clienteExistente != null) {
            System.out.println("\nCliente já cadastrado! Deseja atualizar os dados? (S/N)");
            String atualizar = scanner.nextLine();
            if (!atualizar.equalsIgnoreCase("S")) {
                nomeCliente = clienteExistente.getNome();
                System.out.println("Email: " + clienteExistente.getEmail());
                System.out.println("Endereço: " + clienteExistente.getEnderecoCompleto());
                
                processarEmprestimoEGerarNota(usuario, livro, valorEmprestimo, clienteExistente);
                return;
            }
        }
        
        System.out.print("Email do cliente: ");
        String email = scanner.nextLine();

        System.out.println("\n=== ENDEREÇO DO CLIENTE ===");
        System.out.print("Rua: ");
        String rua = scanner.nextLine();
        System.out.print("Número: ");
        String numero = scanner.nextLine();
        System.out.print("Complemento: ");
        String complemento = scanner.nextLine();
        System.out.print("Bairro: ");
        String bairro = scanner.nextLine();
        System.out.print("Cidade: ");
        String cidade = scanner.nextLine();
        System.out.print("Estado: ");
        String estado = scanner.nextLine();
        System.out.print("CEP: ");
        String cep = scanner.nextLine();

        // Salva ou atualiza os dados do cliente
        Cliente cliente = new Cliente(nomeCliente, cpf, email, rua, numero, 
            complemento, bairro, cidade, estado, cep);
        cliente = ClienteService.salvarCliente(cliente);
        
        // Prossegue com o empréstimo
        processarEmprestimoEGerarNota(usuario, livro, valorEmprestimo, cliente);
    }

    private static void processarEmprestimoEGerarNota(Usuario usuario, Livro livro, double valorEmprestimo, Cliente cliente) {
        // Processar pagamento
        System.out.println("\n=== FORMA DE PAGAMENTO ===");
        System.out.println("1. Cartão de Crédito");
        System.out.println("2. Boleto Bancário");
        System.out.print("Escolha a forma de pagamento (1 ou 2): ");
        int opcaoPagamento = scanner.nextInt();
        scanner.nextLine(); // Limpar buffer

        String tipoPagamento = opcaoPagamento == 1 ? "card" : "boleto";

        // Criar carrinho temporário para processar o pagamento
        Carrinho carrinhoTemp = new Carrinho(1L, usuario);
        // Cria uma cópia do livro para não afetar o original
        Livro livroTemp = new Livro(livro.getTitulo());
        livroTemp.setId(livro.getId());
        livroTemp.setValor(valorEmprestimo); // Já define o valor como o valor do empréstimo
        carrinhoTemp.adicionarLivro(livroTemp, 1);

        System.out.println("\nProcessando pagamento...");
        com.stripe.model.checkout.Session sessaoPagamento = PagamentoService.criarSessaoCheckout(carrinhoTemp, tipoPagamento, cliente.getCpf(), false);
        
        if (sessaoPagamento != null) {
            System.out.println("\n=== INFORMAÇÕES DO PAGAMENTO ===");
            System.out.println("Link para pagamento: " + sessaoPagamento.getUrl());
            System.out.println("\nPor favor, acesse o link acima para finalizar o pagamento.");
            System.out.println("Após a confirmação do pagamento, o empréstimo será registrado.");
            
            // Aguarda confirmação do pagamento (em uma implementação real, isso seria feito via webhook)
            System.out.print("\nPressione ENTER após realizar o pagamento para continuar...");
            scanner.nextLine();

            try {
                Emprestimo emprestimo = EmprestimoService.realizarEmprestimo(usuario, livro);
                System.out.println("\nEmpréstimo realizado com sucesso!");
                System.out.printf("Data de devolução prevista: %s\n", emprestimo.getDataPrevista());
                System.out.printf("Valor do empréstimo: R$%.2f\n", emprestimo.getValorEmprestimo());

                // Gerar nota fiscal do empréstimo
                Venda venda = new Venda();
                venda.setId(sessaoPagamento.getId());
                venda.setClienteNome(cliente.getNome());
                venda.setClienteCpf(cliente.getCpf());
                venda.setClienteEmail(cliente.getEmail());
                venda.setClienteEndereco(cliente.getEnderecoCompleto());
                venda.setValorTotal(valorEmprestimo);
                venda.setTipoPagamento(tipoPagamento);
                venda.setDataVenda(LocalDateTime.now());
                venda.setStatus("PAGO");
                
                // Adiciona o item do empréstimo
                ItemVenda item = new ItemVenda();
                item.setLivro(livro);
                item.setQuantidade(1);
                item.setValorUnitario(valorEmprestimo);
                item.setValorTotal(valorEmprestimo);
                venda.addItem(item);
                
                // Gera documentos fiscais
                DocumentoFiscalService.gerarNotaFiscal(venda);
                DocumentoFiscalService.gerarRecibo(venda);
                
                // Salva a venda no banco e no histórico
                PagamentoService.salvarVendaNoBanco(venda);
                
                HashMap<String, Object> vendaMap = new HashMap<>();
                vendaMap.put("sessionId", venda.getId());
                vendaMap.put("cliente", venda.getClienteNome());
                vendaMap.put("cpf", venda.getClienteCpf());
                vendaMap.put("email", venda.getClienteEmail());
                vendaMap.put("valor", venda.getValorTotal());
                vendaMap.put("tipoPagamento", venda.getTipoPagamento());
                vendaMap.put("data", venda.getDataVenda());
                vendaMap.put("status", venda.getStatus());
                PagamentoService.adicionarVendaAoHistorico(vendaMap);
                
                System.out.println("\nDocumentos fiscais gerados e enviados para: " + cliente.getEmail());
            } catch (RuntimeException e) {
                System.out.println("Erro ao realizar empréstimo: " + e.getMessage());
            }
        } else {
            System.out.println("Erro ao processar o pagamento. Por favor, tente novamente.");
        }
    }

    private static void devolverLivro() {
        System.out.println("\n=== DEVOLVER LIVRO ===");
        System.out.print("Digite o ID do empréstimo: ");
        long emprestimoId = scanner.nextLong();

        try {
            double multa = EmprestimoService.registrarDevolucao(emprestimoId);
            System.out.println("Devolução registrada com sucesso!");
            if (multa > 0) {
                System.out.printf("Multa por atraso: R$%.2f\n", multa);
            }
        } catch (RuntimeException e) {
            System.out.println("Erro ao registrar devolução: " + e.getMessage());
        }
    }

    private static void listarEmprestimosAtivos() {
        System.out.println("\n=== EMPRÉSTIMOS ATIVOS ===");
        List<Emprestimo> emprestimosAtivos = EmprestimoService.listarTodosEmprestimosAtivos();
        
        if (emprestimosAtivos.isEmpty()) {
            System.out.println("Não há empréstimos ativos.");
            return;
        }

        for (Emprestimo emp : emprestimosAtivos) {
            System.out.printf("\nID: %d\n", emp.getId());
            System.out.printf("Usuário: %s\n", emp.getUsuario().getNome());
            System.out.printf("Livro: %s\n", emp.getLivro().getTitulo());
            System.out.printf("Data Empréstimo: %s\n", emp.getDataEmprestimo());
            System.out.printf("Data Prevista Devolução: %s\n", emp.getDataPrevista());
            System.out.printf("Valor: R$%.2f\n", emp.getValorEmprestimo());
            
            double multa = emp.calcularMulta();
            if (multa > 0) {
                System.out.printf("Multa atual: R$%.2f\n", multa);
            }
        }
    }

    public static void gerenciarFornecedores() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== GERENCIAR FORNECEDORES ===");
            System.out.println("1. Listar fornecedores");
            System.out.println("2. Adicionar fornecedor");
            System.out.println("3. Remover fornecedor");
            System.out.println("4. Voltar");
            System.out.print("Escolha uma opção: ");
            int opcao = scanner.nextInt();
            scanner.nextLine(); // Limpar buffer

            if (opcao == 1) {
                List<Fornecedor> fornecedores = FornecedorService.listarFornecedores();
                System.out.println("\n--- Fornecedores ---");
                for (Fornecedor f : fornecedores) {
                    System.out.println("ID: " + f.getId() + " | Nome: " + f.getNome() + " | Email: " + f.getEmail());
                }
            } else if (opcao == 2) {
                System.out.print("Nome do fornecedor: ");
                String nome = scanner.nextLine();
                System.out.print("Email do fornecedor: ");
                String email = scanner.nextLine();
                FornecedorService.cadastrarFornecedor(nome, email);
                System.out.println("Fornecedor cadastrado!");
            } else if (opcao == 3) {
                System.out.print("ID do fornecedor para remover: ");
                long id = scanner.nextLong();
                boolean removido = FornecedorService.removerFornecedor(id);
                if (removido) {
                    System.out.println("Fornecedor removido!");
                } else {
                    System.out.println("Fornecedor não encontrado.");
                }
            } else if (opcao == 4) {
                break;
            } else {
                System.out.println("Opção inválida!");
            }
        }
    }

    public static void gerenciarUsuarios() {
        MenuStrategySelector.exibirMenuGerenciarUsuarios();
        MenuStrategySelector.gerenciarOpcoesGerenciarUsuarios();
    }

    public static void exibirListaUsuarios() {
        List<Usuario> usuarios = UsuarioService.listarUsuarios();
        System.out.println("\n===LISTA DE USUÁRIOS===");
        usuarios.forEach(Main::exibirUsuario);
    }

    public static void exibirUsuario(Usuario usuario) {
        if(Objects.isNull(usuario)) {
            System.out.println("Usuário Inexistente");
            return;
        }

        System.out.println();
        System.out.print("Nome: ");
        System.out.println(usuario.getNome());
        System.out.print("Login: ");
        System.out.println(usuario.getLogin());
        System.out.print("Email: ");
        System.out.println(usuario.getEmail());
        System.out.print("Role: ");
        System.out.println(usuario.getRole());
        System.out.print("Status: ");
        System.out.println(usuario.getStatus() ? "ATIVO" : "INATIVO");
    }

    public static void atualizarUsuarioRole() {
        System.out.println("\nLogin do usuário a ser atualizado: ");
        String login = scanner.next();
        System.out.println("\nNova role do usuário: ");
        String role = scanner.next().toUpperCase(Locale.ROOT);
        boolean success = UsuarioService.atualizarUsuarioRole(login, role);
        if (success) {
            System.out.println("Role atualizada com sucesso");
        } else {
            System.out.println("Erro ao atualizar. Role inválida.");
        }
    }
}