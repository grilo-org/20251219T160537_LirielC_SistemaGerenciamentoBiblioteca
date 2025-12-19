package com.biblioteca.service;

import com.biblioteca.model.Venda;
import com.biblioteca.model.ItemVenda;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentoFiscalService {
    private static final String OUTPUT_PATH = "output/";
    private static final Logger log = LoggerFactory.getLogger(DocumentoFiscalService.class);

    static {
        // Cria diretório de saída se não existir
        new File(OUTPUT_PATH).mkdirs();
    }

    public static void gerarNotaFiscal(Venda venda) {
        try {
            // Prepara os parâmetros
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("NOTA_FISCAL_NUMERO", venda.getId());
            parametros.put("DATA_EMISSAO", venda.getDataVenda().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            parametros.put("CLIENTE_NOME", venda.getClienteNome());
            parametros.put("CLIENTE_CPF", venda.getClienteCpf());
            parametros.put("CLIENTE_EMAIL", venda.getClienteEmail());
            parametros.put("CLIENTE_ENDERECO", venda.getClienteEndereco());
            parametros.put("TIPO_PAGAMENTO", venda.getTipoPagamento().equals("card") ? "Cartão de Crédito" : "Boleto Bancário");
            parametros.put("TIPO_COMPRA", venda.getTipoCompra()!=null?venda.getTipoCompra():"COMPRA");
            if("ALUGUEL".equalsIgnoreCase(venda.getTipoCompra())){
                java.time.LocalDate limite = venda.getDataVenda().toLocalDate().plusDays(7);
                parametros.put("DATA_DEVOLUCAO", limite.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            parametros.put("VALOR_TOTAL", String.format("%.2f", venda.getValorTotal()));

            // Prepara os itens da nota
            List<Map<String, Object>> itens = new ArrayList<>();
            for (ItemVenda item : venda.getItens()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("codigo", item.getLivro().getId());
                itemMap.put("descricao", item.getLivro().getTitulo());
                itemMap.put("quantidade", item.getQuantidade());
                itemMap.put("valorUnitario", item.getValorUnitario());
                itemMap.put("valorTotal", item.getValorTotal());
                itens.add(itemMap);
            }

            // Cria o datasource
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(itens);

            // Carrega e compila o template usando ClassLoader
            InputStream templateStream = DocumentoFiscalService.class.getClassLoader().getResourceAsStream("relatorios/danfe_template.jrxml");
            if (templateStream == null) {
                throw new RuntimeException("Template DANFE não encontrado no classpath");
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

            // Preenche o relatório
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

            // Exporta para PDF
            String outputFile = OUTPUT_PATH + "NF_" + venda.getId() + ".pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, outputFile);

            log.info("Nota fiscal gerada com sucesso: {}", outputFile);
        } catch (Exception e) {
            log.error("Erro ao gerar nota fiscal", e);
        }
    }

    public static void gerarRecibo(Venda venda) {
        try {
            // Prepara os parâmetros
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("RECIBO_NUMERO", venda.getId());
            parametros.put("DATA_EMISSAO", venda.getDataVenda().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            parametros.put("CLIENTE_NOME", venda.getClienteNome());
            parametros.put("CLIENTE_CPF", venda.getClienteCpf());
            parametros.put("TIPO_PAGAMENTO", venda.getTipoPagamento());
            parametros.put("TIPO_COMPRA", venda.getTipoCompra()!=null?venda.getTipoCompra():"COMPRA");
            if("ALUGUEL".equalsIgnoreCase(venda.getTipoCompra())){
                java.time.LocalDate limite = venda.getDataVenda().toLocalDate().plusDays(7);
                parametros.put("DATA_DEVOLUCAO", limite.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            parametros.put("VALOR_TOTAL", String.format("%.2f", venda.getValorTotal()));
            parametros.put("VALOR_POR_EXTENSO", valorPorExtenso(venda.getValorTotal()));

            // Carrega e compila o template usando ClassLoader
            InputStream templateStream = DocumentoFiscalService.class.getClassLoader().getResourceAsStream("relatorios/recibo_template.jrxml");
            if (templateStream == null) {
                throw new RuntimeException("Template de recibo não encontrado no classpath");
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

            // Preenche o relatório
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, new JREmptyDataSource());

            // Exporta para PDF
            String outputFile = OUTPUT_PATH + "Recibo_" + venda.getId() + ".pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, outputFile);

            log.info("Recibo gerado com sucesso: {}", outputFile);
        } catch (Exception e) {
            log.error("Erro ao gerar recibo", e);
        }
    }

    private static String valorPorExtenso(double valor) {
        // Implementação simplificada - em um sistema real, usar uma biblioteca específica
        return String.format("%.2f", valor) + " reais";
    }
} 