package es.dam.ousama.modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

// Entidad principal que maneja todo el estado del jugador
// Aquí guardamos su dinero, sus plantas y las configuraciones del temporizador
public class Estudiante {
    // Stats básicas
    private String nombre;
    private int puntosCrecimiento;
    private int monedasXP;
    private int gotasAgua;

    // Configuración del temporizador Pomodoro (valores por defecto)
    private int metaDiariaMinutos = 60;
    private int minutosEstudio = 25;
    private int minutosDescanso = 5;

    // Variables para controlar si el usuario entra todos los días
    private int rachaDiasEstudio;
    private LocalDate ultimoDiaEstudioMeta;

    // Mochila y tablero de juego (array fijo de 9 casillas)
    private Map<String, Integer> inventario;
    private Planta[] miJardinNuevo;
    private String ultimaRecoleccion;

    // Booster de la tienda
    private LocalDate finMusicaClasica;

    // Guardamos las fechas de las misiones para que el usuario no farmee oro reiniciando la app
    private String ultimaFechaMisionMeta;
    private String ultimaFechaMisionRiego;

    // Constructor para cuando se registra un usuario nuevo
    // Lo inicializamos todo a 0, pero le regalamos 10 gotas de agua para que no empiece pelado
    public Estudiante(String nombre) {
        this.nombre = nombre;
        this.puntosCrecimiento = 0;
        this.monedasXP = 0;
        this.gotasAgua = 10;
        this.rachaDiasEstudio = 0;
        this.ultimoDiaEstudioMeta = LocalDate.now().minusDays(1);
        this.inventario = new HashMap<>();
        this.ultimaRecoleccion = LocalDate.now().toString();

        // Ponemos una fecha ridícula en el pasado para asegurar que el primer día siempre cobre
        this.ultimaFechaMisionMeta = "2000-01-01";
        this.ultimaFechaMisionRiego = "2000-01-01";

        // Rellenamos el jardín con 9 macetas vacías de golpe para evitar NullPointerExceptions
        this.miJardinNuevo = new Planta[9];
        for (int i = 0; i < 9; i++) {
            this.miJardinNuevo[i] = new Planta();
        }
    }

    // Comprueba si ha pasado exactamente 1 día desde su última meta. Si no, le rompe la racha.
    public void registrarMetaCumplida() {
        if (ChronoUnit.DAYS.between(ultimoDiaEstudioMeta, LocalDate.now()) == 1) {
            rachaDiasEstudio++;
        } else if (ChronoUnit.DAYS.between(ultimoDiaEstudioMeta, LocalDate.now()) > 1) {
            rachaDiasEstudio = 1;
        }
        ultimoDiaEstudioMeta = LocalDate.now();
    }

    // Se ejecuta cada vez que el reloj descuenta 60 segundos de estudio
    public void sumarMinutoEstudio() {
        double multiXP = 1.0;

        // Recorremos el jardín. Si tiene un Pino Legendario sano da un bonus, pero si lo deja morir castiga la XP
        for (Planta p : miJardinNuevo) {
            if (p.getTipo().equals("Pino") && p.getEstadoSupervivencia().equals("Óptimo")) {
                multiXP = 1.5;
                break;
            } else if (p.getTipo().equals("Pino") && p.getEstadoSupervivencia().equals("Marchito")) {
                multiXP = 0.5;
            }
        }

        // Ojo: Multiplicamos primero y pasamos a int después, si no Java se come los decimales y nos da 0
        int xpGanada = (int) Math.round(1 * multiXP);
        if (xpGanada < 1) xpGanada = 1; // Mínimo siempre 1 punto

        this.puntosCrecimiento += xpGanada;
        this.gotasAgua += 1;
    }

    // Métodos de economía básica. Devuelven false si intenta comprar o regar sin saldo
    public void sumarMonedas(int cantidad) { this.monedasXP += cantidad; }
    public boolean gastarMonedas(int cantidad) {
        if (monedasXP >= cantidad) { monedasXP -= cantidad; return true; }
        return false;
    }
    public boolean gastarAgua(int cantidad) {
        if (gotasAgua >= cantidad) { gotasAgua -= cantidad; return true; }
        return false;
    }

