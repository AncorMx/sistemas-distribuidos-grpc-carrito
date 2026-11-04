package com.itson.carritogrpc;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Interfaz gráfica Swing del Servidor gRPC que muestra el inventario en tiempo real
 * y el log de eventos de las peticiones recibidas.
 */
public class ServidorGUI extends JFrame {

    private JTable tablaInventario;
    private DefaultTableModel modeloTabla;
    private JTextArea areaLog;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ServidorGUI() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Servidor gRPC - Carrito de Compras (Puerto 50051)");
        setSize(850, 600);
        setMinimumSize(new Dimension(700, 450));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Panel Principal
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Encabezado
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(new Color(24, 43, 73));
        panelHeader.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel lblTitulo = new JLabel("PANEL DE CONTROL DEL SERVIDOR gRPC");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblEstado = new JLabel("● ESCUCHANDO EN PUERTO 50051");
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblEstado.setForeground(new Color(76, 175, 80));

        panelHeader.add(lblTitulo, BorderLayout.WEST);
        panelHeader.add(lblEstado, BorderLayout.EAST);
        panelPrincipal.add(panelHeader, BorderLayout.NORTH);

        // Tabla de Inventario
        String[] columnas = {"ID Producto", "Nombre del Producto", "Precio Unitario ($)", "Stock Disponible"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // No editable directamente desde la celda
            }
        };

        tablaInventario = new JTable(modeloTabla);
        tablaInventario.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaInventario.setRowHeight(26);
        tablaInventario.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tablaInventario.getTableHeader().setBackground(new Color(230, 235, 245));

        // Alineación y formato de celdas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tablaInventario.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tablaInventario.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        tablaInventario.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

        tablaInventario.getColumnModel().getColumn(0).setPreferredWidth(100);
        tablaInventario.getColumnModel().getColumn(1).setPreferredWidth(280);
        tablaInventario.getColumnModel().getColumn(2).setPreferredWidth(120);
        tablaInventario.getColumnModel().getColumn(3).setPreferredWidth(110);

        JScrollPane scrollTabla = new JScrollPane(tablaInventario);
        scrollTabla.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                " Inventario en Tiempo Real ",
                0, 0, new Font("Segoe UI", Font.BOLD, 14), new Color(24, 43, 73)));

        // Área de Logs
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setBackground(new Color(248, 249, 250));
        areaLog.setForeground(new Color(33, 37, 41));
        areaLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);

        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                " Registro de Eventos y Transacciones ",
                0, 0, new Font("Segoe UI", Font.BOLD, 14), new Color(24, 43, 73)));

        // SplitPane para dividir tabla y logs
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTabla, scrollLog);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(6);

        panelPrincipal.add(splitPane, BorderLayout.CENTER);
        setContentPane(panelPrincipal);
    }

    /**
     * Actualiza el JTable con los datos del inventario usando SwingUtilities.invokeLater.
     */
    public void actualizarTabla(List<ItemInventario> items) {
        SwingUtilities.invokeLater(() -> {
            modeloTabla.setRowCount(0);
            for (ItemInventario item : items) {
                modeloTabla.addRow(new Object[]{
                        item.getId(),
                        item.getNombre(),
                        String.format("$ %,.2f", item.getPrecio()),
                        item.getStock()
                });
            }
        });
    }

    /**
     * Agrega una línea con marca de tiempo al área de log usando SwingUtilities.invokeLater.
     */
    public void agregarLog(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            String hora = LocalTime.now().format(timeFormatter);
            areaLog.append("[" + hora + "] " + mensaje + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }
}

