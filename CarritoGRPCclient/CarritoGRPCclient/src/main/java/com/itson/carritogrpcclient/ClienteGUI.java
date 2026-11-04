package com.itson.carritogrpcclient;

import com.tienda.grpc.CarritoRequest;
import com.tienda.grpc.CarritoResponse;
import com.tienda.grpc.CarritoServiceGrpc;
import com.tienda.grpc.CatalogoResponse;
import com.tienda.grpc.Empty;
import com.tienda.grpc.Producto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Interfaz gráfica Swing para el Cliente gRPC.
 * Mantiene abierto el ManagedChannel y el BlockingStub durante el ciclo de vida de la ventana.
 */
public class ClienteGUI extends JFrame {

    private final String host = "localhost";
    private final int port = 50051;

    private ManagedChannel channel;
    private CarritoServiceGrpc.CarritoServiceBlockingStub stub;

    private JTextField txtUsuario;
    private JTable tablaCatalogo;
    private DefaultTableModel modeloCatalogo;

    private JTable tablaCarrito;
    private DefaultTableModel modeloCarrito;

    private JSpinner spinnerCantidad;
    private JLabel lblSubtotalValor;
    private JLabel lblImpuestosValor;
    private JLabel lblTotalValor;

    // Lista en memoria de productos del carrito local
    private final List<Producto> itemsCarrito = new ArrayList<>();

    public ClienteGUI() {
        iniciarConexionGRPC();
        initComponents();
        cargarCatalogo();
    }

    /**
     * Inicializa el ManagedChannel y el stub bloqueante de gRPC.
     */
    private void iniciarConexionGRPC() {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = CarritoServiceGrpc.newBlockingStub(channel);
    }

