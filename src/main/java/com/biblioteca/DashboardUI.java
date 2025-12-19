package com.biblioteca;

import com.biblioteca.service.PagamentoService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardUI extends JFrame {
    private JTable vendasTable;
    private JLabel totalVendasLabel;
    private JLabel quantidadeVendasLabel;
    private JPanel graficosPanel;

    public DashboardUI() {
        setTitle("Dashboard de Vendas");
        setSize(1000, 800); 
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        
        JPanel metricasPanel = new JPanel(new GridLayout(1, 2));
        totalVendasLabel = new JLabel("Total de Vendas: R$ 0,00", SwingConstants.CENTER);
        quantidadeVendasLabel = new JLabel("Quantidade de Vendas: 0", SwingConstants.CENTER);
        metricasPanel.add(totalVendasLabel);
        metricasPanel.add(quantidadeVendasLabel);
        topPanel.add(metricasPanel, BorderLayout.CENTER);
        
        JButton refreshButton = new JButton("Atualizar Dados");
        refreshButton.addActionListener(e -> atualizarDados());
        topPanel.add(refreshButton, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);

        String[] colunas = {"Cliente", "CPF", "Valor", "Método", "Data", "Status"};
        DefaultTableModel modelo = new DefaultTableModel(colunas, 0);
        vendasTable = new JTable(modelo);
        JScrollPane scrollPane = new JScrollPane(vendasTable);
        add(scrollPane, BorderLayout.CENTER);

        graficosPanel = new JPanel(new GridLayout(2, 2)); 
        add(graficosPanel, BorderLayout.SOUTH);

        atualizarDados();
    }

    public void atualizarDados() {
        try {
            PagamentoService.atualizarHistorico();
            
            double totalVendas = PagamentoService.calcularTotalVendas();
            List<HashMap<String, Object>> historicoVendas = PagamentoService.getHistoricoVendas();
            HashMap<String, Integer> vendasPorTipo = PagamentoService.contarVendasPorTipoPagamento();
            
            if (historicoVendas == null) {
                throw new RuntimeException("Não foi possível carregar o histórico de vendas");
            }
            
            List<HashMap<String, Object>> vendasPagas = historicoVendas.stream()
                .filter(v -> "PAGO".equals(v.get("status")))
                .collect(Collectors.toList());
            
            totalVendasLabel.setText(String.format("Total de Vendas: R$ %.2f", totalVendas));
            quantidadeVendasLabel.setText("Quantidade de Vendas Pagas: " + vendasPagas.size());

            DefaultTableModel modelo = (DefaultTableModel) vendasTable.getModel();
            modelo.setRowCount(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            for (HashMap<String, Object> venda : historicoVendas) {
                try {
                    String cliente = venda.get("cliente") != null ? venda.get("cliente").toString() : "N/A";
                    String cpf = venda.get("cpf") != null ? venda.get("cpf").toString() : "N/A";
                    String valor = "R$ 0,00";
                    if (venda.get("valor") != null) {
                        Double valorDouble = (Double) venda.get("valor");
                        valor = String.format("R$ %.2f", valorDouble);
                    }
                    String tipoPagamento = venda.get("tipoPagamento") != null ? 
                        (venda.get("tipoPagamento").equals("card") ? "Cartão" : "Boleto") : "N/A";
                    String data = "N/A";
                    if (venda.get("data") != null) {
                        LocalDateTime dataVenda = (LocalDateTime) venda.get("data");
                        data = dataVenda.format(formatter);
                    }
                    String status = venda.get("status") != null ? venda.get("status").toString() : "N/A";

                    modelo.addRow(new Object[]{
                        cliente,
                        cpf,
                        valor,
                        tipoPagamento,
                        data,
                        status
                    });
                } catch (Exception e) {
                    System.out.println("Erro ao processar venda: " + e.getMessage());
                    e.printStackTrace();
                    // Adiciona uma linha com dados padrão para não quebrar a visualização
                    modelo.addRow(new Object[]{
                        "ERRO",
                        "ERRO",
                        "R$ 0,00",
                        "N/A",
                        "N/A",
                        "ERRO"
                    });
                }
            }

            graficosPanel.removeAll();
            
            JPanel pagamentoPanel = new JPanel();
            pagamentoPanel.setBorder(BorderFactory.createTitledBorder("Vendas por Tipo de Pagamento"));
            StringBuilder pagamentoText = new StringBuilder("<html>");
            for (String tipo : new String[]{"card", "boleto"}) {
                String tipoFormatado = tipo.equals("card") ? "Cartão" : "Boleto";
                int quantidade = vendasPorTipo.getOrDefault(tipo, 0);
                double valorTotal = vendasPagas.stream()
                    .filter(v -> tipo.equals(v.get("tipoPagamento")))
                    .filter(v -> v.get("valor") != null)
                    .mapToDouble(v -> ((Double)v.get("valor")))
                    .sum();
                
                pagamentoText.append(tipoFormatado)
                            .append(": ")
                            .append(quantidade)
                            .append(" vendas - R$ ")
                            .append(String.format("%.2f", valorTotal))
                            .append("<br>");
            }
            pagamentoText.append("</html>");
            pagamentoPanel.add(new JLabel(pagamentoText.toString()));
            graficosPanel.add(pagamentoPanel);

            JPanel metricasPanel = new JPanel();
            metricasPanel.setBorder(BorderFactory.createTitledBorder("Métricas Gerais"));
            StringBuilder metricasText = new StringBuilder("<html>");
            metricasText.append("Total de Vendas Pagas: R$ ")
                        .append(String.format("%.2f", totalVendas))
                        .append("<br>")
                        .append("Quantidade de Vendas Pagas: ")
                        .append(vendasPagas.size())
                        .append("<br>");
            
            if (!vendasPagas.isEmpty()) {
                metricasText.append("Ticket Médio: R$ ")
                            .append(String.format("%.2f", totalVendas / vendasPagas.size()))
                            .append("<br>");
            }
            
            metricasText.append("</html>");
            metricasPanel.add(new JLabel(metricasText.toString()));
            graficosPanel.add(metricasPanel);

            JPanel graficoVendasPanel = new JPanel();
            graficoVendasPanel.setBorder(BorderFactory.createTitledBorder("Gráfico de Vendas por Data"));
            graficoVendasPanel.setLayout(new BorderLayout());
            
            Map<String, Double> vendasPorData = vendasPagas.stream()
                .filter(v -> v.get("data") != null && v.get("valor") != null)
                .collect(Collectors.groupingBy(
                    v -> ((LocalDateTime)v.get("data")).format(DateTimeFormatter.ofPattern("dd/MM")),
                    Collectors.summingDouble(v -> ((Double)v.get("valor")))
                ));

            JPanel barrasPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int width = getWidth();
                    int height = getHeight();
                    int barWidth = width / (vendasPorData.size() + 1);
                    int maxHeight = height - 50;

                    double maxValue = vendasPorData.values().stream().mapToDouble(v -> v).max().orElse(0.0);
                    if (maxValue == 0.0) return;

                    int x = barWidth;
                    for (Map.Entry<String, Double> entry : vendasPorData.entrySet()) {
                        int barHeight = (int)((entry.getValue() / maxValue) * maxHeight);
                        g2d.setColor(new Color(70, 130, 180));
                        g2d.fillRect(x, height - barHeight - 30, barWidth - 10, barHeight);
                        
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(entry.getKey(), x, height - 10);
                        
                        String valor = String.format("R$%.0f", entry.getValue());
                        g2d.drawString(valor, x, height - barHeight - 35);
                        
                        x += barWidth;
                    }
                }
            };
            barrasPanel.setPreferredSize(new Dimension(400, 200));
            graficoVendasPanel.add(barrasPanel, BorderLayout.CENTER);
            graficosPanel.add(graficoVendasPanel);

            JPanel statusPanel = new JPanel();
            statusPanel.setBorder(BorderFactory.createTitledBorder("Status das Vendas"));
            Map<String, Long> vendasPorStatus = historicoVendas.stream()
                .filter(v -> v.get("status") != null)
                .collect(Collectors.groupingBy(
                    v -> (String)v.get("status"),
                    Collectors.counting()
                ));
            
            StringBuilder statusText = new StringBuilder("<html>");
            for (Map.Entry<String, Long> entry : vendasPorStatus.entrySet()) {
                statusText.append(entry.getKey())
                         .append(": ")
                         .append(entry.getValue())
                         .append(" vendas<br>");
            }
            statusText.append("</html>");
            statusPanel.add(new JLabel(statusText.toString()));
            graficosPanel.add(statusPanel);

            graficosPanel.revalidate();
            graficosPanel.repaint();
        } catch (Exception e) {
            System.out.println("Erro ao atualizar dashboard: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Erro ao carregar dados do dashboard: " + e.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void mostrarDashboard() {
        SwingUtilities.invokeLater(() -> {
            DashboardUI dashboard = new DashboardUI();
            dashboard.setLocationRelativeTo(null);
            dashboard.setVisible(true);
        });
    }
} 