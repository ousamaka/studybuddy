package es.dam.ousama.modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Representa una planta del huerto. Es como un mini Tamagotchi: tiene sed, crece y nos da pasta.
public class Planta {
    // Datos básicos (fase 0: semilla, 1: brote, 2: adulta, 3: tronco seco para el hospital)
    private String tipo;
    private int fase;
    private int hidratacion;
    private LocalDate fechaPlantacion;
    private LocalDate ultimaVezRegada;
    private LocalDate finToldoProtector; // Para saber cuándo caduca el booster de la tienda
    private int diasSeguidosOptimo;

    // Historial de Producción (El ROI de la planta para ver si ha rentado plantarla)
    private int oroTotalGenerado;
    private int aguaTotalGenerada;
    private int xpTotalGenerado;

    // Constructor por defecto. Si la maceta está libre, metemos esto para que no salten NullPointers
    public Planta() {
        this.tipo = "Vacía";
        this.fase = -1;
        this.hidratacion = 0;
        this.oroTotalGenerado = 0;
        this.aguaTotalGenerada = 0;
        this.xpTotalGenerado = 0;
    }

    // Constructor normal. Cuando compramos una semilla en la tienda empieza a tope de vida
    public Planta(String tipo) {
        this.tipo = tipo;
        this.fase = 0;
        this.hidratacion = 100;
        this.fechaPlantacion = LocalDate.now();
        this.ultimaVezRegada = LocalDate.now();
        this.diasSeguidosOptimo = 0;
        this.oroTotalGenerado = 0;
        this.aguaTotalGenerada = 0;
        this.xpTotalGenerado = 0;
    }

    // Lógica del Tamagotchi. Recibe la fecha por parámetro para poder simular días pasados si el usuario no ha abierto la app
    public String getEstadoSupervivencia(LocalDate fechaSimulada) {
        if (fase < 2 || tipo.equals("Vacía")) return "Creciendo"; // Las semillas no se secan
        if (tipo.equals("Cactus")) return "Óptimo"; // El cactus está chetado, es inmune

        // Si la BD nos trae un null por ser un registro viejo, usamos la fecha de hoy para salvar los muebles
        LocalDate fechaBase = this.ultimaVezRegada != null ? this.ultimaVezRegada : fechaSimulada;

        // Si el toldo se rompió mientras no estábamos, la sed empieza a contar desde el día que se rompió
        if (finToldoProtector != null && finToldoProtector.isAfter(fechaBase)) {
            if (fechaSimulada.isBefore(finToldoProtector) || fechaSimulada.isEqual(finToldoProtector)) {
                return "Óptimo";
            } else {
                fechaBase = finToldoProtector;
            }
        }

        long diasSinRegar = ChronoUnit.DAYS.between(fechaBase, fechaSimulada);

        if (diasSinRegar <= 0) return "Óptimo"; // Todo flama
        if (diasSinRegar == 1) return "Sediento"; // Ojo cuidado
        return "Marchito"; // RIP
    }

    // Sobrecarga por comodidad para cuando solo queremos saber cómo está la planta HOY
    public String getEstadoSupervivencia() {
        return getEstadoSupervivencia(LocalDate.now());
    }

    // Le echamos agua. Devuelve true si pasa a la siguiente fase (crece)
    public boolean regar(int gotas) {
        // Si es un tronco seco (fase 3), el riego normal no sirve, necesita pasar por el hospital
        if (fase == 3) return false;

        // Tope de agua a 100, no queremos ahogarla
        this.hidratacion = Math.min(100, this.hidratacion + (gotas * 20));
        this.ultimaVezRegada = LocalDate.now();

        if (fase < 2) {
            fase++;
            return true;
        }

        if (getEstadoSupervivencia().equals("Óptimo")) {
            diasSeguidosOptimo++; // Sumamos para posibles perks
        }
        return false;
    }

    // La guita. ¿Qué nos genera esta planta en la fecha que le pasamos?
    public int[] calcularProduccion(boolean tieneMusicaClasica, LocalDate fechaSimulada) {
        int[] recursos = new int[]{0, 0, 0};
        // Si no es adulta o está muerta ese día, no produce nada
        if (fase < 2 || getEstadoSupervivencia(fechaSimulada).equals("Marchito")) return recursos;

        // El booster de la caja de música da un 20% extra de gratis
        double multiplicador = tieneMusicaClasica ? 1.20 : 1.0;

        switch (tipo) {
            case "Cactus": recursos[0] = (int) (10 * multiplicador); break;
            case "Bonsái": recursos[0] = (int) (30 * multiplicador); recursos[2] = 10; break;
            case "Girasol": recursos[1] = 40; break; // Farmea agua, no oro. Está subido a 40 para que rente mantenerlo
            case "Pino": recursos[0] = (int) (100 * multiplicador); recursos[2] = 50; break;
        }
        return recursos;
    }

    // Sobrecarga
    public int[] calcularProduccion(boolean tieneMusicaClasica) {
        return calcularProduccion(tieneMusicaClasica, LocalDate.now());
    }

    // Vamos sumando lo que genera cada día para luego chulear en la Pokédex de la maceta
    public void registrarProduccion(int oro, int agua, int xp) {
        this.oroTotalGenerado += oro;
        this.aguaTotalGenerada += agua;
        this.xpTotalGenerado += xp;
    }

    // Congela la sed 3 días (ideal si el finde no abres la app)
    public void aplicarToldoProtector() {
        this.finToldoProtector = LocalDate.now().plusDays(3);
    }

    // ==========================================
    // GETTERS Y SETTERS BÁSICOS
    // ==========================================
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getFase() { return fase; }
    public void setFase(int fase) { this.fase = fase; }
    public int getHidratacion() { return hidratacion; }
    public void setHidratacion(int hidratacion) { this.hidratacion = hidratacion; }
    public LocalDate getUltimaVezRegada() { return ultimaVezRegada; }
    public void setUltimaVezRegada(LocalDate ultimaVezRegada) { this.ultimaVezRegada = ultimaVezRegada; }
    public LocalDate getFinToldoProtector() { return finToldoProtector; }
    public void setFinToldoProtector(LocalDate finToldoProtector) { this.finToldoProtector = finToldoProtector; }

    // Protección anti nulos para que no salte el error de ChronoUnit si la BD viene sucia
    public LocalDate getFechaPlantacion() { return this.fechaPlantacion != null ? this.fechaPlantacion : LocalDate.now(); }
    public void setFechaPlantacion(LocalDate fechaPlantacion) { this.fechaPlantacion = fechaPlantacion; }

    public int getDiasSeguidosOptimo() { return diasSeguidosOptimo; }
    public void resetDiasOptimo() { this.diasSeguidosOptimo = 0; }
    public int getOroTotalGenerado() { return oroTotalGenerado; }
    public void setOroTotalGenerado(int oroTotalGenerado) { this.oroTotalGenerado = oroTotalGenerado; }
    public int getAguaTotalGenerada() { return aguaTotalGenerada; }
    public void setAguaTotalGenerada(int aguaTotalGenerada) { this.aguaTotalGenerada = aguaTotalGenerada; }
    public int getXpTotalGenerado() { return xpTotalGenerado; }
    public void setXpTotalGenerado(int xpTotalGenerado) { this.xpTotalGenerado = xpTotalGenerado; }
}