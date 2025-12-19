package com.biblioteca.service;

import com.biblioteca.model.Fornecedor;
import com.biblioteca.model.Livro;
import com.biblioteca.model.Usuario;
import com.biblioteca.util.JPAUtil;
import org.springframework.scheduling.annotation.Scheduled;
import com.biblioteca.repository.FornecedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class FornecedorService {

    // ===== DEPENDÊNCIAS WEB =====
    @Autowired(required = false)
    private FornecedorRepository fornecedorRepository;
    
    @Autowired(required = false)
    private AuditoriaService auditoriaService;
    
    @Autowired(required = false)
    private UsuarioService usuarioService;

    // ===== MÉTODOS WEB =====
    public Fornecedor salvarFornecedorWeb(String nome, String email){
        if(fornecedorRepository!=null){
            Fornecedor f = new Fornecedor(nome,email);
            Fornecedor fornecedorSalvo = fornecedorRepository.save(f);
            
            // Registrar auditoria de INSERT
            if (auditoriaService != null && usuarioService != null) {
                Usuario usuario = getCurrentUser();
                if (usuario != null) {
                    auditoriaService.createAuditoriaInsertWeb(fornecedorSalvo, usuario);
                }
            }
            
            return fornecedorSalvo;
        }
        return cadastrarFornecedor(nome,email);
    }

    public boolean atualizarFornecedorWeb(Long id, String nome, String email){
        if(fornecedorRepository!=null){
            return fornecedorRepository.findById(id).map(fornecedorAnterior->{
                // Criar cópia para auditoria
                Fornecedor fornecedorAnterioCopy = new Fornecedor();
                fornecedorAnterioCopy.setId(fornecedorAnterior.getId());
                fornecedorAnterioCopy.setNome(fornecedorAnterior.getNome());
                fornecedorAnterioCopy.setEmail(fornecedorAnterior.getEmail());
                
                fornecedorAnterior.setNome(nome);
                fornecedorAnterior.setEmail(email);
                Fornecedor fornecedorSalvo = fornecedorRepository.save(fornecedorAnterior);
                
                // Registrar auditoria de UPDATE
                if (auditoriaService != null && usuarioService != null) {
                    Usuario usuario = getCurrentUser();
                    if (usuario != null) {
                        auditoriaService.createAuditoriaUpdateWeb(fornecedorSalvo, fornecedorAnterioCopy, usuario);
                    }
                }
                
                return true;
            }).orElse(false);
        }
        return atualizarFornecedor(id, nome);
    }

    public boolean removerFornecedorWeb(Long id){
        if(fornecedorRepository!=null){
            Optional<Fornecedor> fornecedorOptional = fornecedorRepository.findById(id);
            if(fornecedorOptional.isPresent()){
                Fornecedor fornecedor = fornecedorOptional.get();
                
                // Registrar auditoria antes de deletar
                if (auditoriaService != null && usuarioService != null) {
                    Usuario usuario = getCurrentUser();
                    if (usuario != null) {
                        auditoriaService.createAuditoriaDeleteWeb(fornecedor, usuario);
                    }
                }
                
                fornecedorRepository.deleteById(id);
                return true;
            }
            return false;
        }
        return removerFornecedor(id);
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

    public List<Fornecedor> listarPaginado(int offset, int limit){
        if(fornecedorRepository!=null){
            int page = offset/limit;
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page,limit);
            return fornecedorRepository.findAll(pageable).getContent();
        }
        List<Fornecedor> todos = listarFornecedores();
        int to = Math.min(offset+limit, todos.size());
        if(offset>to) return java.util.Collections.emptyList();
        return todos.subList(offset,to);
    }

    public long contarFornecedores(){
        if(fornecedorRepository!=null) return fornecedorRepository.count();
        return listarFornecedores().size();
    }

    public static Fornecedor cadastrarFornecedor(String nome, String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Fornecedor fornecedor = new Fornecedor(nome, email);
            em.persist(fornecedor);
            em.getTransaction().commit();
            return fornecedor;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }


    public static Fornecedor buscarFornecedor(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Fornecedor.class, id);
        } finally {
            em.close();
        }
    }

    public List<Fornecedor> buscarPorNome(String nome){
        if(fornecedorRepository!=null){
            return fornecedorRepository.findByNomeContainingIgnoreCase(nome);
        }
        return buscarPorNomeConsole(nome);
    }

    // versão console
    public static List<Fornecedor> buscarPorNomeConsole(String nome) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT f FROM Fornecedor f WHERE LOWER(f.nome) LIKE LOWER(:nome)",
                Fornecedor.class)
                .setParameter("nome", "%" + nome + "%")
                .getResultList();
        } finally {
            em.close();
        }
    }

    public static Fornecedor buscarPorEmail(String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT f FROM Fornecedor f WHERE LOWER(f.email) LIKE LOWER(:email)",
                            Fornecedor.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public static boolean atualizarFornecedor(long id, String novoNome) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Fornecedor fornecedor = em.find(Fornecedor.class, id);
            if (fornecedor == null) {
                return false;
            }
            fornecedor.setNome(novoNome);
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

    public static boolean removerFornecedor(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Fornecedor fornecedor = em.find(Fornecedor.class, id);
            if (fornecedor == null) {
                return false;
            }
            em.remove(fornecedor);
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

    public static List<Fornecedor> listarFornecedores() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT f FROM Fornecedor f", Fornecedor.class)
                .getResultList();
        } finally {
            em.close();
        }
    }

    public static boolean existeFornecedor(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Fornecedor.class, id) != null;
        } finally {
            em.close();
        }
    }

    //Cron ativado a cada 30 minutos
    @Scheduled(cron = "0 0/30 * * * *")
    public void executarVarreduraEstoque() {
        List<Livro> livros = LivroService.listarLivros();

        List<Livro> livrosParaReposicao = livros.stream()
                .filter(livro -> livro.getQuantidadeEstoque() < 5)
                .collect(Collectors.toList());

        Fornecedor fornecedor = listarFornecedores().stream()
                .filter(f -> f.getEmail() != null)
                .findFirst()
                .orElse(new Fornecedor());

        EmailService.enviarEmailDeReposicaoDeEstoque(livrosParaReposicao, fornecedor);
    }
} 