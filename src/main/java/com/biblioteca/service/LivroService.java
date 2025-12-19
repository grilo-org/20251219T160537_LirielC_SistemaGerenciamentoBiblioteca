package com.biblioteca.service;

import com.biblioteca.model.Livro;
import com.biblioteca.model.Usuario;
import com.biblioteca.repository.LivroRepository;
import com.biblioteca.util.JPAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.biblioteca.util.IsbnValidator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Service para gerenciamento de livros.
 * Suporta tanto operações via Spring Boot (web) quanto console (legacy).
 */
@Service
@Transactional
public class LivroService {
    
    @Autowired(required = false)
    private LivroRepository livroRepository;
    
    @Autowired(required = false)
    private AuditoriaService auditoriaService;
    
    @Autowired(required = false)
    private UsuarioService usuarioService;
    
    // ========== MÉTODOS WEB (Spring Data JPA) ==========
    
    /**
     * Adiciona um novo livro (versão web)
     */
    public Livro salvarLivro(String titulo, double valor, Integer quantidade) {
        Livro livro = new Livro(titulo);
        livro.setValor(valor);
        livro.setQuantidadeEstoque(quantidade);
        return salvarLivro(livro);
    }

    public Livro salvarLivro(Livro livro){
        if(livroRepository!=null){
            // Validar ISBN se fornecido
            String isbn = livro.getIsbn();
            if (isbn != null && !isbn.trim().isEmpty()) {
                if (!IsbnValidator.isValidIsbn(isbn)) {
                    throw new IllegalArgumentException("ISBN inválido: " + isbn);
                }
                // Verificar se ISBN já existe (apenas para novos livros)
                if (livro.getId() == null && livroRepository.existsByIsbn(IsbnValidator.cleanIsbn(isbn))) {
                    throw new IllegalArgumentException("ISBN já existe: " + isbn);
                }
                // Limpar e armazenar ISBN sem formatação
                livro.setIsbn(IsbnValidator.cleanIsbn(isbn));
            }
            
            boolean isNew = livro.getId() == null;
            Livro savedLivro = livroRepository.save(livro);
            
            // Registrar auditoria
            if (auditoriaService != null && usuarioService != null) {
                Usuario usuario = getCurrentUser();
                if (usuario != null) {
                    if (isNew) {
                        auditoriaService.createAuditoriaInsertWeb(savedLivro, usuario);
                    } else {
                        // Para updates, buscar o livro anterior
                        Optional<Livro> livroAnterior = livroRepository.findById(livro.getId());
                        if (livroAnterior.isPresent()) {
                            auditoriaService.createAuditoriaUpdateWeb(savedLivro, livroAnterior.get(), usuario);
                        }
                    }
                }
            }
            
            return savedLivro;
        }
        // console fallback
        Livro novo = adicionarLivro(livro.getTitulo(), livro.getValor());
        novo.setQuantidadeEstoque(livro.getQuantidadeEstoque());
        return novo;
    }
    
    /**
     * Busca livro por ID (versão web)
     */
    public Optional<Livro> buscarPorId(Long id) {
        if (livroRepository != null) {
            return livroRepository.findById(id);
        }
        // Fallback para versão console
        Livro livro = buscarLivro(id);
        return Optional.ofNullable(livro);
    }
    
    /**
     * Lista todos os livros (versão web)
     */
    public List<Livro> listarTodos() {
        if (livroRepository != null) {
            return livroRepository.findAll();
        }
        // Fallback para versão console
        return listarLivros();
    }
    
    /**
     * Busca livros por título (versão web)
     */
    public List<Livro> buscarPorTituloWeb(String titulo) {
        if (livroRepository != null) {
            return livroRepository.findByTituloContainingIgnoreCase(titulo);
        }
        // Fallback para versão console
        return buscarPorTitulo(titulo);
    }
    
    /**
     * Busca livros por autor (versão web)
     */
    public List<Livro> buscarPorAutorWeb(String autor) {
        if (livroRepository != null) {
            return livroRepository.findByAutorContainingIgnoreCase(autor);
        }
        return java.util.Collections.emptyList();
    }
    
    /**
     * Busca livro por ISBN (versão web)
     */
    public Optional<Livro> buscarPorIsbnWeb(String isbn) {
        if (livroRepository != null && isbn != null) {
            String cleanIsbn = IsbnValidator.cleanIsbn(isbn);
            List<Livro> livros = livroRepository.findByIsbn(cleanIsbn);
            return livros.isEmpty() ? Optional.empty() : Optional.of(livros.get(0));
        }
        return Optional.empty();
    }
    
