package com.biblioteca.service;

import com.biblioteca.model.Cliente;
import com.biblioteca.model.Usuario;
import com.biblioteca.repository.ClienteRepository;
import com.biblioteca.util.JPAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.util.List;
import java.util.Optional;

/**
 * Service para gerenciamento de clientes.
 * 
 * COMPATIBILIDADE DUAL:
 * - Web: Usa Spring Data JPA (@Autowired repository)
 * - Console: Usa JPAUtil tradicional (métodos estáticos mantidos)
 * 
 * A lógica de negócio é compartilhada entre as duas versões.
 */
@Service
@Transactional
public class ClienteService {
    
    // ========== DEPENDÊNCIAS SPRING (VERSÃO WEB) ==========
    @Autowired(required = false) // required=false para compatibilidade console
    private ClienteRepository clienteRepository;
    
    @Autowired(required = false)
    private AuditoriaService auditoriaService;
    
    @Autowired(required = false)
    private UsuarioService usuarioService;
    
    // ========== MÉTODOS WEB (SPRING DATA JPA) ==========
    
    /**
     * Salva ou atualiza cliente usando Spring Data JPA (versão web)
     */
    public Cliente salvarClienteWeb(Cliente cliente) {
        if (clienteRepository == null) {
            // Fallback para versão console
            return salvarCliente(cliente);
        }
        
        // Verifica se cliente já existe pelo CPF
        Optional<Cliente> clienteExistente = clienteRepository.findByCpf(cliente.getCpf());
        
        if (clienteExistente.isPresent()) {
            // Atualiza cliente existente
            Cliente cliente_atual = clienteExistente.get();
            Cliente clienteAnterior = new Cliente(); // Para auditoria
            clienteAnterior.setId(cliente_atual.getId());
            clienteAnterior.setNome(cliente_atual.getNome());
            clienteAnterior.setEmail(cliente_atual.getEmail());
            clienteAnterior.setCpf(cliente_atual.getCpf());
            clienteAnterior.setRua(cliente_atual.getRua());
            clienteAnterior.setNumero(cliente_atual.getNumero());
            clienteAnterior.setComplemento(cliente_atual.getComplemento());
            clienteAnterior.setBairro(cliente_atual.getBairro());
            clienteAnterior.setCidade(cliente_atual.getCidade());
            clienteAnterior.setEstado(cliente_atual.getEstado());
            clienteAnterior.setCep(cliente_atual.getCep());
            
            cliente_atual.setNome(cliente.getNome());
            cliente_atual.setEmail(cliente.getEmail());
            cliente_atual.setRua(cliente.getRua());
            cliente_atual.setNumero(cliente.getNumero());
            cliente_atual.setComplemento(cliente.getComplemento());
            cliente_atual.setBairro(cliente.getBairro());
            cliente_atual.setCidade(cliente.getCidade());
            cliente_atual.setEstado(cliente.getEstado());
            cliente_atual.setCep(cliente.getCep());
            
            Cliente clienteSalvo = clienteRepository.save(cliente_atual);
            
            // Registrar auditoria de UPDATE
            if (auditoriaService != null && usuarioService != null) {
                Usuario usuario = getCurrentUser();
                if (usuario != null) {
                    auditoriaService.createAuditoriaUpdateWeb(clienteSalvo, clienteAnterior, usuario);
                }
            }
            
            return clienteSalvo;
        }
        
        // Salva novo cliente
        Cliente clienteSalvo = clienteRepository.save(cliente);
        
        // Registrar auditoria de INSERT
        if (auditoriaService != null && usuarioService != null) {
            Usuario usuario = getCurrentUser();
            if (usuario != null) {
                auditoriaService.createAuditoriaInsertWeb(clienteSalvo, usuario);
            }
        }
        
        return clienteSalvo;
    }
    
    /**
     * Busca cliente por CPF usando Spring Data JPA (versão web)
     */
    public Optional<Cliente> buscarPorCpfWeb(String cpf) {
        if (clienteRepository == null) {
            // Fallback para versão console
            Cliente cliente = buscarPorCpf(cpf);
            return Optional.ofNullable(cliente);
        }
        
        return clienteRepository.findByCpf(cpf);
    }
    
    /**
     * Busca cliente por ID usando Spring Data JPA (versão web)
     */
    public Optional<Cliente> buscarClienteWeb(Long id) {
        if (clienteRepository == null) {
            return Optional.empty();
        }
        
        return clienteRepository.findById(id);
    }
    
    /**
     * Lista todos os clientes usando Spring Data JPA (versão web)
     */
    public List<Cliente> listarTodosWeb() {
        if (clienteRepository == null) {
            // Fallback para versão console
            return listarTodos();
        }
        
        return clienteRepository.findAll();
    }
    
    /**
     * Busca clientes por nome usando Spring Data JPA (versão web)
     */
    public List<Cliente> buscarPorNomeWeb(String nome) {
        if (clienteRepository == null) {
            return List.of(); // Retorna lista vazia na versão console
        }
        
        return clienteRepository.findByNomeContainingIgnoreCase(nome);
    }
    
    /**
     * Conta total de clientes (versão web)
     */
    public long contarClientes() {
        if (clienteRepository != null) {
            return clienteRepository.count();
        }
        // Fallback para versão console
        return listarTodos().size();
    }
    
    /**
     * Remove cliente por ID (versão web)
     */
    public boolean removerClienteWeb(Long id) {
        if (clienteRepository == null) {
            return false;
        }
        
        try {
            Optional<Cliente> clienteOptional = clienteRepository.findById(id);
            if (clienteOptional.isPresent()) {
                Cliente cliente = clienteOptional.get();
                
                // Registrar auditoria antes de deletar
                if (auditoriaService != null && usuarioService != null) {
                    Usuario usuario = getCurrentUser();
                    if (usuario != null) {
                        auditoriaService.createAuditoriaDeleteWeb(cliente, usuario);
                    }
                }
                
                clienteRepository.deleteById(id);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
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
    
    // ========== MÉTODOS CONSOLE (COMPATIBILIDADE) ==========
    // Mantidos exatamente como estavam para compatibilidade
    
    public static Cliente salvarCliente(Cliente cliente) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            
            Cliente clienteExistente = buscarPorCpf(cliente.getCpf());
            if (clienteExistente != null) {
                
                clienteExistente.setNome(cliente.getNome());
                clienteExistente.setEmail(cliente.getEmail());
                clienteExistente.setRua(cliente.getRua());
                clienteExistente.setNumero(cliente.getNumero());
                clienteExistente.setComplemento(cliente.getComplemento());
                clienteExistente.setBairro(cliente.getBairro());
                clienteExistente.setCidade(cliente.getCidade());
                clienteExistente.setEstado(cliente.getEstado());
                clienteExistente.setCep(cliente.getCep());
                em.merge(clienteExistente);
                em.getTransaction().commit();
                return clienteExistente;
            }
            
            
            em.persist(cliente);
            em.getTransaction().commit();
            return cliente;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public static Cliente buscarPorCpf(String cpf) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT c FROM Cliente c WHERE c.cpf = :cpf",
                Cliente.class)
                .setParameter("cpf", cpf)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    public static List<Cliente> listarTodos() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM Cliente c", Cliente.class)
                .getResultList();
        } finally {
            em.close();
        }
    }
} 