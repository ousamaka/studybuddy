package es.dam.ousama.modelo;

public class Estudiante {
    private String nombre;
    private int puntosCrecimiento;
    private final int metaDiariaXP = 3600; // Meta de 1 hora (3600 segundos)

    public Estudiante(String nombre) {
        this.nombre = nombre;
        this.puntosCrecimiento = 0;
    }

    public void sumarPuntos(int puntos) {
        this.puntosCrecimiento += puntos;
    }

    public String getNombre() { return nombre; }
    public int getPuntosCrecimiento() { return puntosCrecimiento; }
    public void setPuntosCrecimiento(int puntos) { this.puntosCrecimiento = puntos; }
    public double getProgresoMeta() { return (double) puntosCrecimiento / metaDiariaXP; }
}