    /**
     * Busca livros com múltiplos critérios
     */
    public List<Livro> buscarPorMultiplosCriterios(String titulo, String autor, String isbn) {
        if (livroRepository != null) {
            String cleanIsbn = (isbn != null && !isbn.trim().isEmpty()) ? IsbnValidator.cleanIsbn(isbn) : null;
            return livroRepository.findByMultipleCriteria(titulo, autor, cleanIsbn);
        }
        return java.util.Collections.emptyList();
    }
    
    /**
     * Filtra livros por faixa de preço (versão web)
     */
    public List<Livro> filtrarPorPrecoWeb(double precoMinimo, double precoMaximo) {
        if (livroRepository != null) {
            return livroRepository.findByPrecoBetween(precoMinimo, precoMaximo);
        }
        // Fallback para versão console
        return filtrarPorPreco(precoMinimo, precoMaximo);
    }
    
    /**
     * Atualiza livro (versão web)
     */
    public Optional<Livro> atualizarLivroWeb(Long id, String novoTitulo, double novoValor, Integer novaQtd) {
        if (livroRepository != null) {
            return livroRepository.findById(id)
                .map(livroAnterior -> {
                    Livro livroAtualizado = new Livro();
                    livroAtualizado.setId(livroAnterior.getId());
                    livroAtualizado.setTitulo(novoTitulo);
                    livroAtualizado.setValor(novoValor);
                    livroAtualizado.setQuantidadeEstoque(novaQtd);
                    livroAtualizado.setAutor(livroAnterior.getAutor());
                    livroAtualizado.setIsbn(livroAnterior.getIsbn());
                    livroAtualizado.setUrlImagem(livroAnterior.getUrlImagem());
                    
                    Livro savedLivro = livroRepository.save(livroAtualizado);
                    
                    // Registrar auditoria
                    if (auditoriaService != null && usuarioService != null) {
                        Usuario usuario = getCurrentUser();
                        if (usuario != null) {
                            auditoriaService.createAuditoriaUpdateWeb(savedLivro, livroAnterior, usuario);
                        }
                    }
                    
                    return savedLivro;
                });
        }
        // Fallback para versão console
        boolean atualizado = atualizarLivro(id, novoTitulo, novoValor);
        return atualizado ? buscarPorId(id) : Optional.empty();
    }
    
    /**
     * Atualiza livro completo incluindo URL da imagem, ISBN e autor (versão web)
     */
    public Optional<Livro> atualizarLivroCompletoWeb(Livro livroAtualizado) {
        if (livroRepository != null) {
            // Validar ISBN se fornecido
            String isbn = livroAtualizado.getIsbn();
            if (isbn != null && !isbn.trim().isEmpty()) {
                if (!IsbnValidator.isValidIsbn(isbn)) {
                    throw new IllegalArgumentException("ISBN inválido: " + isbn);
                }
                // Verificar se ISBN já existe em outro livro
                if (livroRepository.existsByIsbnAndIdNot(IsbnValidator.cleanIsbn(isbn), livroAtualizado.getId())) {
                    throw new IllegalArgumentException("ISBN já existe para outro livro: " + isbn);
                }
            }
            
            return livroRepository.findById(livroAtualizado.getId())
                .map(livroAnterior -> {
                    // Criar cópia para auditoria
                    Livro livroAnterioCopy = new Livro();
                    livroAnterioCopy.setId(livroAnterior.getId());
                    livroAnterioCopy.setTitulo(livroAnterior.getTitulo());
                    livroAnterioCopy.setAutor(livroAnterior.getAutor());
                    livroAnterioCopy.setIsbn(livroAnterior.getIsbn());
                    livroAnterioCopy.setValor(livroAnterior.getValor());
                    livroAnterioCopy.setQuantidadeEstoque(livroAnterior.getQuantidadeEstoque());
                    livroAnterioCopy.setUrlImagem(livroAnterior.getUrlImagem());
                    
                    livroAnterior.setTitulo(livroAtualizado.getTitulo());
                    livroAnterior.setAutor(livroAtualizado.getAutor());
                    livroAnterior.setIsbn(isbn != null ? IsbnValidator.cleanIsbn(isbn) : null);
                    livroAnterior.setValor(livroAtualizado.getValor());
                    livroAnterior.setQuantidadeEstoque(livroAtualizado.getQuantidadeEstoque());
                    livroAnterior.setUrlImagem(livroAtualizado.getUrlImagem());
                    
                    Livro savedLivro = livroRepository.save(livroAnterior);
                    
                    // Registrar auditoria
                    if (auditoriaService != null && usuarioService != null) {
                        Usuario usuario = getCurrentUser();
                        if (usuario != null) {
                            auditoriaService.createAuditoriaUpdateWeb(savedLivro, livroAnterioCopy, usuario);
                        }
                    }
                    
                    return savedLivro;
                });
        }
        return Optional.empty();
    }
    
