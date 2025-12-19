package com.biblioteca.service;

import com.biblioteca.model.Auditoria;
import com.biblioteca.model.Usuario;
import com.biblioteca.repository.AuditoriaRepository;
import com.biblioteca.util.JPAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static com.biblioteca.Main.usuarioAtivo;

/**
 * Service para gerenciamento de auditoria.
 * 
 * COMPATIBILIDADE DUAL:
 * - Web: Usa Spring Data JPA (@Autowired repository)
 * - Console: Usa JPAUtil tradicional (métodos estáticos mantidos)
 */
@Service
@Transactional
public class AuditoriaService {

    // ========== DEPENDÊNCIAS SPRING (VERSÃO WEB) ==========
    @Autowired(required = false) // required=false para compatibilidade console
    private AuditoriaRepository auditoriaRepository;

    // ========== MÉTODOS WEB (SPRING DATA JPA) ==========
    
    /**
     * Cria auditoria de INSERT usando Spring Data JPA (versão web)
     */
    public <E> void createAuditoriaInsertWeb(E entidade, Usuario usuario) {
        if (auditoriaRepository == null) {
            // Fallback para versão console
            createAuditoriaInsert(entidade);
            return;
        }
        
        Auditoria auditoria = buildAuditoriaWeb(entidade, "INSERT", usuario);
        auditoria.setDadosAlterados(getDadosAlteradosInsertAndDelete(entidade));
        auditoriaRepository.save(auditoria);
    }
    
    /**
     * Cria auditoria de UPDATE usando Spring Data JPA (versão web)
     */
    public <E, T> void createAuditoriaUpdateWeb(E entidadeAtualizada, T entidadePersistida, Usuario usuario) {
        if (auditoriaRepository == null) {
            // Fallback para versão console
            createAuditoriaUpdate(entidadeAtualizada, entidadePersistida);
            return;
        }
        
        Auditoria auditoria = buildAuditoriaWeb(entidadePersistida, "UPDATE", usuario);
        auditoria.setDadosAlterados(getDadosAlteradosUpdate(entidadeAtualizada, entidadePersistida));
        auditoriaRepository.save(auditoria);
    }
    
    /**
     * Registra evento de LOGIN na auditoria
     */
    public void registrarLogin(Usuario usuario, boolean sucesso, String detalhes) {
        if (auditoriaRepository == null) {
            return;
        }
        
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setData(LocalDateTime.now());
        auditoria.setNomeEntidade("Sistema");
        auditoria.setIdEntidade(usuario != null ? usuario.getId() : null);
        auditoria.setAcao(sucesso ? "LOGIN_SUCESSO" : "LOGIN_FALHA");
        auditoria.setDadosAlterados(detalhes);
        
        auditoriaRepository.save(auditoria);
    }

    /**
     * Registra evento de LOGOUT na auditoria
     */
    public void registrarLogout(Usuario usuario) {
        if (auditoriaRepository == null) {
            return;
        }
        
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setData(LocalDateTime.now());
        auditoria.setNomeEntidade("Sistema");
        auditoria.setIdEntidade(usuario.getId());
        auditoria.setAcao("LOGOUT");
        auditoria.setDadosAlterados("Usuário realizou logout do sistema");
        
        auditoriaRepository.save(auditoria);
    }

    /**
     * Registra mudança de senha na auditoria
     */
    public void registrarMudancaSenha(Usuario usuario, String detalhes) {
        if (auditoriaRepository == null) {
            return;
        }
        
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setData(LocalDateTime.now());
        auditoria.setNomeEntidade("Usuario");
        auditoria.setIdEntidade(usuario.getId());
        auditoria.setAcao("MUDANCA_SENHA");
        auditoria.setDadosAlterados(detalhes);
        
        auditoriaRepository.save(auditoria);
    }

    /**
     * Registra tentativa de acesso negado na auditoria
     */
    public void registrarAcessoNegado(Usuario usuario, String recurso, String detalhes) {
        if (auditoriaRepository == null) {
            return;
        }
        
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setData(LocalDateTime.now());
        auditoria.setNomeEntidade("Sistema");
        auditoria.setIdEntidade(usuario != null ? usuario.getId() : null);
        auditoria.setAcao("ACESSO_NEGADO");
        auditoria.setDadosAlterados("Tentativa de acesso negado ao recurso: " + recurso + ". " + detalhes);
        
        auditoriaRepository.save(auditoria);
    }

