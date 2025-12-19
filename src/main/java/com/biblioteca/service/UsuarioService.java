package com.biblioteca.service;

import com.biblioteca.model.Usuario;
import com.biblioteca.repository.UsuarioRepository;
import com.biblioteca.util.JPAUtil;
import com.biblioteca.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service para gerenciamento de usuários.
 * 
 * COMPATIBILIDADE DUAL:
 * - Web: Usa Spring Data JPA (@Autowired repository)
 * - Console: Usa JPAUtil tradicional (métodos estáticos mantidos)
 * 
 * A lógica de negócio é compartilhada entre as duas versões.
 */
@Service
@Transactional
public class UsuarioService {
    
    // ========== DEPENDÊNCIAS SPRING (VERSÃO WEB) ==========
    @Autowired(required = false) // required=false para compatibilidade console
    private UsuarioRepository usuarioRepository;
    
    @Autowired(required = false)
    private AuditoriaService auditoriaService;
    
    @Autowired(required = false)
    private AuthenticationService authenticationService;
    
    // ========== MÉTODOS WEB (SPRING DATA JPA) ==========
    
    /**
     * Cadastra novo usuário usando Spring Data JPA (versão web)
     */
    public Usuario cadastrarUsuarioWeb(Usuario usuario) {
        if (usuarioRepository == null) {
            // Fallback para versão console
            return cadastrarUsuario(usuario);
        }
        
        // Validações de negócio
        validarUsuarioUnico(usuario);
        
        // Criptografa a senha antes de salvar
        if (usuario.getSenha() != null && !usuario.getSenha().startsWith("$2a$")) {
            usuario.setSenha(AuthenticationService.encryptPassword(usuario.getSenha()));
        }
        
        usuario.setStatus(Boolean.TRUE);
        return usuarioRepository.save(usuario);
    }
    
    /**
     * Valida se login, email e CPF são únicos no sistema
     */
    private void validarUsuarioUnico(Usuario usuario) {
        // Valida login único
        if (usuario.getLogin() != null) {
            Optional<Usuario> existentePorLogin = usuarioRepository.findByLogin(usuario.getLogin());
            if (existentePorLogin.isPresent() && !existentePorLogin.get().getId().equals(usuario.getId())) {
                throw new IllegalArgumentException("Já existe um usuário com este login: " + usuario.getLogin());
            }
        }
        
        // Valida email único
        if (usuario.getEmail() != null) {
            Optional<Usuario> existentePorEmail = usuarioRepository.findByEmail(usuario.getEmail());
            if (existentePorEmail.isPresent() && !existentePorEmail.get().getId().equals(usuario.getId())) {
                throw new IllegalArgumentException("Já existe um usuário com este e-mail: " + usuario.getEmail());
            }
        }
        
        // Valida CPF único
        if (usuario.getCpf() != null && !usuario.getCpf().trim().isEmpty()) {
            String cpfLimpo = usuario.getCpf().replaceAll("[^0-9]", "");
            Optional<Usuario> existentePorCpf = usuarioRepository.findByCpf(cpfLimpo);
            if (existentePorCpf.isPresent() && !existentePorCpf.get().getId().equals(usuario.getId())) {
                throw new IllegalArgumentException("Já existe um usuário com este CPF: " + usuario.getCpf());
            }
        }
    }
    
    /**
     * Busca usuário por ID usando Spring Data JPA (versão web)
     */
    public Optional<Usuario> buscarUsuarioWeb(Long id) {
        if (usuarioRepository == null) {
            // Fallback para versão console
            Usuario usuario = buscarUsuario(id);
            return Optional.ofNullable(usuario);
        }
        
        return usuarioRepository.findById(id);
    }
    
    /**
     * Busca usuário por login usando Spring Data JPA (versão web)
     */
    public Optional<Usuario> buscarUsuarioByLoginWeb(String login) {
        if (usuarioRepository == null) {
            // Fallback para versão console
            Usuario usuario = buscarUsuario(login);
            return Optional.ofNullable(usuario);
        }
        
        return usuarioRepository.findByLogin(login);
    }

    /**
     * Busca usuário por CPF usando Spring Data JPA (versão web)
     */
    public Optional<Usuario> buscarUsuarioPorCpf(String cpf) {
        if (usuarioRepository == null) {
            return Optional.empty();
        }
        
        return usuarioRepository.findByCpf(cpf);
    }
    
    /**
     * Lista todos os usuários usando Spring Data JPA (versão web)
     */
    public List<Usuario> listarUsuariosWeb() {
        if (usuarioRepository == null) {
            // Fallback para versão console
            return listarUsuarios();
        }
        
        return usuarioRepository.findAll();
    }
    
