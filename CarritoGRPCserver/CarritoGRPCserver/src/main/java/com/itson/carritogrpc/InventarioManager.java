package com.itson.carritogrpc;

import com.tienda.grpc.CarritoRequest;
import com.tienda.grpc.CarritoResponse;
import com.tienda.grpc.Producto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestor del inventario en memoria thread-safe y procesamiento atómico de órdenes.
 */
public class InventarioManager {

    // Mapa sincronizado que preserva el orden de inserción de los productos
    private final Map<String, ItemInventario> inventario = Collections.synchronizedMap(new LinkedHashMap<>());

    public InventarioManager() {
        precargarInventario();
    }

    /**
     * Carga inicial de productos con ID, Nombre, Precio y Stock inicial.
     */
    private void precargarInventario() {
        agregarProducto(new ItemInventario("PROD-001", "Laptop Gamer Asus ROG 16GB", 24999.00, 5));
        agregarProducto(new ItemInventario("PROD-002", "Mouse Inalámbrico Logitech", 450.00, 15));
        agregarProducto(new ItemInventario("PROD-003", "Teclado Mecánico RGB Switch Red", 1250.00, 10));
        agregarProducto(new ItemInventario("PROD-004", "Monitor 27'' IPS 144Hz Full HD", 4800.00, 8));
        agregarProducto(new ItemInventario("PROD-005", "Auriculares HyperX Cloud II", 1100.00, 12));
        agregarProducto(new ItemInventario("PROD-006", "Disco SSD Kingston M.2 NVMe 1TB", 1350.00, 20));
        agregarProducto(new ItemInventario("PROD-007", "Memoria RAM Corsair 16GB DDR4", 750.00, 14));
        agregarProducto(new ItemInventario("PROD-008", "Cámara Web Logitech 1080p", 620.00, 0)); // Stock 0 para pruebas
    }

    public void agregarProducto(ItemInventario item) {
        inventario.put(item.getId(), item);
    }

    /**
     * Obtiene una copia de la lista del inventario para la GUI del servidor.
     */
    public synchronized List<ItemInventario> getListaInventario() {
        return new ArrayList<>(inventario.values());
    }

    /**
     * Retorna todos los productos registrados (ID, nombre, precio) sin importar si su stock es 0
     * y sin exponer el stock real (cantidad en 0).
     */
    public synchronized List<Producto> obtenerCatalogo() {
        List<Producto> catalogo = new ArrayList<>();
        for (ItemInventario item : inventario.values()) {
            Producto p = Producto.newBuilder()
                    .setId(item.getId())
                    .setNombre(item.getNombre())
                    .setPrecio(item.getPrecio())
                    .setCantidad(0) // No expone el stock real al cliente
                    .build();
            catalogo.add(p);
        }
        return catalogo;
    }

    /**
     * Procesa la compra de manera atómica (todo o nada) y sincronizada para soportar
     * múltiples clientes concurrentes de forma segura.
     */
    public synchronized CarritoResponse procesarCarrito(CarritoRequest request) {
        String usuario = (request.getUsuarioId() == null || request.getUsuarioId().trim().isEmpty())
                ? "Anónimo"
                : request.getUsuarioId().trim();

        // 1. Validar que el carrito no esté vacío
        if (request.getItemsCount() == 0) {
            return CarritoResponse.newBuilder()
                    .setTransaccionId("")
                    .setTotalNeto(0)
                    .setImpuestos(0)
                    .setTotalPagar(0)
                    .setEstado("ERROR")
                    .setMensaje("El carrito está vacío. Debe incluir al menos un producto para procesar la orden.")
                    .build();
        }

        // 2. Validar que cada ítem tenga cantidad > 0, precio > 0 y exista en el inventario
        for (Producto item : request.getItemsList()) {
            if (item.getCantidad() <= 0) {
                return CarritoResponse.newBuilder()
                        .setTransaccionId("")
                        .setTotalNeto(0)
                        .setImpuestos(0)
                        .setTotalPagar(0)
                        .setEstado("ERROR")
                        .setMensaje("El producto '" + item.getNombre() + "' (" + item.getId() + ") tiene una cantidad inválida (" + item.getCantidad() + "). La cantidad debe ser mayor a 0.")
                        .build();
            }

            if (item.getPrecio() <= 0) {
                return CarritoResponse.newBuilder()
                        .setTransaccionId("")
                        .setTotalNeto(0)
                        .setImpuestos(0)
                        .setTotalPagar(0)
                        .setEstado("ERROR")
                        .setMensaje("El producto '" + item.getNombre() + "' (" + item.getId() + ") tiene un precio inválido ($" + item.getPrecio() + "). El precio debe ser mayor a 0.")
                        .build();
            }

            if (!inventario.containsKey(item.getId())) {
                return CarritoResponse.newBuilder()
                        .setTransaccionId("")
                        .setTotalNeto(0)
                        .setImpuestos(0)
                        .setTotalPagar(0)
                        .setEstado("ERROR")
                        .setMensaje("El producto con ID '" + item.getId() + "' no existe en el catálogo del servidor.")
                        .build();
            }
        }

        // 3. Agrupar cantidades en caso de que un ID se repita en la lista del carrito
        Map<String, Integer> cantidadesAgrupadas = new LinkedHashMap<>();
        for (Producto item : request.getItemsList()) {
            cantidadesAgrupadas.merge(item.getId(), item.getCantidad(), Integer::sum);
        }

        // 4. Atomicidad: Verificar PRIMERO que exista stock suficiente para TODOS los productos
        for (Map.Entry<String, Integer> entry : cantidadesAgrupadas.entrySet()) {
            String prodId = entry.getKey();
            int cantidadSolicitada = entry.getValue();
            ItemInventario itemInventario = inventario.get(prodId);

            if (itemInventario.getStock() < cantidadSolicitada) {
                // Cancelar TODA la orden sin descontar nada
                return CarritoResponse.newBuilder()
                        .setTransaccionId("")
                        .setTotalNeto(0)
                        .setImpuestos(0)
                        .setTotalPagar(0)
                        .setEstado("ERROR")
                        .setMensaje("Stock insuficiente para el producto '" + itemInventario.getNombre()
                                + "' (ID: " + prodId + "). Stock disponible: " + itemInventario.getStock()
                                + ", Cantidad requerida: " + cantidadSolicitada + ". La orden ha sido cancelada en su totalidad.")
                        .build();
            }
        }

        // 5. Si todo es válido: Descontar del inventario
        for (Map.Entry<String, Integer> entry : cantidadesAgrupadas.entrySet()) {
            String prodId = entry.getKey();
            int cantidadSolicitada = entry.getValue();
            ItemInventario itemInventario = inventario.get(prodId);
            itemInventario.descontarStock(cantidadSolicitada);
        }

        // 6. Calcular subtotal, impuestos (16%) y total
        double subtotal = 0.0;
        for (Producto item : request.getItemsList()) {
            subtotal += item.getPrecio() * item.getCantidad();
        }
        double impuestos = subtotal * 0.16;
        double total = subtotal + impuestos;
        String transaccionId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return CarritoResponse.newBuilder()
                .setTransaccionId(transaccionId)
                .setTotalNeto(subtotal)
                .setImpuestos(impuestos)
                .setTotalPagar(total)
                .setEstado("EXITOSO")
                .setMensaje("Compra realizada con éxito para el usuario '" + usuario + "'. Se procesaron "
                        + request.getItemsCount() + " ítems (Unidades totales: "
                        + cantidadesAgrupadas.values().stream().mapToInt(Integer::intValue).sum() + ").")
                .build();
    }
}

