package es.dam.ousama.modelo;

public class Estudiante {
    private String nombre;
    private int puntosCrecimiento;

    public Estudiante(String nombre) {
        this.nombre = nombre;
        this.puntosCrecimiento = 0;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPuntosCrecimiento() {
        return puntosCrecimiento;
    }

    public void sumarPuntos(int puntos) {
        this.puntosCrecimiento += puntos;
    }
}