    // Gestiona el mapa del inventario. Si un item llega a 0 lo borramos por completo de la BD
    public void añadirItem(String tipo) { inventario.put(tipo, inventario.getOrDefault(tipo, 0) + 1); }
    public void quitarItem(String tipo) {
        if (inventario.containsKey(tipo) && inventario.get(tipo) > 0) {
            inventario.put(tipo, inventario.get(tipo) - 1);
            if (inventario.get(tipo) == 0) inventario.remove(tipo);
        }
    }

    public void activarMusicaClasica() { this.finMusicaClasica = LocalDate.now().plusDays(1); }
    public boolean tieneMusicaActiva() { return finMusicaClasica != null && !LocalDate.now().isAfter(finMusicaClasica); }

    // Sistema de subida de nivel dinámico.
    // El bucle va restando XP hasta ver en qué nivel se queda. Cada fase cuesta 100 más que la anterior
    public int getNivel() {
        int nivel = 1; int xpRestante = puntosCrecimiento; int costeFase = 100;
        while (xpRestante >= costeFase) { xpRestante -= costeFase; nivel++; costeFase = nivel * 100; }
        return nivel;
    }

    // Saca la experiencia suelta que tiene en su nivel actual (para dibujar la barra amarilla)
    public int getXpActualEnNivel() {
        int nivel = 1; int xpRestante = puntosCrecimiento; int costeFase = 100;
        while (xpRestante >= costeFase) { xpRestante -= costeFase; nivel++; costeFase = nivel * 100; }
        return xpRestante;
    }
    public int getXpParaSiguienteNivel() { return getNivel() * 100; }

    // ================= GETTERS Y SETTERS BÁSICOS =================
    public String getNombre() { return nombre; }
    public int getPuntosCrecimiento() { return puntosCrecimiento; }
    public void setPuntosCrecimiento(int puntos) { this.puntosCrecimiento = puntos; }
    public int getMonedasXP() { return monedasXP; }
    public void setMonedasXP(int monedas) { this.monedasXP = monedas; }
    public int getGotasAgua() { return gotasAgua; }
    public void setGotasAgua(int gotas) { this.gotasAgua = gotas; }
    public int getMetaDiariaMinutos() { return metaDiariaMinutos; }
    public void setMetaDiariaMinutos(int meta) { this.metaDiariaMinutos = meta; }
    public int getMinutosEstudio() { return minutosEstudio; }
    public void setMinutosEstudio(int min) { this.minutosEstudio = min; }
    public int getMinutosDescanso() { return minutosDescanso; }
    public void setMinutosDescanso(int min) { this.minutosDescanso = min; }
    public Map<String, Integer> getInventarioSemillas() { return inventario; }
    public void setInventarioSemillas(Map<String, Integer> inventario) { this.inventario = inventario; }
    public Planta[] getMiJardinNuevo() { return miJardinNuevo; }
    public void setMiJardinNuevo(Planta[] jardin) { this.miJardinNuevo = jardin; }
    public String getUltimaRecoleccion() { return ultimaRecoleccion; }
    public void setUltimaRecoleccion(String ultima) { this.ultimaRecoleccion = ultima; }
    public int getRachaDiasEstudio() { return rachaDiasEstudio; }

    public String getUltimaFechaMisionMeta() { return ultimaFechaMisionMeta != null ? ultimaFechaMisionMeta : "2000-01-01"; }
    public void setUltimaFechaMisionMeta(String d) { this.ultimaFechaMisionMeta = d; }
    public String getUltimaFechaMisionRiego() { return ultimaFechaMisionRiego != null ? ultimaFechaMisionRiego : "2000-01-01"; }
    public void setUltimaFechaMisionRiego(String d) { this.ultimaFechaMisionRiego = d; }
}