    /**
     * Registra operação personalizada na auditoria
     */
    public void registrarOperacao(Usuario usuario, String entidade, Long idEntidade, String acao, String detalhes) {
        if (auditoriaRepository == null) {
            return;
        }
        
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setData(LocalDateTime.now());
        auditoria.setNomeEntidade(entidade);
        auditoria.setIdEntidade(idEntidade);
        auditoria.setAcao(acao);
        auditoria.setDadosAlterados(detalhes);
        
        auditoriaRepository.save(auditoria);
    }

    /**
     * Cria auditoria de DELETE usando Spring Data JPA (versão web)
     */
    public <E> void createAuditoriaDeleteWeb(E entidade, Usuario usuario) {
        if (auditoriaRepository == null) {
            // Fallback para versão console
            createAuditoriaDelete(entidade);
            return;
        }
        
        Auditoria auditoria = buildAuditoriaWeb(entidade, "DELETE", usuario);
        auditoria.setDadosAlterados(getDadosAlteradosInsertAndDelete(entidade));
        auditoriaRepository.save(auditoria);
    }
    
    /**
     * Build auditoria para versão web (com usuário parametrizado)
     */
    private <E> Auditoria buildAuditoriaWeb(E entidade, String acao, Usuario usuario) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setData(LocalDateTime.now());
        auditoria.setNomeEntidade(entidade.getClass().getSimpleName());
        auditoria.setIdEntidade(getEntityIdFromEntityFields(entidade));
        auditoria.setAcao(acao);
        return auditoria;
    }

    // ========== MÉTODOS CONSOLE (COMPATIBILIDADE) ==========
    // Mantidos exatamente como estavam para compatibilidade

    public static <E> void createAuditoriaInsert(E entidade) {
        Auditoria auditoria = buildAuditoria(entidade, "INSERT");
        auditoria.setDadosAlterados(getDadosAlteradosInsertAndDelete(entidade));
        salvarAuditoria(auditoria);
    }

    public static <E, T> void createAuditoriaUpdate(E entidadeAtualizada, T entidadePersistida) {
        Auditoria auditoria = buildAuditoria(entidadePersistida, "UPDATE");
        auditoria.setDadosAlterados(getDadosAlteradosUpdate(entidadeAtualizada, entidadePersistida));
        salvarAuditoria(auditoria);
    }

    public static <E> void createAuditoriaDelete(E entidade) {
        Auditoria auditoria = buildAuditoria(entidade, "DELETE");
        auditoria.setDadosAlterados(getDadosAlteradosInsertAndDelete(entidade));
        salvarAuditoria(auditoria);
    }

    private static <E> Auditoria buildAuditoria(E entidade, String acao) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuarioAtivo);
        auditoria.setData(LocalDateTime.now());
        auditoria.setNomeEntidade(entidade.getClass().getSimpleName());
        auditoria.setIdEntidade(getEntityIdFromEntityFields(entidade));
        auditoria.setAcao(acao);

        return auditoria;
    }

    public static <E> String getDadosAlteradosInsertAndDelete(E entidade) {
        try {
            return (String) entidade.getClass().getDeclaredMethod("toString").invoke(entidade, null);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return "";
    }

    public static <E, T> String getDadosAlteradosUpdate(E entidadeAtualizada, T entidadePersistida) {
        try {
            String dadosAtualizados = (String) entidadeAtualizada.getClass().getDeclaredMethod("toString").invoke(entidadeAtualizada, null);
            String dadosPersistidos = (String) entidadePersistida.getClass().getDeclaredMethod("toString").invoke(entidadePersistida, null);
            return "Dados Atualizados = " + dadosAtualizados + "; Dados Anteriores = " + dadosPersistidos;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return "";
    }

    public static <E> Long getEntityIdFromEntityFields(E entity) {
        Field[] entityFields = entity.getClass().getDeclaredFields();

        try {
            for (Field entityField : entityFields) {
                entityField.setAccessible(Boolean.TRUE);
                String entityFieldName = entityField.getName();
                Object entityFieldValue = entityField.get(entity);
                entityField.setAccessible(Boolean.FALSE);

                if (entityFieldName.equals("id")) {
                    return Long.parseLong(entityFieldValue.toString());
                }
            }
        } catch(Exception e) {
            System.out.println("Erro na busca do entityId ao criar auditoria");
        }
        return null;
    }

    private static Auditoria salvarAuditoria(Auditoria auditoria) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(auditoria);
            em.getTransaction().commit();
            return auditoria;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