    /**
     * Lista usuários ativos ordenados por nome (versão web)
     */
    public List<Usuario> listarUsuariosAtivosWeb() {
        if (usuarioRepository == null) {
            // Fallback para versão console
            return listarUsuarios(); // TODO: filtrar ativos na versão console
        }
        
        return usuarioRepository.findActiveUsersOrderByName();
    }
    
    /**
     * Atualiza role do usuário usando Spring Data JPA (versão web)
     */
    public boolean atualizarUsuarioRoleWeb(String login, String role) {
        if (usuarioRepository == null) {
            // Fallback para versão console
            return atualizarUsuarioRole(login, role);
        }
        
        List<String> rolesPermitidas = Arrays.asList("ADMIN", "GERENTE", "FUNCIONARIO", "CLIENTE");
        if (!rolesPermitidas.contains(role)) {
            return false;
        }
        
        Optional<Usuario> optUsuario = usuarioRepository.findByLogin(login);
        if (optUsuario.isEmpty()) {
            return false;
        }
        
        Usuario usuario = optUsuario.get();
        Usuario usuarioAntigo = new Usuario(usuario);
        usuario.setRole(role);
        
        usuarioRepository.save(usuario);
        
        // Auditoria se disponível
        if (auditoriaService != null) {
            auditoriaService.createAuditoriaUpdateWeb(usuario, usuarioAntigo, usuario);
        }
        
        return true;
    }
    
    /**
     * Conta total de usuários (versão web)
     */
    public long contarUsuarios() {
        if (usuarioRepository != null) {
            return usuarioRepository.count();
        }
        // Fallback para versão console
        return listarUsuarios().size();
    }
    
