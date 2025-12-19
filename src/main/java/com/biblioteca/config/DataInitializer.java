package com.biblioteca.config;

import com.biblioteca.model.Livro;
import com.biblioteca.model.Usuario;
import com.biblioteca.service.LivroService;
import com.biblioteca.service.UsuarioService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Inicializa dados mínimos para testes/avaliação.
 */
@Component
@Profile("prod")
public class DataInitializer {

    @Autowired(required = false)
    private UsuarioService usuarioService;

    @Autowired(required = false)
    private LivroService livroService;

    @PostConstruct
    public void init(){
        System.out.println("=== INICIANDO DATA INITIALIZER ===");
        
        if(usuarioService==null || livroService==null) {
            System.out.println("❌ Services não injetados! UsuarioService: " + usuarioService + ", LivroService: " + livroService);
            return;
        }
        
        long usuariosExistentes = usuarioService.contarUsuarios();
        System.out.println("Usuários existentes no banco: " + usuariosExistentes);
        
        if(usuariosExistentes > 0) {
            System.out.println("✅ Dados já existem no banco - pulando inicialização");
            
            // Mesmo que já existam usuários, vamos verificar se os livros estão lá
            long livrosExistentes = livroService.contarLivros();
            System.out.println("Livros existentes no banco: " + livrosExistentes);
            
            if (livrosExistentes == 0) {
                System.out.println("📚 Adicionando livros que estão faltando...");
                adicionarLivros();
            }
            return; // já populado
        }

        // Usuários
        Usuario admin = new Usuario();
        admin.setNome("Admin");
        admin.setLogin("admin");
        admin.setEmail("admin@demo.com");
        admin.setSenha("admin");
        admin.setRole("ADMIN");
        usuarioService.cadastrarUsuarioWeb(admin);

        Usuario gerente = new Usuario();
        gerente.setNome("Gerente Demo");
        gerente.setLogin("gerente");
        gerente.setEmail("gerente@demo.com");
        gerente.setSenha("gerente");
        gerente.setRole("GERENTE");
        usuarioService.cadastrarUsuarioWeb(gerente);

        Usuario func = new Usuario();
        func.setNome("Funcionario Demo");
        func.setLogin("func");
        func.setEmail("func@demo.com");
        func.setSenha("func");
        func.setRole("FUNCIONARIO");
        usuarioService.cadastrarUsuarioWeb(func);

        Usuario cliente = new Usuario();
        cliente.setNome("Cliente Demo");
        cliente.setLogin("cliente");
        cliente.setEmail("cliente@demo.com");
        cliente.setSenha("cliente");
        cliente.setRole("CLIENTE");
        usuarioService.cadastrarUsuarioWeb(cliente);

        // Livros
        adicionarLivros();

        System.out.println("📚 Dados demo inseridos.");
    }
    
    private void adicionarLivros() {
        System.out.println("📚 Adicionando livros ao banco...");
        
        // Criar livros com ISBN e autor
        Livro domCasmurro = new Livro();
        domCasmurro.setTitulo("Dom Casmurro");
        domCasmurro.setAutor("Machado de Assis");
        domCasmurro.setIsbn("978-85-359-0277-5");
        domCasmurro.setValor(50.0);
        domCasmurro.setQuantidadeEstoque(10);
        livroService.salvarLivro(domCasmurro);
        
        Livro mil984 = new Livro();
        mil984.setTitulo("1984");
        mil984.setAutor("George Orwell");
        mil984.setIsbn("978-0-452-28423-4");
        mil984.setValor(45.0);
        mil984.setQuantidadeEstoque(4); // <5 para testar alerta
        livroService.salvarLivro(mil984);
        
        Livro cleanCode = new Livro();
        cleanCode.setTitulo("Clean Code");
        cleanCode.setAutor("Robert C. Martin");
        cleanCode.setIsbn("978-0-13-235088-4");
        cleanCode.setValor(120.0);
        cleanCode.setQuantidadeEstoque(8);
        livroService.salvarLivro(cleanCode);
        
        Livro alienista = new Livro();
        alienista.setTitulo("O Alienista");
        alienista.setAutor("Machado de Assis");
        alienista.setIsbn("978-85-254-2965-4");
        alienista.setValor(30.0);
        alienista.setQuantidadeEstoque(2); // alerta
        livroService.salvarLivro(alienista);
        
        Livro senhorAneis = new Livro();
        senhorAneis.setTitulo("O Senhor dos Anéis");
        senhorAneis.setAutor("J.R.R. Tolkien");
        senhorAneis.setIsbn("978-85-336-2562-4");
        senhorAneis.setValor(90.0);
        senhorAneis.setQuantidadeEstoque(12);
        livroService.salvarLivro(senhorAneis);
        
        System.out.println("✅ Livros adicionados com sucesso!");
    }
} 