    /**
     * Remove livro (versão web)
     */
    public boolean removerLivroWeb(Long id) {
        if (livroRepository != null) {
            Optional<Livro> livroOptional = livroRepository.findById(id);
            if (livroOptional.isPresent()) {
                Livro livro = livroOptional.get();
                
                // Registrar auditoria antes de deletar
                if (auditoriaService != null && usuarioService != null) {
                    Usuario usuario = getCurrentUser();
                    if (usuario != null) {
                        auditoriaService.createAuditoriaDeleteWeb(livro, usuario);
                    }
                }
                
                livroRepository.deleteById(id);
                return true;
            }
            return false;
        }
        // Fallback para versão console
        return removerLivro(id);
    }
    
    /**
     * Conta total de livros (versão web)
     */
    public long contarLivros() {
        if (livroRepository != null) {
            return livroRepository.count();
        }
        // Fallback para versão console
        return listarLivros().size();
    }
    
    /**
     * Conta livros disponíveis (não emprestados) (versão web)
     */
    public long contarLivrosDisponiveis() {
        if (livroRepository != null) {
            return livroRepository.countAvailableBooks();
        }
        // Fallback para versão console - assume que todos estão disponíveis
        return listarLivros().size();
    }
    
    /**
     * Lista livros paginados (versão web)
     */
    public List<Livro> listarPaginado(int offset, int limit) {
        if (livroRepository != null) {
            int page = offset / limit;
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, limit);
            return livroRepository.findAll(pageable).getContent();
        }
        List<Livro> todos = listarLivros();
        int to = Math.min(offset + limit, todos.size());
        if (offset > to) return java.util.Collections.emptyList();
        return todos.subList(offset, to);
    }

    /**
     * Obtém o usuário atual da sessão
     */
    private Usuario getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return usuarioService.buscarUsuarioByLoginWeb(auth.getName()).orElse(null);
            }
        } catch (Exception e) {
            // Ignorar erros de contexto de segurança
        }
        return null;
    }
    
    // ========== MÉTODOS CONSOLE (Legacy - JPA Util) ==========
    
    public static Livro adicionarLivro(String titulo, double valor) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Livro livro = new Livro(titulo);
            livro.setValor(valor);
            em.persist(livro);
            em.getTransaction().commit();
            return livro;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public static Livro buscarLivro(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Livro.class, id);
        } finally {
            em.close();
        }
    }

    public static List<Livro> buscarPorTitulo(String titulo) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT l FROM Livro l WHERE LOWER(l.titulo) LIKE LOWER(:titulo)",
                Livro.class)
                .setParameter("titulo", "%" + titulo + "%")
                .getResultList();
        } finally {
            em.close();
        }
    }

    public static boolean atualizarLivro(long id, String novoTitulo, double novoValor) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Livro livro = em.find(Livro.class, id);
            if (livro == null) {
                return false;
            }
            livro.setTitulo(novoTitulo);
            livro.setValor(novoValor);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return false;
        } finally {
            em.close();
        }
    }

    public static boolean removerLivro(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Livro livro = em.find(Livro.class, id);
            if (livro == null) {
                return false;
            }
            em.remove(livro);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return false;
        } finally {
            em.close();
        }
    }

    public static List<Livro> listarLivros() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT l FROM Livro l", Livro.class)
                .getResultList();
        } finally {
            em.close();
        }
    }

    public static boolean existeLivro(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Livro.class, id) != null;
        } finally {
            em.close();
        }
    }

    public static List<Livro> filtrarPorPreco(double precoMinimo, double precoMaximo) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT l FROM Livro l WHERE l.valor BETWEEN :min AND :max",
                Livro.class)
                .setParameter("min", precoMinimo)
                .setParameter("max", precoMaximo)
                .getResultList();
        } finally {
            em.close();
        }
    }
} 