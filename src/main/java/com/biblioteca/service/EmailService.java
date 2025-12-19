package com.biblioteca.service;

import com.biblioteca.model.Fornecedor;
import com.biblioteca.model.Livro;
import com.biblioteca.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service para envio de emails.
 * 
 * COMPATIBILIDADE DUAL:
 * - Web: Usa mock/simulação de email com log estruturado
 * - Console: Usa geração de arquivo HTML para preview
 * 
 * Para produção, pode ser facilmente integrado com JavaMailSender.
 */
@Service
public class EmailService {
    
    // ========== DEPENDÊNCIAS SPRING (VERSÃO WEB) ==========
    @Autowired(required = false)
    private UsuarioService usuarioService;
    
    // Mock de emails enviados para debug/testing
    private static final List<Map<String, Object>> emailsEnviados = new ArrayList<>();
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    // ========== MÉTODOS WEB (MOCKADOS/SIMULADOS) ==========
    
    /**
     * Envia email de recuperação de senha (versão web mockada)
     */
    public boolean enviarEmailRecuperacaoSenhaWeb(String email, String token) {
        // Verifica se usuário existe
        if (usuarioService != null) {
            try {
                if (!usuarioService.existeEmail(email)) {
                    System.out.println("❌ [EMAIL WEB] Email não cadastrado: " + email);
                    return false;
                }
            } catch (Exception e) {
                // Fallback para console
                return enviarEmailDeRecuperacaoDeSenhaConsole(email, token);
            }
        }
        
        // Mock do envio - simula sucesso
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("tipo", "RECUPERACAO_SENHA");
        emailData.put("destinatario", email);
        emailData.put("token", token);
        emailData.put("dataEnvio", LocalDateTime.now());
        emailData.put("status", "ENVIADO");
        emailData.put("assunto", "Redefinição de Senha - Sistema Biblioteca");
        emailsEnviados.add(emailData);
        
        System.out.println("✅ [EMAIL WEB] Email de recuperação enviado para: " + email);
        System.out.println("📧 [EMAIL WEB] Token: " + token);
        System.out.println("🕒 [EMAIL WEB] Enviado em: " + LocalDateTime.now().format(FORMATTER));
        
        return true;
    }
    
    /**
     * Envia email de reposição de estoque (versão web mockada)
     */
    public boolean enviarEmailReposicaoEstoqueWeb(List<Livro> livros, Fornecedor fornecedor) {
        if (fornecedor == null || fornecedor.getEmail() == null) {
            System.out.println("❌ [EMAIL WEB] Fornecedor sem email válido");
            return false;
        }
        
        // Mock do envio - simula sucesso
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("tipo", "REPOSICAO_ESTOQUE");
        emailData.put("destinatario", fornecedor.getEmail());
        emailData.put("fornecedor", fornecedor.getNome());
        emailData.put("quantidadeLivros", livros.size());
        emailData.put("livros", livros.stream().map(Livro::getTitulo).toArray());
        emailData.put("dataEnvio", LocalDateTime.now());
        emailData.put("status", "ENVIADO");
        emailData.put("assunto", "Solicitação de Reposição - Sistema Biblioteca");
        emailsEnviados.add(emailData);
        
        System.out.println("✅ [EMAIL WEB] Email de reposição enviado para: " + fornecedor.getEmail());
        System.out.println("🏢 [EMAIL WEB] Fornecedor: " + fornecedor.getNome());
        System.out.println("📚 [EMAIL WEB] Livros solicitados: " + livros.size());
        System.out.println("🕒 [EMAIL WEB] Enviado em: " + LocalDateTime.now().format(FORMATTER));
        
        return true;
    }
    
    /**
     * Envia email de confirmação de empréstimo (novo para web)
     */
    public boolean enviarEmailConfirmacaoEmprestimoWeb(Usuario usuario, Livro livro, String dataVencimento) {
        if (usuario == null || usuario.getEmail() == null) {
            System.out.println("❌ [EMAIL WEB] Usuário sem email válido");
            return false;
        }
        
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("tipo", "CONFIRMACAO_EMPRESTIMO");
        emailData.put("destinatario", usuario.getEmail());
        emailData.put("usuario", usuario.getNome());
        emailData.put("livro", livro.getTitulo());
        emailData.put("dataVencimento", dataVencimento);
        emailData.put("dataEnvio", LocalDateTime.now());
        emailData.put("status", "ENVIADO");
        emailData.put("assunto", "Confirmação de Empréstimo - Sistema Biblioteca");
        emailsEnviados.add(emailData);
        
        System.out.println("✅ [EMAIL WEB] Email de confirmação enviado para: " + usuario.getEmail());
        System.out.println("👤 [EMAIL WEB] Usuário: " + usuario.getNome());
        System.out.println("📖 [EMAIL WEB] Livro: " + livro.getTitulo());
        System.out.println("📅 [EMAIL WEB] Vencimento: " + dataVencimento);
        
        return true;
    }
    
