package com.itson.carritogrpc;

import com.tienda.grpc.CarritoRequest;
import com.tienda.grpc.CarritoResponse;
import com.tienda.grpc.CarritoServiceGrpc.CarritoServiceImplBase;
import com.tienda.grpc.CatalogoResponse;
import com.tienda.grpc.Empty;
import com.tienda.grpc.Producto;
import io.grpc.stub.StreamObserver;

import java.util.List;

/**
 * Implementación del servicio gRPC CarritoService.
 * Gestiona la entrega del catálogo y el procesamiento de compras de forma thread-safe.
 */
public class CarritoServiceImpl extends CarritoServiceImplBase {

    private final InventarioManager inventarioManager;
    private final ServidorGUI gui;

    public CarritoServiceImpl(InventarioManager inventarioManager, ServidorGUI gui) {
        this.inventarioManager = inventarioManager;
        this.gui = gui;
    }

    /**
     * Retorna todos los productos registrados (ID, nombre, precio) sin importar si su stock es 0
     * y sin exponer el stock real.
     */
    @Override
    public void obtenerCatalogo(Empty request, StreamObserver<CatalogoResponse> responseObserver) {
        if (gui != null) {
            gui.agregarLog("Solicitud de catálogo recibida de un cliente.");
        }

        List<Producto> catalogo = inventarioManager.obtenerCatalogo();
        CatalogoResponse response = CatalogoResponse.newBuilder()
                .addAllProductos(catalogo)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        if (gui != null) {
            gui.agregarLog("Catálogo enviado con éxito (" + catalogo.size() + " productos).");
        }
    }

    /**
     * Procesa la compra de forma atómica y sincronizada (synchronized) para admitir
     * múltiples clientes concurrentes sin condiciones de carrera.
     */
    @Override
    public synchronized void procesarCarrito(CarritoRequest request, StreamObserver<CarritoResponse> responseObserver) {
        String usuario = (request.getUsuarioId() == null || request.getUsuarioId().isEmpty())
                ? "Anónimo"
                : request.getUsuarioId();

        if (gui != null) {
            gui.agregarLog("Petición de procesamiento de carrito recibida para usuario: '"
                    + usuario + "' con " + request.getItemsCount() + " ítems.");
        }

        // Procesamiento atómico en el gestor de inventario
        CarritoResponse response = inventarioManager.procesarCarrito(request);

        if ("EXITOSO".equals(response.getEstado())) {
            if (gui != null) {
                gui.agregarLog("✔ ORDEN EXITOSA [" + response.getTransaccionId() + "] "
                        + "Total: $" + String.format("%.2f", response.getTotalPagar())
                        + " (Neto: $" + String.format("%.2f", response.getTotalNeto())
                        + ", IVA: $" + String.format("%.2f", response.getImpuestos()) + ")");
                // Actualizar la tabla del servidor en tiempo real en el EDT
                gui.actualizarTabla(inventarioManager.getListaInventario());
            }
        } else {
            if (gui != null) {
                gui.agregarLog("✖ ORDEN RECHAZADA: " + response.getMensaje());
            }
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
