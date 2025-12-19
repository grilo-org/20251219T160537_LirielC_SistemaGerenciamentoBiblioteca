package com.biblioteca.service;

import com.biblioteca.model.Emprestimo;
import com.biblioteca.model.Livro;
import com.biblioteca.model.Usuario;
import com.biblioteca.repository.EmprestimoRepository;
import com.biblioteca.util.JPAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service para gerenciamento de empréstimos.
 * 
 * COMPATIBILIDADE DUAL:
 * - Web: Usa Spring Data JPA (@Autowired repository)
 * - Console: Usa JPAUtil tradicional (métodos estáticos mantidos)
 */
@Service
@Transactional
public class EmprestimoService {
    
    // ========== DEPENDÊNCIAS SPRING (VERSÃO WEB) ==========
    @Autowired(required = false)
    private EmprestimoRepository emprestimoRepository;
    
    @Autowired(required = false)
    private EmailService emailService;
    
    @Autowired(required = false)
    private com.biblioteca.repository.LivroRepository livroRepository;
    
    @Autowired(required = false)
    private AuditoriaService auditoriaService;
    
    @org.springframework.beans.factory.annotation.Value("${aluguel.prazo-dias:7}")
    private int prazoDiasPadrao;
    
    @org.springframework.beans.factory.annotation.Value("${aluguel.multa-dia:2.0}")
    private double multaPorDia;
    
    // ========== MÉTODOS WEB (SPRING DATA JPA) ==========
    
    /**
     * Realiza empréstimo usando Spring Data JPA (versão web)
     */
    public Emprestimo realizarEmprestimoWeb(Usuario usuario, Livro livro) {
        if (emprestimoRepository == null) {
            // Fallback para versão console
            return realizarEmprestimo(usuario, livro);
        }
        
        // Verificações de negócio
        if (temEmprestimosEmAtrasoWeb(usuario)) {
            throw new RuntimeException("Usuário possui empréstimos em atraso");
        }
        
        if (contarEmprestimosAtivosWeb(usuario) >= 3) {
            throw new RuntimeException("Usuário já atingiu o limite de empréstimos");
        }
        
        if(calcularMultasPendentesWeb(usuario) > 0){
            throw new RuntimeException("Usuário possui multas pendentes. Regularize antes de novo empréstimo.");
        }
        
        // Valida estoque
        if (livro.getQuantidadeEstoque() != null && livro.getQuantidadeEstoque() <= 0) {
            throw new RuntimeException("Livro sem estoque disponível");
        }
        
        // Criar novo empréstimo
        LocalDate dataEmprestimo = LocalDate.now();
        LocalDate dataPrevista = dataEmprestimo.plusDays(prazoDiasPadrao);
        
        Emprestimo emprestimo = new Emprestimo(usuario, livro, dataEmprestimo, dataPrevista);
        emprestimo.setPrazoDias(prazoDiasPadrao);
        Emprestimo emprestimoSalvo = emprestimoRepository.save(emprestimo);
        
        // Decrementa estoque do livro
        if (livroRepository != null) {
            livro.setQuantidadeEstoque(livro.getQuantidadeEstoque() == null ? null : livro.getQuantidadeEstoque() - 1);
            livroRepository.save(livro);
        }
        
        // Auditoria
        if (auditoriaService != null) {
            auditoriaService.createAuditoriaInsertWeb(emprestimoSalvo, usuario);
        }
        
        // Envia email de confirmação se EmailService disponível
        if (emailService != null) {
            try {
                String dataVencimentoStr = dataPrevista.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                emailService.enviarEmailConfirmacaoEmprestimoWeb(usuario, livro, dataVencimentoStr);
            } catch (Exception e) {
                System.out.println("⚠️ Erro ao enviar email de confirmação: " + e.getMessage());
            }
        }
        
        long diasAtraso = java.time.temporal.ChronoUnit.DAYS.between(emprestimo.getDataPrevista(), LocalDate.now());
        double multa = 0.0;
        if(diasAtraso>0){
            multa = diasAtraso * (emprestimo.getValorEmprestimo()*0.10);
        }
        emprestimo.setMultaTotal(multa);
        
        return emprestimoSalvo;
    }
    
