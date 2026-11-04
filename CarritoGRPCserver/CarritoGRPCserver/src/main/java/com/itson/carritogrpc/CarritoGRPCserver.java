package com.itson.carritogrpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;

/**
 * Punto de entrada principal para el Servidor gRPC.
 * Inicializa la interfaz gráfica Swing antes de levantar el servidor gRPC en el puerto 50051.
 */
public class CarritoGRPCserver {

    private static final int PUERTO = 50051;

    public static void main(String[] args) {
        // Establecer apariencia visual moderna según el sistema operativo
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Si falla Nimbus, se utiliza el L&F por defecto de Swing
        }

        // 1. Inicializar el inventario
        InventarioManager inventarioManager = new InventarioManager();

        // 2. Crear e iniciar la GUI del Servidor antes de levantar el socket gRPC
        ServidorGUI gui = new ServidorGUI();
        SwingUtilities.invokeLater(() -> {
            gui.setVisible(true);
            gui.actualizarTabla(inventarioManager.getListaInventario());
            gui.agregarLog("Iniciando componentes del servidor...");
        });

        // 3. Configurar y levantar el servidor gRPC en el puerto 50051
        Server server = ServerBuilder.forPort(PUERTO)
                .addService(new CarritoServiceImpl(inventarioManager, gui))
                .build();

        try {
            server.start();
            gui.agregarLog("Servidor gRPC iniciado correctamente y escuchando en el puerto " + PUERTO);
            System.out.println("Servidor de Carrito iniciado en el puerto " + PUERTO + "...");

            // Hook para apagado controlado al cerrar la aplicación
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Apagando servidor gRPC...");
                server.shutdown();
                System.out.println("Servidor gRPC detenido.");
            }));

            server.awaitTermination();
        } catch (IOException ex) {
            gui.agregarLog("ERROR al iniciar el servidor en el puerto " + PUERTO + ": " + ex.getMessage());
            System.getLogger(CarritoGRPCserver.class.getName())
                    .log(System.Logger.Level.ERROR, "No se pudo iniciar el servidor", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            gui.agregarLog("Servidor gRPC interrumpido.");
        }
    }
}