    /**
     * Envia email de lembrete de devolução (novo para web)
     */
    public boolean enviarEmailLembreteWeb(Usuario usuario, List<Livro> livrosAtrasados) {
        if (usuario == null || usuario.getEmail() == null) {
            System.out.println("❌ [EMAIL WEB] Usuário sem email válido");
            return false;
        }
        
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("tipo", "LEMBRETE_DEVOLUCAO");
        emailData.put("destinatario", usuario.getEmail());
        emailData.put("usuario", usuario.getNome());
        emailData.put("quantidadeLivros", livrosAtrasados.size());
        emailData.put("livros", livrosAtrasados.stream().map(Livro::getTitulo).toArray());
        emailData.put("dataEnvio", LocalDateTime.now());
        emailData.put("status", "ENVIADO");
        emailData.put("assunto", "Lembrete de Devolução - Sistema Biblioteca");
        emailsEnviados.add(emailData);
        
        System.out.println("⚠️ [EMAIL WEB] Email de lembrete enviado para: " + usuario.getEmail());
        System.out.println("👤 [EMAIL WEB] Usuário: " + usuario.getNome());
        System.out.println("📚 [EMAIL WEB] Livros em atraso: " + livrosAtrasados.size());
        
        return true;
    }
    
    /**
     * Envia email de confirmação de alteração de senha (novo para web)
     */
    public boolean enviarEmailConfirmacaoAlteracaoWeb(String email) {
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("tipo", "CONFIRMACAO_ALTERACAO_SENHA");
        emailData.put("destinatario", email);
        emailData.put("dataEnvio", LocalDateTime.now());
        emailData.put("status", "ENVIADO");
        emailData.put("assunto", "Senha Alterada - Sistema Biblioteca");
        emailsEnviados.add(emailData);
        
        System.out.println("✅ [EMAIL WEB] Email de confirmação enviado para: " + email);
        System.out.println("🔐 [EMAIL WEB] Confirmação de alteração de senha");
        System.out.println("🕒 [EMAIL WEB] Enviado em: " + LocalDateTime.now().format(FORMATTER));
        
        return true;
    }
    
    /**
     * Lista emails enviados (para debug/admin)
     */
    public List<Map<String, Object>> listarEmailsEnviados() {
        return new ArrayList<>(emailsEnviados);
    }
    
    /**
     * Conta emails enviados por tipo
     */
    public Map<String, Long> contarEmailsPorTipo() {
        Map<String, Long> contadores = new HashMap<>();
        emailsEnviados.forEach(email -> {
            String tipo = (String) email.get("tipo");
            contadores.merge(tipo, 1L, Long::sum);
        });
        return contadores;
    }
    
    /**
     * Limpa histórico de emails (para testing)
     */
    public void limparHistoricoEmails() {
        emailsEnviados.clear();
        System.out.println("🗑️ [EMAIL WEB] Histórico de emails limpo");
    }
    
    /**
     * Envia um email simples (mock) para qualquer finalidade.
     */
    public boolean enviarEmail(String destinatario, String assunto, String corpo){
        if(destinatario==null || destinatario.isBlank()) return false;
        Map<String,Object> emailData = new java.util.HashMap<>();
        emailData.put("tipo","LIVRE");
        emailData.put("destinatario",destinatario);
        emailData.put("assunto",assunto);
        emailData.put("corpo",corpo);
        emailData.put("status","ENVIADO");
        emailData.put("dataEnvio",java.time.LocalDateTime.now());
        emailsEnviados.add(emailData);
        System.out.println("📧 [EMAIL WEB] ("+assunto+") enviado para: "+destinatario);
        return true;
    }
    
    // ========== MÉTODOS CONSOLE (COMPATIBILIDADE) ==========
    // Mantidos exatamente como estavam para compatibilidade
    
