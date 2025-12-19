package com.biblioteca.strategy;

import com.biblioteca.model.Usuario;
import com.biblioteca.service.UsuarioService;

import java.util.Objects;
import java.util.Scanner;

import static com.biblioteca.Main.*;

public class MenuStrategySelector {

    public static void exibirMenu(Usuario usuario) {
        if(Objects.isNull(usuario)) {
            exibirMenuLogin();
            return;
        }

        switch (usuario.getRole()) {
            case "ADMIN":
                exibirMenuAdmin();
                break;
            case "GERENTE":
                exibirMenuGerente();
                break;
            case "FUNCIONARIO":
                exibirMenuFuncionario();
                break;
            case "CLIENTE":
                exibirMenuCliente();
                break;
            default:
                System.out.println("Role inválida");
        }
    }

    public static void gerenciarOpcoes(Usuario usuario) {
        if(Objects.isNull(usuario)) {
            gerenciarOpcoesLogin();
            return;
        }

        switch (usuario.getRole()) {
            case "ADMIN":
                gerenciarOpcoesAdmin();
                break;
            case "GERENTE":
                gerenciarOpcoesGerente();
                break;
            case "FUNCIONARIO":
                gerenciarOpcoesFuncionario();
                break;
            case "CLIENTE":
                gerenciarOpcoesCliente();
                break;
            default:
                System.out.println("Role inválida");
        }
    }

    private static void exibirMenuLogin() {
        System.out.println("\n=== BEM-VINDO AO SISTEMA DE BIBLIOTECA ===");
        System.out.println("1. Fazer Login");
        System.out.println("2. Registrar-se");
        System.out.println("3. Recuperar Senha");
        System.out.println("4. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private static void exibirMenuAdmin() {
        System.out.println("\n=== MENU ADMIN ===");
        System.out.println("0. Gerenciar Usuários");
        System.out.println("1. Gerenciar Carrinho");
        System.out.println("2. Realizar Compra");
        System.out.println("3. Listar Livros");
        System.out.println("4. Dashboard de Vendas");
        System.out.println("5. Gerenciar Empréstimos");
        System.out.println("6. Gerenciar Fornecedores");
        System.out.println("7. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private static void exibirMenuGerente() {
        System.out.println("\n=== MENU GERENTE ===");
        System.out.println("1. Listar Livros");
        System.out.println("2. Dashboard de Vendas");
        System.out.println("3. Gerenciar Empréstimos");
        System.out.println("4. Gerenciar Fornecedores");
        System.out.println("5. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private static void exibirMenuFuncionario() {
        System.out.println("\n=== MENU FUNCIONARIO ===");
        System.out.println("1. Listar Livros");
        System.out.println("2. Gerenciar Empréstimos");
        System.out.println("3. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private static void exibirMenuCliente() {
        System.out.println("\n=== MENU CLIENTE ===");
        System.out.println("1. Gerenciar Carrinho");
        System.out.println("2. Realizar Compra");
        System.out.println("3. Listar Livros");
        System.out.println("4. Sair");
        System.out.print("Escolha uma opção: ");
    }

    public static void exibirMenuGerenciarUsuarios() {
        System.out.println("\n=== GERENCIAR USUARIOS ===");
        System.out.println("1. Listar todos os usuários");
        System.out.println("2. Excluir usuário");
        System.out.println("3. Buscar usuário");
        System.out.println("4. Alterar role de um usuário");
        System.out.println("5. Voltar");
        System.out.print("Escolha uma opção: ");
    }

    private static void gerenciarOpcoesLogin() {
        Scanner scanner = new Scanner(System.in);
        int opcao = scanner.nextInt();
        scanner.nextLine(); // Limpar o buffer

        switch (opcao) {
            case 1:
                efetuarLogin();
                break;
            case 2:
                registrarUsuario();
                break;
            case 3:
                recuperarSenha();
                break;
            case 4:
                deslogar();
                System.out.println("\nSaindo do sistema...");
                return;
            default:
                System.out.println("Opção inválida!");
        }
    }

    private static void gerenciarOpcoesCliente() {
        Scanner scanner = new Scanner(System.in);
        int opcao = scanner.nextInt();
        scanner.nextLine(); // Limpar o buffer

        switch (opcao) {
            case 1:
                gerenciarCarrinho();
                break;
            case 2:
                realizarCompra();
                break;
            case 3:
                listarLivros();
                break;
            case 4:
                deslogar();
                System.out.println("\nSaindo do sistema...");
                return;
            default:
                System.out.println("Opção inválida!");
        }
    }

    private static void gerenciarOpcoesFuncionario() {
        Scanner scanner = new Scanner(System.in);
        int opcao = scanner.nextInt();
        scanner.nextLine(); // Limpar o buffer

        switch (opcao) {
            case 1:
                listarLivros();
                break;
            case 2:
                gerenciarEmprestimos();
                break;
            case 3:
                deslogar();
                System.out.println("\nSaindo do sistema...");
                return;
            default:
                System.out.println("Opção inválida!");
        }
    }

    private static void gerenciarOpcoesGerente() {
        Scanner scanner = new Scanner(System.in);
        int opcao = scanner.nextInt();
        scanner.nextLine(); // Limpar o buffer

        switch (opcao) {
            case 1:
                listarLivros();
                break;
            case 2:
                exibirDashboard();
                break;
            case 3:
                gerenciarEmprestimos();
                break;
            case 4:
                gerenciarFornecedores();
                break;
            case 5:
                deslogar();
                System.out.println("\nSaindo do sistema...");
                return;
            default:
                System.out.println("Opção inválida!");
        }
    }

    private static void gerenciarOpcoesAdmin() {
        Scanner scanner = new Scanner(System.in);
        int opcao = scanner.nextInt();
        scanner.nextLine(); // Limpar o buffer

        switch (opcao) {
            case 0:
                gerenciarUsuarios();
                break;
            case 1:
                gerenciarCarrinho();
                break;
            case 2:
                realizarCompra();
                break;
            case 3:
                listarLivros();
                break;
            case 4:
                exibirDashboard();
                break;
            case 5:
                gerenciarEmprestimos();
                break;
            case 6:
                gerenciarFornecedores();
                break;
            case 7:
                deslogar();
                System.out.println("\nSaindo do sistema...");
                return;
            default:
                System.out.println("Opção inválida!");
        }
    }

    public static void gerenciarOpcoesGerenciarUsuarios() {
        Scanner scanner = new Scanner(System.in);
        int opcao = scanner.nextInt();
        scanner.nextLine(); // Limpar o buffer

        switch (opcao) {
            case 1:
                exibirListaUsuarios();
                break;
            case 2:
                System.out.println("\nLogin do usuário a ser removido: ");
                UsuarioService.removerUsuario(scanner.next());
                break;
            case 3:
                System.out.println("\nInsira o login do usuário a ser buscado: ");
                Usuario usuario = UsuarioService.buscarUsuario(scanner.next());
                System.out.println("===RESULTADO DA BUSCA===");
                exibirUsuario(usuario);
                break;
            case 4:
                atualizarUsuarioRole();
                break;
            case 5:
                deslogar();
                System.out.println("\nSaindo do sistema...");
                return;
            default:
                System.out.println("Opção inválida!");
        }
    }

    private static void deslogar() {
        usuarioAtivo = null;
    }
}
