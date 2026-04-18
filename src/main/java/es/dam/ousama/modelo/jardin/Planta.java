package es.dam.ousama.modelo.jardin;

public class Planta {
    private String tipo; // "Cactus", "Bonsái", "Girasol"
    private int precioXP;
    private int xpActual; // XP que le has dedicado a esta planta en concreto
    private int xpParaCrecer; // Cuánta XP necesita para ser adulta
    private int fase; // 0: Semilla, 1: Brote, 2: Adulta

    public Planta(String tipo, int precioXP, int xpParaCrecer) {
        this.tipo = tipo;
        this.precioXP = precioXP;
        this.xpParaCrecer = xpParaCrecer;
        this.xpActual = 0;
        this.fase = 0; // Nace como semilla
    }

    // Lógica de evolución
    public void regar(int xpAñadida) {
        if (fase == 2) return; // Si ya es adulta, no crece más

        this.xpActual += xpAñadida;

        // Si llega a la mitad de su meta, es un brote. Si llega al final, es adulta.
        if (xpActual >= xpParaCrecer) {
            fase = 2; // Adulta
        } else if (xpActual >= xpParaCrecer / 2) {
            fase = 1; // Brote
        }
    }

    // Devuelve el Emoji correspondiente a su fase y tipo
    public String getIconoVisual() {
        if (fase == 0) return "🌱"; // Semilla (Igual para todas)
        if (fase == 1) return "🌿"; // Brote (Igual para todas)

        // Fase 2 (Adulta): Depende del tipo
        switch (tipo) {
            case "Cactus": return "🌵";
            case "Bonsái": return "🌳";
            case "Girasol": return "🌻";
            case "Pino": return "🌲";
            default: return "🪴";
        }
    }

    // Getters
    public String getTipo() { return tipo; }
    public int getPrecioXP() { return precioXP; }
    public int getXpActual() { return xpActual; }
    public int getXpParaCrecer() { return xpParaCrecer; }
    public int getFase() { return fase; }
}