    /**
     * Registra devolução (versão web)
     */
    public double registrarDevolucaoWeb(Long emprestimoId) {
        if (emprestimoRepository == null) {
            // Fallback para versão console
            return registrarDevolucao(emprestimoId);
        }
        
        Optional<Emprestimo> emprestimoOpt = emprestimoRepository.findById(emprestimoId);
        if (emprestimoOpt.isEmpty()) {
            throw new RuntimeException("Empréstimo não encontrado");
        }
        
        Emprestimo emprestimo = emprestimoOpt.get();
        if (emprestimo.isDevolvido()) {
            throw new RuntimeException("Empréstimo já foi devolvido");
        }
        
        emprestimo.setDataDevolucao(LocalDate.now());
        emprestimo.setDevolvido(true);
        emprestimo.setStatus("DEVOLVIDO");
        
        double multa = emprestimo.calcularMulta();
        emprestimoRepository.save(emprestimo);
        
        // Incrementa estoque do livro
        if (livroRepository != null) {
            Livro livro = emprestimo.getLivro();
            livro.setQuantidadeEstoque(livro.getQuantidadeEstoque() == null ? null : livro.getQuantidadeEstoque() + 1);
            livroRepository.save(livro);
        }
        
        // Auditoria
        if (auditoriaService != null) {
            auditoriaService.createAuditoriaUpdateWeb(emprestimo, emprestimo, emprestimo.getUsuario());
        }
        
        return multa;
    }
    
    /**
     * Verifica empréstimos em atraso (versão web)
     */
    public boolean temEmprestimosEmAtrasoWeb(Usuario usuario) {
        if (emprestimoRepository == null) {
            // Fallback para versão console
            return temEmprestimosEmAtraso(usuario);
        }
        return emprestimoRepository.countOverdueLoans(LocalDate.now()) > 0;
    }
    
    /**
     * Conta empréstimos ativos por usuário (versão web)
     */
    public int contarEmprestimosAtivosWeb(Usuario usuario) {
        if (emprestimoRepository == null) {
            // Fallback para versão console
            return contarEmprestimosAtivos(usuario);
        }
        return (int) emprestimoRepository.countActiveLoansByUser(usuario);
    }
    
    /**
     * Lista empréstimos ativos do usuário (versão web)
     */
    public List<Emprestimo> listarEmprestimosAtivosWeb(Usuario usuario) {
        if (emprestimoRepository == null) {
            // Fallback para versão console
            return listarEmprestimosAtivos(usuario);
        }
        return emprestimoRepository.findByUsuario(usuario);
    }
    
    /**
     * Lista todos os empréstimos ativos (versão web)
     */
    public List<Emprestimo> listarTodosEmprestimosAtivosWeb() {
        if (emprestimoRepository == null) {
            // Fallback para versão console
            return listarTodosEmprestimosAtivos();
        }
        return emprestimoRepository.findActiveLoans();
    }
    
    /**
     * Lista todos os empréstimos (versão web)
     */
    public List<Emprestimo> listarTodosEmprestimosWeb() {
        if (emprestimoRepository == null) {
            return List.of();
        }
        return emprestimoRepository.findAll();
    }
    
    /**
     * Verifica situação regular do usuário (versão web)
     */
    public boolean usuarioEmSituacaoRegularWeb(Usuario usuario) {
        return !temEmprestimosEmAtrasoWeb(usuario) && contarEmprestimosAtivosWeb(usuario) < 3;
    }
    