    public static boolean enviarEmailDeRecuperacaoDeSenhaConsole(String email, String token) {
        Usuario usuario = UsuarioService.buscarUsuarioByEmail(email);
        if (usuario == null) {
            System.out.println("Email não cadastrado");
            return false;
        }
        System.out.println("Enviando email de recuperacao de senha para " + email);
        try {
            generateEmailFromTemplate(getEmailRecuperacaoSenhaContent(token));
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static void enviarEmailDeReposicaoDeEstoque(List<Livro> livros, Fornecedor fornecedor) {
        if (fornecedor == null) {
            System.out.println("Não existe fornecedor com email válido");
            return;
        }
        System.out.println("Enviando email de pedido de reposição de estoque para " + fornecedor.getEmail());
        try {
            generateEmailFromTemplate(getEmailReposicaoEstoque(livros));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void generateEmailFromTemplate(String emailBody) {
        try {
            previewHtmlInBrowser(emailBody);
        } catch (Exception e) {
            System.out.println("Não foi possivel criar o conteúdo do email");
        }
    }

    public static void previewHtmlInBrowser(String renderedHtml) throws IOException {
        File tempFile = File.createTempFile("email-preview-", ".html");

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(renderedHtml);
        } catch (IOException e) {
            System.out.println("Não foi possivel criar o conteúdo do email");
        }

        // Open in browser
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(tempFile.toURI());
        } else {
            System.out.println("Desktop not supported. Open this file manually: " + tempFile.getAbsolutePath());
        }
    }

    public static String getEmailRecuperacaoSenhaContent(String token) {
        String email = "<!DOCTYPE html>\n" +
                "<html lang=\"pt-BR\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Redefinição de Senha</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            background-color: #f5f6fa;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .container {\n" +
                "            background-color: white;\n" +
                "            padding: 30px;\n" +
                "            border-radius: 10px;\n" +
                "            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);\n" +
                "            max-width: 500px;\n" +
                "            margin: auto;\n" +
                "        }\n" +
                "        .header {\n" +
                "            font-size: 22px;\n" +
                "            margin-bottom: 15px;\n" +
                "            color: #333;\n" +
                "        }\n" +
                "        .token {\n" +
                "            font-size: 26px;\n" +
                "            font-weight: bold;\n" +
                "            color: #007bff;\n" +
                "            text-align: center;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        .footer {\n" +
                "            font-size: 14px;\n" +
                "            color: #666;\n" +
                "            margin-top: 30px;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"container\">\n" +
                "    <div class=\"header\">Olá!</div>\n" +
                "    <p>Você solicitou a redefinição de senha da sua conta. Use o código abaixo para continuar o processo:</p>\n" +
                "\n" +
                "    <div class=\"token\">${token}</div>\n" +
                "\n" +
                "    <p>Se você não fez essa solicitação, ignore este e-mail.</p>\n" +
                "\n" +
                "    <div class=\"footer\">\n" +
                "        <p>Obrigado,<br>Sua Equipe de Suporte</p>\n" +
                "    </div>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>";

        return email.replace("${token}", token);
    }

    private static String getEmailReposicaoEstoque(List<Livro> livros) {
        String email = "<!DOCTYPE html>\n" +
                "<html lang=\"pt-BR\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Solicitação de Reposição de Livros</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            background-color: #f5f6fa;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .container {\n" +
                "            background-color: white;\n" +
                "            padding: 30px;\n" +
                "            border-radius: 10px;\n" +
                "            max-width: 700px;\n" +
                "            margin: auto;\n" +
                "            box-shadow: 0 0 10px rgba(0,0,0,0.1);\n" +
                "        }\n" +
                "        .header {\n" +
                "            font-size: 20px;\n" +
                "            margin-bottom: 20px;\n" +
                "            color: #333;\n" +
                "        }\n" +
                "        table {\n" +
                "            width: 100%;\n" +
                "            border-collapse: collapse;\n" +
                "            margin-top: 15px;\n" +
                "        }\n" +
                "        th, td {\n" +
                "            border: 1px solid #ccc;\n" +
                "            padding: 8px 12px;\n" +
                "            text-align: left;\n" +
                "        }\n" +
                "        th {\n" +
                "            background-color: #eaeaea;\n" +
                "        }\n" +
                "        .footer {\n" +
                "            font-size: 14px;\n" +
                "            color: #555;\n" +
                "            margin-top: 30px;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">Solicitação de Reposição de Livros</div>\n" +
                "        <p>Prezados,</p>\n" +
                "        <p>Segue abaixo a lista de livros que necessitam de reposição em nosso estoque:</p>\n" +
                "\n" +
                "        ${livros}\n" +
                "\n" +
                "        <p>Solicitamos a gentileza de verificar a possibilidade de envio dos exemplares listados o quanto antes.</p>\n" +
                "\n" +
                "        <div class=\"footer\">\n" +
                "            <p>Atenciosamente,<br>\n" +
                "            Equipe da Biblioteca</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>\n";

        StringBuilder listaLivros = new StringBuilder("<ul>\n");
        livros.forEach(livro -> {
            listaLivros
                .append("<li>")
                .append(livro.getTitulo())
                .append("</li>")
                .append("\n");
        });
        listaLivros.append("</ul>");

        return email.replace("${livros}", listaLivros.toString());
    }
}