    /**
     * Lista usuários paginados.
     */
    public List<Usuario> listarPaginado(int offset, int limit) {
        if (usuarioRepository != null) {
            int page = offset / limit;
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, limit);
            return usuarioRepository.findAll(pageable).getContent();
        }
        List<Usuario> todos = listarUsuarios();
        int to = Math.min(offset + limit, todos.size());
        if (offset > to) return java.util.Collections.emptyList();
        return todos.subList(offset, to);
    }
    
    /**
     * Atualiza senha do usuário com criptografia (versão web)
     */
    public boolean atualizarSenhaWeb(String login, String novaSenha) {
        if (usuarioRepository == null) {
            // Fallback para versão console
            atualizarSenha(login, AuthenticationService.encryptPassword(novaSenha));
            return true;
        }
        
        Optional<Usuario> optUsuario = usuarioRepository.findByLogin(login);
        if (optUsuario.isEmpty()) {
            return false;
        }
        
        Usuario usuario = optUsuario.get();
        usuario.setSenha(AuthenticationService.encryptPassword(novaSenha));
        usuarioRepository.save(usuario);
        return true;
    }
    
    /**
     * Verifica se um email existe no sistema (versão web)
     */
    public boolean existeEmail(String email) {
        if (usuarioRepository == null) {
            // Fallback para versão console
            return buscarUsuarioByEmail(email) != null;
        }
        
        return usuarioRepository.findByEmail(email).isPresent();
    }
    
    /**
     * Busca usuário por token de recuperação (versão web)
     */
    public Usuario buscarUsuarioPorTokenRecuperacao(String token) {
        if (usuarioRepository == null) {
            // Fallback para versão console
            return buscarUsuarioByTokenRecuperacao(token);
        }
        
        return usuarioRepository.findByTokenRecuperacao(token).orElse(null);
    }
    
    /**
     * Limpa token de recuperação do usuário (versão web)
     */
    public void limparTokenRecuperacao(String email) {
        if (usuarioRepository == null) {
            // Fallback para versão console
            atualizarTokenRecuperacao(email, null);
            return;
        }
        
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            usuario.setTokenRecuperacao(null);
            usuarioRepository.save(usuario);
        });
    }
    
    public void atualizarTokenRecuperacaoWeb(String email, String token) {
        if (usuarioRepository == null) {
            // Fallback para versão console
            atualizarTokenRecuperacao(email, token);
            return;
        }

        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            usuario.setTokenRecuperacao(token);
            usuarioRepository.save(usuario);
        });
    }
    
    /**
     * Atualiza e-mail, CPF e outros dados do usuário logado (versão web).
     */
    public boolean atualizarDadosContaWeb(String login, String novoEmail, String novoCpf, String novoEndereco, String novoTelefone){
        if(usuarioRepository==null) return false;
        Optional<Usuario> opt = usuarioRepository.findByLogin(login);
        if(opt.isEmpty()) return false;
        Usuario u = opt.get();
        if(novoEmail!=null && !novoEmail.isBlank()) u.setEmail(novoEmail);
        if(novoCpf!=null && !novoCpf.isBlank() && (u.getCpf()==null || u.getCpf().isBlank()))
            u.setCpf(novoCpf);
        if(novoEndereco!=null) u.setEndereco(novoEndereco);
        if(novoTelefone!=null) u.setTelefone(novoTelefone);
        usuarioRepository.save(u);
        return true;
    }
    
    // ========== MÉTODOS CONSOLE (COMPATIBILIDADE) ==========
    // Mantidos exatamente como estavam para compatibilidade
    
    public static Usuario cadastrarUsuario(String nome) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = new Usuario(nome);
            em.persist(usuario);
            em.getTransaction().commit();
            return usuario;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public static Usuario cadastrarUsuario(Usuario usuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            usuario.setStatus(Boolean.TRUE);
            em.getTransaction().begin();
            em.persist(usuario);
            em.getTransaction().commit();
            return usuario;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public static Usuario buscarUsuario(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Usuario.class, id);
        } finally {
            em.close();
        }
    }

    public static Usuario buscarUsuario(String login) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Usuario> usuarios = em.createQuery(
                "SELECT u FROM Usuario u WHERE u.login LIKE :login",
                Usuario.class)
                .setParameter("login", login)
                .getResultList();

            if (!usuarios.isEmpty()) {
                return usuarios.get(0);
            }
            return null;
        } finally {
            em.close();
        }
    }

    public static Usuario buscarUsuarioByEmail(String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Usuario usuario = em.createQuery(
                            "SELECT u FROM Usuario u WHERE u.email LIKE :email",
                            Usuario.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return usuario;
        } finally {
            em.close();
        }
    }

    public static Usuario buscarUsuarioByTokenRecuperacao(String token) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Usuario> usuarios = em.createQuery(
                "SELECT u FROM Usuario u WHERE u.tokenRecuperacao = :token",
                Usuario.class)
                .setParameter("token", token)
                .getResultList();
            
            return usuarios.isEmpty() ? null : usuarios.get(0);
        } finally {
            em.close();
        }
    }

    public static List<Usuario> buscarPorNome(String nome) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT u FROM Usuario u WHERE LOWER(u.nome) LIKE LOWER(:nome)",
                Usuario.class)
                .setParameter("nome", "%" + nome + "%")
                .getResultList();
        } finally {
            em.close();
        }
    }

    public static boolean atualizarUsuario(long id, String novoNome) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = em.find(Usuario.class, id);
            if (usuario == null) {
                return false;
            }
            usuario.setNome(novoNome);
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

    public static boolean atualizarUsuarioRole(String login, String role) {
        List<String> rolesPermitidas = Arrays.asList("ADMIN", "GERENTE", "FUNCIONARIO", "CLIENTE");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            if (!rolesPermitidas.contains(role)) {
                return false;
            }
            em.getTransaction().begin();
            Usuario usuario = em.createQuery("SELECT u FROM Usuario u WHERE u.login LIKE :login", Usuario.class)
                    .setParameter("login", login)
                    .getSingleResult();
            if (usuario == null) {
                return false;
            }
            Usuario usuarioPersistido = new Usuario(usuario);
            usuario.setRole(role);
            AuditoriaService.createAuditoriaUpdate(usuario, usuarioPersistido);
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

    public static void atualizarTokenRecuperacao(String email, String token) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = em.createQuery(
                            "SELECT u FROM Usuario u  WHERE u.email LIKE :email",
                            Usuario.class)
                    .setParameter("email", email)
                    .getSingleResult();

            usuario.setTokenRecuperacao(token);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            em.close();
        }
    }

    public static void atualizarSenha(String login, String senha) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = em.createQuery(
                            "SELECT u FROM Usuario u  WHERE u.login LIKE :login",
                            Usuario.class)
                    .setParameter("login", login)
                    .getSingleResult();

            usuario.setSenha(senha);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            em.close();
        }
    }

    public static boolean removerUsuario(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = em.find(Usuario.class, id);
            if (usuario == null) {
                return false;
            }
            usuario.setStatus(Boolean.FALSE);
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

    public static boolean removerUsuario(String login) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = em.createQuery("SELECT u FROM Usuario u WHERE u.login LIKE :login", Usuario.class)
                    .setParameter("login", login)
                    .getSingleResult();
            if (usuario == null) {
                return false;
            }
            usuario.setStatus(Boolean.FALSE);
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

    public static List<Usuario> listarUsuarios() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM Usuario u", Usuario.class).getResultList();
        } finally {
            em.close();
        }
    }

    public static boolean existeUsuario(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Usuario usuario = em.find(Usuario.class, id);
            return usuario != null;
        } finally {
            em.close();
        }
    }

    public static boolean usuarioEmSituacaoRegular(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Usuario usuario = em.find(Usuario.class, id);
            return usuario != null && usuario.getStatus();
        } finally {
            em.close();
        }
    }

    public boolean removerUsuarioWeb(Long id){
        if(usuarioRepository!=null){
            if(usuarioRepository.existsById(id)){
                usuarioRepository.deleteById(id);
                return true;
            }
            return false;
        }
        return removerUsuario(id);
    }
} 