    /**
     * Lista empréstimos em atraso (versão web)
     */
    public List<Emprestimo> listarEmprestimosEmAtraso() {
        if (emprestimoRepository != null) {
            return emprestimoRepository.findOverdueLoans(LocalDate.now());
        }
        // Fallback manual
        return listarTodosEmprestimosAtivos().stream()
            .filter(e -> e.getDataPrevista().isBefore(LocalDate.now()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Conta total de empréstimos (versão web)
     */
    public long contarEmprestimos() {
        if (emprestimoRepository != null) {
            return emprestimoRepository.count();
        }
        // Fallback para versão console - contador manual
        return listarTodosEmprestimosAtivos().size();
    }
    
    /**
     * Conta empréstimos ativos totais (versão web)
     */
    public long contarEmprestimosAtivos() {
        if (emprestimoRepository != null) {
            return emprestimoRepository.countActiveLoans();
        }
        // Fallback para versão console
        return listarTodosEmprestimosAtivos().size();
    }
    
    public List<Emprestimo> listarPaginado(int offset,int limit){
        if(emprestimoRepository!=null){
            Pageable p= PageRequest.of(offset/limit,limit);
            return emprestimoRepository.findAll(p).getContent();
        }
        List<Emprestimo> todos=listarTodosEmprestimosWeb();
        int to=Math.min(offset+limit, todos.size());
        if(offset>to) return java.util.Collections.emptyList();
        return todos.subList(offset,to);
    }

    public Emprestimo salvarEmprestimo(Usuario usuario,Livro livro, LocalDate dataEmprestimo, LocalDate dataPrevista){
        Emprestimo emp=new Emprestimo(usuario,livro,dataEmprestimo,dataPrevista);
        if(emprestimoRepository!=null){
            return emprestimoRepository.save(emp);
        }
        EntityManager em= JPAUtil.getEntityManager();
        em.getTransaction().begin();
        em.persist(emp);
        em.getTransaction().commit();
        em.close();
        return emp;
    }

    public Emprestimo devolverEmprestimo(Long id){
        if(emprestimoRepository!=null){
            return emprestimoRepository.findById(id).map(e->{
                e.setDevolvido(true);
                e.setDataDevolucao(LocalDate.now());
                e.setStatus("DEVOLVIDO");
                return emprestimoRepository.save(e);
            }).orElse(null);
        }
        EntityManager em=JPAUtil.getEntityManager();
        Emprestimo e=em.find(Emprestimo.class,id);
        if(e==null) return null;
        em.getTransaction().begin();
        e.setDevolvido(true);
        e.setDataDevolucao(LocalDate.now());
        e.setStatus("DEVOLVIDO");
        em.getTransaction().commit();
        em.close();
        return e;
    }
    
    // ========== MÉTODOS CONSOLE (COMPATIBILIDADE) ==========
    // Mantidos exatamente como estavam para compatibilidade
    
    public static Emprestimo realizarEmprestimo(Usuario usuario, Livro livro) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            if (temEmprestimosEmAtraso(usuario)) {
                throw new RuntimeException("Usuário possui empréstimos em atraso");
            }

            if (contarEmprestimosAtivos(usuario) >= 3) {
                throw new RuntimeException("Usuário já atingiu o limite de empréstimos");
            }

            em.getTransaction().begin();
            LocalDate dataEmprestimo = LocalDate.now();
            LocalDate dataPrevista = dataEmprestimo.plusDays(7); // 7 dias de prazo

            Emprestimo emprestimo = new Emprestimo(usuario, livro, dataEmprestimo, dataPrevista);
            em.persist(emprestimo);
            em.getTransaction().commit();
            return emprestimo;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public static double registrarDevolucao(long emprestimoId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Emprestimo emprestimo = em.find(Emprestimo.class, emprestimoId);
            
            if (emprestimo == null) {
                throw new RuntimeException("Empréstimo não encontrado");
            }

            if (emprestimo.isDevolvido()) {
                throw new RuntimeException("Empréstimo já foi devolvido");
            }

            emprestimo.setDataDevolucao(LocalDate.now());
            emprestimo.setDevolvido(true);
            emprestimo.setStatus("DEVOLVIDO");

            long diasAtraso = java.time.temporal.ChronoUnit.DAYS.between(emprestimo.getDataPrevista(), LocalDate.now());
            double multa = 0.0;
            if(diasAtraso>0){
                multa = diasAtraso * (emprestimo.getValorEmprestimo()*0.10);
            }
            emprestimo.setMultaTotal(multa);

            em.getTransaction().commit();
            return multa;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public static boolean temEmprestimosEmAtraso(Usuario usuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(e) FROM Emprestimo e " +
                "WHERE e.usuario = :usuario " +
                "AND e.devolvido = false " +
                "AND e.dataPrevista < CURRENT_DATE",
                Long.class)
                .setParameter("usuario", usuario)
                .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    public static int contarEmprestimosAtivos(Usuario usuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(e) FROM Emprestimo e " +
                "WHERE e.usuario = :usuario " +
                "AND e.devolvido = false",
                Long.class)
                .setParameter("usuario", usuario)
                .getSingleResult();
            return count.intValue();
        } finally {
            em.close();
        }
    }

    public static List<Emprestimo> listarEmprestimosAtivos(Usuario usuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT e FROM Emprestimo e " +
                "WHERE e.usuario = :usuario " +
                "AND e.devolvido = false",
                Emprestimo.class)
                .setParameter("usuario", usuario)
                .getResultList();
        } finally {
            em.close();
        }
    }