    private void initComponents() {
        String randomUserId = "CLI-" + (100 + new Random().nextInt(900));
        setTitle("Cliente Carrito de Compras gRPC (" + randomUserId + ")");
        setSize(950, 700);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Al cerrar la ventana liberamos los recursos del canal gRPC
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (channel != null && !channel.isShutdown()) {
                    channel.shutdown();
                }
            }
        });

        JPanel panelContenedor = new JPanel(new BorderLayout(10, 10));
        panelContenedor.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // 1. PANEL SUPERIOR: Usuario y Estado de Conexión
        JPanel panelTop = new JPanel(new BorderLayout(10, 10));
        panelTop.setBackground(new Color(240, 243, 248));
        panelTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 235), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JPanel panelUsuario = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelUsuario.setOpaque(false);
        JLabel lblUsuario = new JLabel("ID de Usuario:");
        lblUsuario.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtUsuario = new JTextField(randomUserId, 12);
        txtUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton btnRefrescarCatalogo = new JButton("🔄 Recargar Catálogo");
        btnRefrescarCatalogo.addActionListener(e -> cargarCatalogo());

        panelUsuario.add(lblUsuario);
        panelUsuario.add(txtUsuario);
        panelUsuario.add(btnRefrescarCatalogo);

        JLabel lblHost = new JLabel("Servidor: " + host + ":" + port + " (Conectado)");
        lblHost.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHost.setForeground(new Color(40, 120, 50));

        panelTop.add(panelUsuario, BorderLayout.WEST);
        panelTop.add(lblHost, BorderLayout.EAST);
        panelContenedor.add(panelTop, BorderLayout.NORTH);

        // 2. TABLA CATÁLOGO
        String[] colCatalogo = {"ID", "Nombre del Producto", "Precio Unitario ($)"};
        modeloCatalogo = new DefaultTableModel(colCatalogo, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaCatalogo = new JTable(modeloCatalogo);
        tablaCatalogo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaCatalogo.setRowHeight(24);
        tablaCatalogo.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(JLabel.CENTER);
        tablaCatalogo.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(JLabel.RIGHT);
        tablaCatalogo.getColumnModel().getColumn(2).setCellRenderer(rightRender);

        tablaCatalogo.getColumnModel().getColumn(0).setPreferredWidth(90);
        tablaCatalogo.getColumnModel().getColumn(1).setPreferredWidth(300);
        tablaCatalogo.getColumnModel().getColumn(2).setPreferredWidth(120);

        JScrollPane scrollCatalogo = new JScrollPane(tablaCatalogo);
        scrollCatalogo.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                " Catálogo de Productos Disponibles ",
                0, 0, new Font("Segoe UI", Font.BOLD, 13), new Color(30, 60, 110)));

        // Panel de acciones del catálogo (seleccionar y agregar al carrito)
        JPanel panelAccionCatalogo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panelAccionCatalogo.add(new JLabel("Cantidad:"));
        spinnerCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spinnerCantidad.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spinnerCantidad.setPreferredSize(new Dimension(70, 26));
        panelAccionCatalogo.add(spinnerCantidad);

        JButton btnAgregar = new JButton("➕ Agregar al Carrito");
        btnAgregar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAgregar.setBackground(new Color(40, 120, 200));
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.addActionListener(e -> agregarProductoAlCarrito());
        panelAccionCatalogo.add(btnAgregar);

        JPanel panelSeccionCatalogo = new JPanel(new BorderLayout());
        panelSeccionCatalogo.add(scrollCatalogo, BorderLayout.CENTER);
        panelSeccionCatalogo.add(panelAccionCatalogo, BorderLayout.SOUTH);

        // 3. TABLA DEL CARRITO LOCAL
        String[] colCarrito = {"ID", "Nombre", "Precio Unitario ($)", "Cantidad", "Subtotal ($)"};
        modeloCarrito = new DefaultTableModel(colCarrito, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaCarrito = new JTable(modeloCarrito);
        tablaCarrito.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaCarrito.setRowHeight(24);
        tablaCarrito.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        tablaCarrito.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        tablaCarrito.getColumnModel().getColumn(3).setCellRenderer(centerRender);
        tablaCarrito.getColumnModel().getColumn(2).setCellRenderer(rightRender);
        tablaCarrito.getColumnModel().getColumn(4).setCellRenderer(rightRender);

        tablaCarrito.getColumnModel().getColumn(0).setPreferredWidth(90);
        tablaCarrito.getColumnModel().getColumn(1).setPreferredWidth(260);
        tablaCarrito.getColumnModel().getColumn(2).setPreferredWidth(110);
        tablaCarrito.getColumnModel().getColumn(3).setPreferredWidth(80);
        tablaCarrito.getColumnModel().getColumn(4).setPreferredWidth(110);

        JScrollPane scrollCarrito = new JScrollPane(tablaCarrito);
        scrollCarrito.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                " Mi Carrito de Compras Local ",
                0, 0, new Font("Segoe UI", Font.BOLD, 13), new Color(30, 60, 110)));

        // Botones de acciones del carrito
        JPanel panelAccionesCarrito = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton btnEliminarItem = new JButton("🗑️ Quitar Ítem");
        btnEliminarItem.addActionListener(e -> eliminarItemCarrito());

        JButton btnVaciarCarrito = new JButton("🧹 Vaciar Carrito");
        btnVaciarCarrito.addActionListener(e -> vaciarCarrito());

        panelAccionesCarrito.add(btnEliminarItem);
        panelAccionesCarrito.add(btnVaciarCarrito);

        JPanel panelSeccionCarrito = new JPanel(new BorderLayout());
        panelSeccionCarrito.add(scrollCarrito, BorderLayout.CENTER);
        panelSeccionCarrito.add(panelAccionesCarrito, BorderLayout.SOUTH);

        // Split pane central
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSeccionCatalogo, panelSeccionCarrito);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(6);
        panelContenedor.add(splitPane, BorderLayout.CENTER);

        // 4. PANEL INFERIOR: RESUMEN Y BOTÓN COMPRAR
        JPanel panelInferior = new JPanel(new BorderLayout(15, 10));
        panelInferior.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 10, 5, 10)
        ));

        // Subpanel con totales
        JPanel panelTotales = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 10, 2, 10);
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridx = 0; gbc.gridy = 0;
        panelTotales.add(new JLabel("Subtotal:"), gbc);
        gbc.gridx = 1;
        lblSubtotalValor = new JLabel("$ 0.00");
        lblSubtotalValor.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panelTotales.add(lblSubtotalValor, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panelTotales.add(new JLabel("IVA (16%):"), gbc);
        gbc.gridx = 1;
        lblImpuestosValor = new JLabel("$ 0.00");
        lblImpuestosValor.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panelTotales.add(lblImpuestosValor, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel lblTotal = new JLabel("TOTAL ESTIMADO:");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        panelTotales.add(lblTotal, gbc);
        gbc.gridx = 1;
        lblTotalValor = new JLabel("$ 0.00");
        lblTotalValor.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotalValor.setForeground(new Color(25, 120, 45));
        panelTotales.add(lblTotalValor, gbc);

        // Botón Comprar
        JButton btnComprar = new JButton("💳 REALIZAR COMPRA");
        btnComprar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnComprar.setBackground(new Color(30, 145, 60));
        btnComprar.setForeground(Color.WHITE);
        btnComprar.setPreferredSize(new Dimension(220, 50));
        btnComprar.addActionListener(e -> enviarCarritoAlServidor());

        panelInferior.add(panelTotales, BorderLayout.CENTER);
        panelInferior.add(btnComprar, BorderLayout.EAST);

        panelContenedor.add(panelInferior, BorderLayout.SOUTH);
        setContentPane(panelContenedor);
    }

    /**
     * Llama al RPC obtenerCatalogo y actualiza la tabla de productos disponibles.
     */
    public void cargarCatalogo() {
        try {
            CatalogoResponse response = stub.obtenerCatalogo(Empty.getDefaultInstance());
            modeloCatalogo.setRowCount(0);
            for (Producto p : response.getProductosList()) {
                modeloCatalogo.addRow(new Object[]{
                        p.getId(),
                        p.getNombre(),
                        String.format("$ %,.2f", p.getPrecio())
                });
            }
        } catch (StatusRuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar con el servidor gRPC en " + host + ":" + port + ".\n"
                            + "Detalle: " + ex.getStatus().getDescription(),
                    "Error de Conexión",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error inesperado al obtener el catálogo: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Agrega el producto seleccionado de la tabla del catálogo al carrito local.
     */
    private void agregarProductoAlCarrito() {
        int selectedRow = tablaCatalogo.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un producto del catálogo para agregarlo al carrito.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = (String) modeloCatalogo.getValueAt(selectedRow, 0);
        String nombre = (String) modeloCatalogo.getValueAt(selectedRow, 1);
        String precioStr = ((String) modeloCatalogo.getValueAt(selectedRow, 2))
                .replace("$", "").replace(",", "").trim();
        double precio = Double.parseDouble(precioStr);
        int cantidad = (Integer) spinnerCantidad.getValue();

        // Si ya existe en el carrito local, agrupamos la cantidad
        boolean encontrado = false;
        for (int i = 0; i < itemsCarrito.size(); i++) {
            Producto item = itemsCarrito.get(i);
            if (item.getId().equals(id)) {
                int nuevaCantidad = item.getCantidad() + cantidad;
                itemsCarrito.set(i, item.toBuilder().setCantidad(nuevaCantidad).build());
                encontrado = true;
                break;
            }
        }

        if (!encontrado) {
            Producto nuevo = Producto.newBuilder()
                    .setId(id)
                    .setNombre(nombre)
                    .setPrecio(precio)
                    .setCantidad(cantidad)
                    .build();
            itemsCarrito.add(nuevo);
        }

        actualizarTablaCarrito();
        spinnerCantidad.setValue(1); // Resetear spinner
    }

    /**
     * Elimina el ítem seleccionado del carrito local.
     */
    private void eliminarItemCarrito() {
        int selectedRow = tablaCarrito.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un ítem del carrito para quitarlo.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        itemsCarrito.remove(selectedRow);
        actualizarTablaCarrito();
    }

    /**
     * Vacía todos los elementos del carrito local.
     */
    private void vaciarCarrito() {
        itemsCarrito.clear();
        actualizarTablaCarrito();
    }

    /**
     * Refresca la tabla del carrito local y recalcula los totales visuales estimados.
     */
    private void actualizarTablaCarrito() {
        modeloCarrito.setRowCount(0);
        double subtotal = 0.0;

        for (Producto item : itemsCarrito) {
            double itemSubtotal = item.getPrecio() * item.getCantidad();
            subtotal += itemSubtotal;
            modeloCarrito.addRow(new Object[]{
                    item.getId(),
                    item.getNombre(),
                    String.format("$ %,.2f", item.getPrecio()),
                    item.getCantidad(),
                    String.format("$ %,.2f", itemSubtotal)
            });
        }

        double impuestos = subtotal * 0.16;
        double total = subtotal + impuestos;

        lblSubtotalValor.setText(String.format("$ %,.2f", subtotal));
        lblImpuestosValor.setText(String.format("$ %,.2f", impuestos));
        lblTotalValor.setText(String.format("$ %,.2f", total));
    }

    /**
     * Envía la petición procesarCarrito al servidor gRPC, valida la respuesta
     * y muestra los resultados mediante diálogos Swing.
     */
    private void enviarCarritoAlServidor() {
        if (itemsCarrito.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "El carrito de compras está vacío. Agregue productos antes de realizar la compra.",
                    "Carrito Vacío",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String usuarioId = txtUsuario.getText().trim();
        if (usuarioId.isEmpty()) {
            usuarioId = "Cliente-Anonimo";
        }

        // Construir CarritoRequest con los productos actuales
        CarritoRequest.Builder requestBuilder = CarritoRequest.newBuilder()
                .setUsuarioId(usuarioId);

        for (Producto item : itemsCarrito) {
            requestBuilder.addItems(item);
        }

        CarritoRequest request = requestBuilder.build();

        try {
            // Llamada síncrona RPC al servidor
            CarritoResponse response = stub.procesarCarrito(request);

            if ("EXITOSO".equalsIgnoreCase(response.getEstado())) {
                // Compra exitosa: Mostrar JOptionPane con INFORMATION_MESSAGE y limpiar carrito
                String recibo = String.format(
                        "=========================================\n"
                                + "          ¡COMPRA EXITOSA!\n"
                                + "=========================================\n"
                                + "ID Transacción: %s\n"
                                + "Usuario:        %s\n"
                                + "-----------------------------------------\n"
                                + "Subtotal Neto:  $ %,.2f\n"
                                + "Impuestos (16%%):$ %,.2f\n"
                                + "TOTAL PAGADO:   $ %,.2f\n"
                                + "-----------------------------------------\n"
                                + "Estado:         %s\n"
                                + "Mensaje:        %s\n"
                                + "=========================================",
                        response.getTransaccionId(),
                        usuarioId,
                        response.getTotalNeto(),
                        response.getImpuestos(),
                        response.getTotalPagar(),
                        response.getEstado(),
                        response.getMensaje()
                );

                JOptionPane.showMessageDialog(this,
                        recibo,
                        "Transacción Completada",
                        JOptionPane.INFORMATION_MESSAGE);

                // Limpiar el carrito local tras la compra exitosa
                vaciarCarrito();

            } else {
                // Fallo del servidor (por ejemplo, falta de stock o datos inválidos)
                // Se muestra ERROR_MESSAGE y NO se borra el carrito local
                String errorMsg = String.format(
                        "La transacción no pudo ser procesada.\n\n"
                                + "Estado: %s\n"
                                + "Motivo: %s\n\n"
                                + "Su carrito se mantendrá intacto para que pueda ajustar las cantidades.",
                        response.getEstado(),
                        response.getMensaje()
                );

                JOptionPane.showMessageDialog(this,
                        errorMsg,
                        "Error en la Transacción",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (StatusRuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error de comunicación con el servidor gRPC:\n"
                            + ex.getStatus().getDescription() + " (" + ex.getStatus().getCode() + ")",
                    "Error gRPC",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Ocurrió un error inesperado al enviar el carrito:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}

