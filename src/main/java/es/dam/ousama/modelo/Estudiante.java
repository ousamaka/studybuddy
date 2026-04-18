package es.dam.ousama.modelo;

import java.util.HashMap;
import java.util.Map;

public class Estudiante {
    private String nombre;
    private int puntosCrecimiento;
    private int monedasXP; // Saldo para gastar en la tienda

    private int metaDiariaMinutos = 60;
    private int minutosEstudio = 25;
    private int minutosDescanso = 5;

    // INVENTARIO: Guarda el nombre de la semilla y la cantidad
    private Map<String, Integer> inventarioSemillas;

    public Estudiante(String nombre) {
        this.nombre = nombre;
        this.puntosCrecimiento = 0;
        this.monedasXP = 0;
        this.inventarioSemillas = new HashMap<>();
    }

    public void sumarPuntos(int puntos) {
        this.puntosCrecimiento += puntos;
        this.monedasXP += puntos; // Ganas monedas a la vez que subes de nivel
    }

    // Lógica de Tienda
    public boolean gastarMonedas(int cantidad) {
        if (monedasXP >= cantidad) {
            monedasXP -= cantidad;
            return true;
        }
        return false;
    }

    public void añadirSemilla(String tipo) {
        inventarioSemillas.put(tipo, inventarioSemillas.getOrDefault(tipo, 0) + 1);
    }

    public int getNivel() { return (puntosCrecimiento / 500) + 1; }

    // Getters y Setters
    public String getNombre() { return nombre; }

    public int getPuntosCrecimiento() { return puntosCrecimiento; }
    public void setPuntosCrecimiento(int puntos) { this.puntosCrecimiento = puntos; }

    public int getMonedasXP() { return monedasXP; }
    public void setMonedasXP(int monedas) { this.monedasXP = monedas; }

    public int getMetaDiariaMinutos() { return metaDiariaMinutos; }
    public void setMetaDiariaMinutos(int meta) { this.metaDiariaMinutos = meta; }

    public int getMinutosEstudio() { return minutosEstudio; }
    public void setMinutosEstudio(int min) { this.minutosEstudio = min; }

    public int getMinutosDescanso() { return minutosDescanso; }
    public void setMinutosDescanso(int min) { this.minutosDescanso = min; }

    public Map<String, Integer> getInventarioSemillas() { return inventarioSemillas; }
}