    public static List<Emprestimo> listarTodosEmprestimosAtivos() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT e FROM Emprestimo e " +
                "WHERE e.devolvido = false " +
                "ORDER BY e.dataPrevista ASC",
                Emprestimo.class)
                .getResultList();
        } finally {
            em.close();
        }
    }

    public static boolean usuarioEmSituacaoRegular(Usuario usuario) {
        if (temEmprestimosEmAtraso(usuario)) {
            return false;
        }

        if (contarEmprestimosAtivos(usuario) >= 3) {
            return false;
        }

        return true;
    }

    public static double calcularMultasPendentes(Usuario usuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Emprestimo> emprestimos = em.createQuery(
                "SELECT e FROM Emprestimo e " +
                "WHERE e.usuario = :usuario " +
                "AND e.devolvido = true " +
                "AND e.dataDevolucao > e.dataPrevista",
                Emprestimo.class)
                .setParameter("usuario", usuario)
                .getResultList();

            return emprestimos.stream()
                .mapToDouble(Emprestimo::calcularMulta)
                .sum();
        } finally {
            em.close();
        }
    }

    /** Retorna soma das multas pendentes de um usuário (versão web) */
    public double calcularMultasPendentesWeb(Usuario usuario){
        if(emprestimoRepository==null) return 0.0;
        return emprestimoRepository.findByUsuario(usuario).stream()
                .filter(Emprestimo::isDevolvido)
                .mapToDouble(e -> e.getMultaTotal()!=null?e.getMultaTotal():0.0)
                .sum();
    }
    
    /**
     * Busca todos os usuários que possuem multas pendentes
     */
    public List<Usuario> buscarUsuariosComMultas() {
        if (emprestimoRepository != null) {
            return emprestimoRepository.findUsersWithFines(LocalDate.now());
        }
        return java.util.Collections.emptyList();
    }
    
    /**
     * Busca empréstimos com multa de um usuário específico
     */
    public List<Emprestimo> buscarEmprestimosComMultaPorUsuario(Usuario usuario) {
        if (emprestimoRepository != null) {
            return emprestimoRepository.findLoansWithFinesByUser(usuario, LocalDate.now());
        }
        return java.util.Collections.emptyList();
    }
    
    /**
     * Calcula multa total de um usuário (incluindo empréstimos ativos atrasados)
     */
    public double calcularMultaTotalUsuario(Usuario usuario) {
        if (emprestimoRepository == null) return 0.0;
        
        List<Emprestimo> emprestimosComMulta = buscarEmprestimosComMultaPorUsuario(usuario);
        return emprestimosComMulta.stream()
                .mapToDouble(e -> {
                    if (e.isDevolvido()) {
                        // Multa já calculada para empréstimos devolvidos
                        return e.getMultaTotal() != null ? e.getMultaTotal() : 0.0;
                    } else {
                        // Calcular multa atual para empréstimos ativos atrasados
                        return e.calcularMulta();
                    }
                })
                .sum();
    }
} 