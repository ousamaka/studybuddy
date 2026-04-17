package es.dam.ousama.modelo;

public class Estudiante {
    private String nombre;
    private int puntosCrecimiento;

    // Configuraciones persistentes
    private int metaDiariaMinutos = 60;
    private int minutosEstudio = 25;
    private int minutosDescanso = 5;

    public Estudiante(String nombre) {
        this.nombre = nombre;
        this.puntosCrecimiento = 0;
    }

    public void sumarPuntos(int puntos) { this.puntosCrecimiento += puntos; }

    // Lógica Gamificación: Cada 500 XP sube 1 nivel
    public int getNivel() { return (puntosCrecimiento / 500) + 1; }

    // Getters y Setters
    public String getNombre() { return nombre; }
    public int getPuntosCrecimiento() { return puntosCrecimiento; }
    public void setPuntosCrecimiento(int puntos) { this.puntosCrecimiento = puntos; }

    public int getMetaDiariaMinutos() { return metaDiariaMinutos; }
    public void setMetaDiariaMinutos(int meta) { this.metaDiariaMinutos = meta; }

    public int getMinutosEstudio() { return minutosEstudio; }
    public void setMinutosEstudio(int min) { this.minutosEstudio = min; }

    public int getMinutosDescanso() { return minutosDescanso; }
    public void setMinutosDescanso(int min) { this.minutosDescanso = min; }
}