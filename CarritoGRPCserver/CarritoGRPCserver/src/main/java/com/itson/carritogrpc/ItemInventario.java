package com.itson.carritogrpc;

/**
 * Representa un producto dentro del inventario del servidor con su stock disponible.
 */
public class ItemInventario {
    private final String id;
    private final String nombre;
    private final double precio;
    private int stock;

    public ItemInventario(String id, String nombre, double precio, int stock) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public synchronized int getStock() {
        return stock;
    }

    public synchronized void setStock(int stock) {
        this.stock = stock;
    }

    public synchronized void descontarStock(int cantidad) {
        if (cantidad <= this.stock) {
            this.stock -= cantidad;
        } else {
            throw new IllegalStateException("Stock insuficiente para descontar.");
        }
    }
}

