package com.itson.carritogrpcclient;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada para la aplicación Cliente gRPC con interfaz Swing.
 * Permite ejecutar múltiples instancias del cliente al mismo tiempo.
 */
public class CarritoGRPCclient {

    public static void main(String[] args) {
        // Establecer apariencia Nimbus si está disponible
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Si no está disponible, Swing usará el L&F por defecto
        }

        // Iniciar la GUI en el Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            ClienteGUI clienteGUI = new ClienteGUI();
            clienteGUI.setVisible(true);
        });
    }
}
