package es.dam.ousama.controlador;

import es.dam.ousama.modelo.*;
import es.dam.ousama.modelo.dao.UsuarioDAO;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.util.Duration;
import org.bson.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Controlador principal de la ventana del Dashboard.
 * Gestiona el temporizador, la conexión con MongoDB, el sistema de estadísticas
 * y la lógica completa del minijuego del huerto.
 */
public class DashboardControlador {

    // --- UI: Cabecera y textos informativos ---
    @FXML private Label lblUser;
    @FXML private Label lblRango;
    @FXML private Label lblTiempo;
    @FXML private Label lblModo;
    @FXML private Label lblMetaTexto;
    @FXML private Label lblHoy;
    @FXML private Label lblSemana;
    @FXML private Label lblAsignaturaActiva;

    // --- UI: Pestaña de Perfil y Estadísticas ---
    @FXML private Label lblStatsNombre;
    @FXML private Label lblStatsRango;
    @FXML private Label lblPlantasVivas;
    @FXML private Label lblOroTotalStats;
    @FXML private Label lblRachaEstudio;

    // --- UI: Barras de progreso ---
    @FXML private ProgressBar progresoSesion;
    @FXML private ProgressBar progresoDiario;
    @FXML private ProgressBar progresoNivel;

    // --- UI: Botones del temporizador ---
    @FXML private Button btnAccion;
    @FXML private Button btnOmitir;
    @FXML private Button btnAbandonar;

    // --- UI: Configuración de tiempos ---
    @FXML private Spinner<Integer> spinEstudio;
    @FXML private Spinner<Integer> spinDescanso;
    @FXML private Spinner<Integer> spinMeta;

    // --- UI: Asignaturas y Gráficos ---
    @FXML private ListView<String> listaAsignaturas;
    @FXML private TextField txtNuevaAsignatura;
    @FXML private PieChart chartAsignaturas;
    @FXML private BarChart<String, Number> chartSemana;
    @FXML private Tab tabEstadisticas;
    @FXML private VBox tarjetaTemporizador;

    // --- UI: Jardín y Tienda ---
    @FXML private Label lblMonedas;
    @FXML private Label lblGotas;
    @FXML private Label lblNivelTienda;
    @FXML private ListView<String> listaInventario;
    @FXML private GridPane gridJardin;
    @FXML private VBox boxLigas;

    @FXML private Button btnCactus;
    @FXML private Button btnBonsai;
    @FXML private Button btnGirasol;
    @FXML private Button btnPino;

    @FXML private Button btnPala;
    @FXML private Button btnFertilizante;
    @FXML private Button btnToldo;
    @FXML private Label lblTotalXP;

    // --- UI: Misiones Diarias ---
    @FXML private CheckBox chkMision1;
    @FXML private CheckBox chkMision2;
    @FXML private Button btnReclamarM1;
    @FXML private Button btnReclamarM2;
    @FXML private Label lblXpSiguienteNivel;

    // Arrays para manejar el grid 3x3 del jardín de forma más cómoda
    private final Button[] botonesJardin = new Button[9];
    private final Tooltip[] tooltipsJardin = new Tooltip[9];

    // Variables de estado de la aplicación
    private Estudiante estudiante;
    private ObservableList<String> misAsignaturas;
    private Timeline timeline; // El motor del reloj
    private int tiempoRestante;
    private int tiempoTotalOriginal;
    private int segundosEstudiadosSesion = 0;
    private int minutosHoyLocales = 0;

    // Acumulamos los minutos aquí y guardamos de golpe al final. Si guardamos minuto a minuto bloqueamos la BD.
    private int minutosPendientesDeGuardar = 0;

    // Flags de control
    private boolean estudiando = false;
    private boolean esModoDescanso = false;
    private boolean bloqueandoLista = false;
    private String asignaturaActual = "Estudio Libre";

    private boolean misionMetaCompletada = false;
    private boolean misionRiegoCompletada = false;
    private boolean recompensaMetaReclamada = false;
    private boolean recompensaRiegoReclamada = false;

    /**
     * Se llama al pasar de la pantalla de Login al Dashboard.
     * Prepara la interfaz inicial.
     */
    public void initData(String u) {
        estudiante = new Estudiante(u);

        if (listaAsignaturas != null) {
            // Personalizamos las celdas para ponerle el candado a Estudio Libre
            listaAsignaturas.setCellFactory(lv -> new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        if (item.equals("Estudio Libre")) {
                            setText(" Estudio Libre");
                            setGraphic(new Label("🔒"));
                        } else {
                            setText(item);
                            setGraphic(null);
                        }
                    }
                }
            });

            misAsignaturas = FXCollections.observableArrayList();
            listaAsignaturas.setItems(misAsignaturas);

            // Listener para cuando cambian de asignatura
            listaAsignaturas.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (bloqueandoLista) return; // Evita bucles infinitos

                // Si están estudiando, bloqueamos el cambio para que no hagan trampas
                if ((estudiando || tiempoRestante < tiempoTotalOriginal) && !esModoDescanso) {
                    bloqueandoLista = true;
                    Platform.runLater(() -> {
                        listaAsignaturas.getSelectionModel().select(oldVal);
                        mostrarMensajeJuego("Sesión en curso", "No puedes cambiar de asignatura ahora.\nTermina o abandona la sesión.");
                        bloqueandoLista = false;
                    });
                    return;
                }
                asignaturaActual = (newVal != null) ? newVal : "Estudio Libre";
                if (lblAsignaturaActiva != null) {
                    lblAsignaturaActiva.setText(asignaturaActual);
                }
            });
        }

        // Cargamos las stats de BD solo cuando el usuario entra a esa pestaña (optimización)
        if (tabEstadisticas != null) {
            tabEstadisticas.selectedProperty().addListener((obs, old, isNow) -> {
                if (isNow) {
                    actualizarEstadisticasDesdeBD();
                    pintarLigasEnEstadisticas();
                }
            });
        }

        if (chkMision1 != null) chkMision1.setDisable(true);
        if (chkMision2 != null) chkMision2.setDisable(true);

        cargarDatosYConfigurarUI(u);
    }

    /**
     * Descarga todo el perfil del usuario desde MongoDB.
     * Lo metemos en un hilo separado para que no se quede colgada la pantalla.
     */
    private void cargarDatosYConfigurarUI(String username) {
        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            Estudiante estBD = dao.obtenerDatosUsuario(username);
            List<String> misGuardadas = dao.obtenerMisAsignaturas(username);

            int minHoyBD = 0;
            LocalDate hoy = LocalDate.now();
            List<Document> historial = dao.obtenerSesiones(username);

            // Calculamos solo los minutos de hoy para rellenar la barra de meta diaria
            for (Document d : historial) {
                if (d.getString("fecha") != null && d.getString("fecha").equals(hoy.toString())) {
                    minHoyBD += d.getInteger("minutos", 0);
                }
            }
            minutosHoyLocales = minHoyBD;

            // Volvemos al hilo de JavaFX para actualizar las ventanas
            Platform.runLater(() -> {
                if (estBD != null) {
                    estudiante.setPuntosCrecimiento(estBD.getPuntosCrecimiento());
                    estudiante.setMonedasXP(estBD.getMonedasXP());
                    estudiante.setGotasAgua(estBD.getGotasAgua());
                    estudiante.setMinutosEstudio(estBD.getMinutosEstudio());
                    estudiante.setMinutosDescanso(estBD.getMinutosDescanso());
                    estudiante.setMetaDiariaMinutos(estBD.getMetaDiariaMinutos());
                    estudiante.setInventarioSemillas(estBD.getInventarioSemillas());
                    estudiante.setMiJardinNuevo(estBD.getMiJardinNuevo());
                    estudiante.setUltimaRecoleccion(estBD.getUltimaRecoleccion());
                    estudiante.setUltimaFechaMisionMeta(estBD.getUltimaFechaMisionMeta());
                    estudiante.setUltimaFechaMisionRiego(estBD.getUltimaFechaMisionRiego());
                }

                // Revisamos si ya reclamó sus misiones de hoy al arrancar
                String hoyStr = LocalDate.now().toString();
                if (hoyStr.equals(estudiante.getUltimaFechaMisionMeta())) {
                    misionMetaCompletada = true;
                    recompensaMetaReclamada = true;
                    if(btnReclamarM1 != null) { btnReclamarM1.setText("Reclamado"); btnReclamarM1.setDisable(true); }
                    if(chkMision1 != null) { chkMision1.setSelected(true); chkMision1.setStyle("-fx-opacity: 0.5; -fx-strikethrough: true;"); }
                }
                if (hoyStr.equals(estudiante.getUltimaFechaMisionRiego())) {
                    misionRiegoCompletada = true;
                    recompensaRiegoReclamada = true;
                    if(btnReclamarM2 != null) { btnReclamarM2.setText("Reclamado"); btnReclamarM2.setDisable(true); }
                    if(chkMision2 != null) { chkMision2.setSelected(true); chkMision2.setStyle("-fx-opacity: 0.5; -fx-strikethrough: true;"); }
                }

                if (username.equalsIgnoreCase("ousama")) {
                    estudiante.setMonedasXP(99999); estudiante.setGotasAgua(99999); estudiante.setPuntosCrecimiento(495000);
                }

                if (spinEstudio != null) spinEstudio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, estudiante.getMinutosEstudio()));
                if (spinDescanso != null) spinDescanso.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, estudiante.getMinutosDescanso()));
                if (spinMeta != null) spinMeta.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 480, estudiante.getMetaDiariaMinutos()));

                if (misAsignaturas != null) {
                    misAsignaturas.setAll(misGuardadas);
                    if (!misAsignaturas.contains("Estudio Libre")) {
                        misAsignaturas.addFirst("Estudio Libre");
                    }
                    listaAsignaturas.getSelectionModel().selectFirst();
                }

                actualizarUI();
                actualizarListaInventario();
                inicializarJardin();
                configurarReloj();

                tiempoTotalOriginal = estudiante.getMinutosEstudio() * 60;
                tiempoRestante = tiempoTotalOriginal;

                actualizarColoresFase();
                actualizarReloj();
                actualizarUIEstadisticasLocales();

                calcularIngresosPasivos();
            });
            dao.cerrarConexion();
        }).start();
    }

    // ========================================================
    // MOTOR DE VENTANAS (Popups propios en vez de alertas nativas)
    // ========================================================

    private void crearVentanaJuego(String titulo, Node contenido) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        if (lblUser != null && lblUser.getScene() != null) {
            popupStage.initOwner(lblUser.getScene().getWindow());
        }
        popupStage.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #cbd5e1; -fx-border-radius: 15; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 5);");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Button btnCerrar = new Button("X");
        btnCerrar.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 16px; -fx-cursor: hand;");
        btnCerrar.setOnAction(e -> popupStage.close());

        HBox header = new HBox(lblTitulo, new Region(), btnCerrar);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER);

        root.getChildren().addAll(header, new Separator(), contenido);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popupStage.setScene(scene);

        // Forzamos el calculo del layout antes de abrirla para que quede 100% centrada en pantalla
        popupStage.sizeToScene();
        popupStage.centerOnScreen();
        popupStage.showAndWait();
    }

    private void mostrarMensajeJuego(String titulo, String mensaje) {
        Label lblInfo = new Label(mensaje);
        lblInfo.setWrapText(true);
        lblInfo.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");

        Button btnOk = new Button("Entendido");
        btnOk.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand;");
        HBox btnBox = new HBox(btnOk);
        btnBox.setAlignment(Pos.CENTER);

        VBox contenido = new VBox(20, lblInfo, btnBox);
        contenido.setPrefWidth(300);

        Platform.runLater(() -> {
            btnOk.setOnAction(e -> ((Stage) btnOk.getScene().getWindow()).close());
        });

        crearVentanaJuego(titulo, contenido);
    }

    // ========================================================
    // TIENDA: Información detallada de los items
    // ========================================================

    @FXML private void infoPala() { mostrarMensajeJuego("Pala (Herramienta)", "Libera instantáneamente una maceta ocupada o marchita. Un solo uso."); }
    @FXML private void infoFertilizante() { mostrarMensajeJuego("Fertilizante", "Aplica esto a una semilla para que alcance su fase adulta y empiece a producir recursos de inmediato."); }
    @FXML private void infoToldo() { mostrarMensajeJuego("Toldo", "Congela la sed de una planta durante 3 días. Ideal si no vas a poder estudiar este fin de semana."); }
    @FXML private void infoCactus() { mostrarMensajeJuego("Cactus (Común)", obtenerDetallesPlanta("Cactus")); }
    @FXML private void infoBonsai() { mostrarMensajeJuego("Bonsái (Raro)", obtenerDetallesPlanta("Bonsái")); }
    @FXML private void infoGirasol() { mostrarMensajeJuego("Girasol (Épico)", obtenerDetallesPlanta("Girasol")); }
    @FXML private void infoPino() { mostrarMensajeJuego("Pino (Legendario)", obtenerDetallesPlanta("Pino")); }

    private String obtenerDetallesPlanta(String tipo) {
        switch(tipo) {
            case "Cactus": return "Producción: 10 Oro / día\nMantenimiento: Bajo (5 Agua / riego)\n\nResistente y constante. No se marchita si olvidas regarlo.";
            case "Bonsái": return "Producción: 30 Oro + 10 XP / día\nMantenimiento: Medio (15 Agua / riego)\n\nRequiere disciplina. Si lo mantienes óptimo, genera grandes beneficios.";
            case "Girasol": return "Producción: 40 Agua / día\nMantenimiento: Alto (20 Agua / riego)\n\nGenerador de recursos masivo. Invierte 20 gotas hoy y recupera 40 mañana para mantener al resto del jardín.";
            case "Pino": return "Producción: 100 Oro + 50 XP / día\nMantenimiento: Extremo (35 Agua / riego)\n\nAlta exigencia. Multiplica la experiencia obtenida si se mantiene sano.";
            default: return "Información botánica no disponible.";
        }
    }

    // ========================================================
    // PERFIL Y SISTEMA DE LIGAS
    // ========================================================

    private void pintarLigasEnEstadisticas() {
        if (boxLigas == null) return;
        boxLigas.getChildren().clear();

        // Configuración de rangos (Estilo "Camino de trofeos" de los juegos)
        String[] nombresLigas = {"Estudiante Novato (Nvl 1-9)", "Aprendiz Constante (Nvl 10-24)", "Erudito del Foco (Nvl 25-49)", "Maestro del Tiempo (Nvl 50-99)", "Leyenda Académica (Nvl 100+)"};
        String[] iconosLigas = {"🥉", "🥈", "🥇", "💎", "👑"};
        int[] nivelesLigas = {1, 10, 25, 50, 100};

        int miNivel = estudiante.getNivel();

        for (int i = 0; i < nombresLigas.length; i++) {
            HBox filaLiga = new HBox(15);
            filaLiga.setAlignment(Pos.CENTER_LEFT);
            filaLiga.setPadding(new Insets(12));

            Label icono = new Label(iconosLigas[i]);
            icono.setStyle("-fx-font-size: 24px;");

            VBox textos = new VBox(2);
            Label nombre = new Label(nombresLigas[i]);
            Label estado = new Label();

            boolean esMiLiga = (i == nombresLigas.length - 1 && miNivel >= nivelesLigas[i]) ||
                    (i < nombresLigas.length - 1 && miNivel >= nivelesLigas[i] && miNivel < nivelesLigas[i+1]);

            // Pintamos diferente si es tu liga actual, si ya la pasaste o si te falta llegar
            if (esMiLiga) {
                filaLiga.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 10; -fx-border-color: #3b82f6; -fx-border-radius: 10; -fx-border-width: 2;");
                nombre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
                estado.setText("Liga Actual");
                estado.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-font-size: 12px;");
            } else if (miNivel >= nivelesLigas[i]) {
                filaLiga.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 10;");
                nombre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #166534;");
                estado.setText("Superada");
                estado.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px;");
            } else {
                filaLiga.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10;");
                nombre.setStyle("-fx-font-size: 15px; -fx-text-fill: #94a3b8;");
                estado.setText("Bloqueada");
                estado.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
                icono.setStyle("-fx-font-size: 24px; -fx-opacity: 0.5;");
            }

            textos.getChildren().addAll(nombre, estado);
            filaLiga.getChildren().addAll(icono, textos);
            boxLigas.getChildren().add(filaLiga);
        }
    }

    private String obtenerRango(int nivel) {
        if (nivel < 10) return "Estudiante Novato";
        if (nivel < 25) return "Aprendiz Constante";
        if (nivel < 50) return "Erudito del Foco";
        if (nivel < 100) return "Maestro del Tiempo";
        return "Leyenda Académica";
    }

    // ========================================================
    // GESTIÓN DEL HUERTO Y PLANTAS
    // ========================================================

    private void calcularIngresosPasivos() {
        String ultima = estudiante.getUltimaRecoleccion();
        LocalDate hoy = LocalDate.now();

        if (ultima != null && !ultima.equals(hoy.toString())) {
            LocalDate fechaUltima = LocalDate.parse(ultima);

            // Calculamos cuántos días ha estado fuera el jugador para darle la paga de todo ese tiempo
            long diasPasados = ChronoUnit.DAYS.between(fechaUltima, hoy);
            if (diasPasados > 14) diasPasados = 14; // Capeamos a 2 semanas para que no haya locuras si dejan de jugar un año

            int oroGenerado = 0; int aguaGenerada = 0; int xpGenerada = 0;

            for (int d = 1; d <= diasPasados; d++) {
                LocalDate diaSimulado = fechaUltima.plusDays(d);
                for (Planta p : estudiante.getMiJardinNuevo()) {
                    int[] recursos = p.calcularProduccion(estudiante.tieneMusicaActiva(), diaSimulado);
                    p.registrarProduccion(recursos[0], recursos[1], recursos[2]);
                    oroGenerado += recursos[0]; aguaGenerada += recursos[1]; xpGenerada += recursos[2];
                }
            }

            // Mostramos el reporte si las plantas han hecho su trabajo
            if (oroGenerado > 0 || aguaGenerada > 0 || xpGenerada > 0) {
                estudiante.sumarMonedas(oroGenerado);
                estudiante.setGotasAgua(estudiante.getGotasAgua() + aguaGenerada);
                estudiante.setPuntosCrecimiento(estudiante.getPuntosCrecimiento() + xpGenerada);
                mostrarMensajeJuego("☀️ Reporte de Jardinería", "Has estado fuera " + diasPasados + " días.\nTu jardín ha generado:\n\n🪙 +" + oroGenerado + " Oro\n💧 +" + aguaGenerada + " Agua\n🌟 +" + xpGenerada + " XP");
                actualizarUI();
            }
            estudiante.setUltimaRecoleccion(hoy.toString());
            sincronizarConNube();
        }
    }

    private void actualizarUI() {
        Platform.runLater(() -> {
            int nivelActual = estudiante.getNivel();

            if (lblUser != null) lblUser.setText(estudiante.getNombre() + " (Nivel " + nivelActual + ")");
            if (lblRango != null) lblRango.setText(obtenerRango(nivelActual));

            if (lblStatsNombre != null) lblStatsNombre.setText(estudiante.getNombre());
            if (lblStatsRango != null) lblStatsRango.setText("Rango: " + obtenerRango(nivelActual));

            if (lblNivelTienda != null) lblNivelTienda.setText(nivelActual + " 🌟");
            if (lblMonedas != null) lblMonedas.setText(estudiante.getMonedasXP() + " 🪙");
            if (lblGotas != null) lblGotas.setText(estudiante.getGotasAgua() + " 💧");
            if (lblTotalXP != null) lblTotalXP.setText(estudiante.getPuntosCrecimiento() + " XP");

            // Bloqueamos visualmente el nivel en el 100
            if (nivelActual >= 100) {
                if (progresoNivel != null) progresoNivel.setProgress(1.0);
                if (lblXpSiguienteNivel != null) lblXpSiguienteNivel.setText("NIVEL MÁXIMO");
            } else {
                int progresoActual = estudiante.getXpActualEnNivel();
                int metaNivel = estudiante.getXpParaSiguienteNivel();
                if (progresoNivel != null) progresoNivel.setProgress((double) progresoActual / metaNivel);
                if (lblXpSiguienteNivel != null) lblXpSiguienteNivel.setText(progresoActual + " / " + metaNivel + " XP");
            }

            // Desbloqueos de la tienda botánica
            if (btnCactus != null) btnCactus.setDisable(nivelActual < 1);
            if (btnBonsai != null) btnBonsai.setDisable(nivelActual < 3);
            if (btnGirasol != null) btnGirasol.setDisable(nivelActual < 5);
            if (btnPino != null) btnPino.setDisable(nivelActual < 10);

            if (lblRachaEstudio != null) lblRachaEstudio.setText(estudiante.getRachaDiasEstudio() + " días");
        });
    }

    private void inicializarJardin() {
        if (gridJardin == null) return;
        gridJardin.getChildren().clear();

        for (int i = 0; i < 9; i++) {
            Button btn = new Button();
            btn.setPrefSize(120, 120);
            btn.setCursor(javafx.scene.Cursor.HAND);

            Tooltip t = new Tooltip("Maceta Vacía");
            t.setStyle("-fx-font-size: 14px; -fx-padding: 10px; -fx-background-color: #1e293b; -fx-text-fill: white;");
            Tooltip.install(btn, t);
            tooltipsJardin[i] = t;

            final int indiceParcela = i;
            btn.setOnAction(e -> clickParcela(indiceParcela));
            botonesJardin[i] = btn;
            gridJardin.add(btn, i % 3, i / 3);
        }
        refrescarTablero();
    }

    private void refrescarTablero() {
        Planta[] terreno = estudiante.getMiJardinNuevo();

        for (int i = 0; i < 9; i++) {
            Planta p = terreno[i];
            Button btn = botonesJardin[i];
            Tooltip t = tooltipsJardin[i];

            btn.getStyleClass().removeAll("parcela-tierra", "parcela-plantada", "parcela-marchita");
            String estiloBase = "";
            String infoAdicional = "";

            if (p.getTipo().equals("Vacía")) {
                btn.setText("+");
                btn.getStyleClass().add("parcela-tierra");
                btn.setStyle("");
                t.setText("Maceta Vacía\nHaz clic para plantar.");
            } else {
                btn.setText(obtenerEmojiPlanta(p));
                String estado = p.getEstadoSupervivencia();

                if (estado.equals("Marchito")) {
                    btn.getStyleClass().add("parcela-marchita");
                    estiloBase = "-fx-border-color: #dc2626; -fx-opacity: 0.7;";
                } else if (estado.equals("Sediento")) {
                    btn.getStyleClass().add("parcela-plantada");
                    estiloBase = "-fx-border-color: #f59e0b;";
                } else {
                    btn.getStyleClass().add("parcela-plantada");
                    estiloBase = "";
                }

                // El toldo pinta el borde morado
                if (p.getFinToldoProtector() != null && (LocalDate.now().isBefore(p.getFinToldoProtector()) || LocalDate.now().isEqual(p.getFinToldoProtector()))) {
                    estiloBase += " -fx-border-color: #8b5cf6; -fx-border-width: 3;";
                    infoAdicional = "\n☂️ Toldo Activo (Protegida)";
                }

                btn.setStyle(estiloBase);
                String info = p.getTipo() + " (Fase " + p.getFase() + ")\nSalud: " + p.getHidratacion() + "% 💧\nEstado: " + estado + infoAdicional;
                t.setText(info);
            }
        }
    }

    private String obtenerEmojiPlanta(Planta p) {
        if (p.getFase() == 0) return "🌱";
        if (p.getFase() == 1) return "🌿";
        if (p.getFase() == 3) return "🪵";
        switch (p.getTipo()) {
            case "Cactus": return "🌵";
            case "Bonsái": return "🌳";
            case "Girasol": return "🌻";
            case "Pino": return "🌲";
            default: return "🪴";
        }
    }

    private void abrirPokedex(Planta p) {
        HBox layout = new HBox(30);
        layout.setPrefWidth(450);
        layout.setAlignment(Pos.CENTER_LEFT);

        Label icono = new Label(obtenerEmojiPlanta(p));
        icono.setStyle("-fx-font-size: 70px; -fx-padding: 15; -fx-background-color: #f0fdf4; -fx-background-radius: 20; -fx-border-color: #22c55e; -fx-border-radius: 20; -fx-border-width: 3;");

        VBox datos = new VBox(10);
        Label titulo = new Label(p.getTipo() + " (Fase " + p.getFase() + "/2)");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #166534;");

        Label lblSalud = new Label("Estado: " + p.getHidratacion() + "% (" + p.getEstadoSupervivencia() + ")");
        lblSalud.setStyle("-fx-font-weight: bold;");

        VBox historialBox = new VBox(5);
        historialBox.setStyle("-fx-background-color: #f8fafc; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: #cbd5e1; -fx-border-radius: 8;");
        Label histTit = new Label("Registro Histórico");
        histTit.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");

        LocalDate fechaInicio = p.getFechaPlantacion() != null ? p.getFechaPlantacion() : LocalDate.now();
        long diasEdad = ChronoUnit.DAYS.between(fechaInicio, LocalDate.now());

        historialBox.getChildren().addAll(
                histTit,
                new Label("Tiempo activo: " + diasEdad + " días"),
                new Label("Oro generado: " + p.getOroTotalGenerado() + " 🪙"),
                new Label("Agua generada: " + p.getAguaTotalGenerada() + " 💧")
        );

        datos.getChildren().addAll(titulo, lblSalud, historialBox);
        layout.getChildren().addAll(icono, datos);

        crearVentanaJuego("Pokédex Botánica", layout);
    }

    private void clickParcela(int indice) {
        Planta[] terreno = estudiante.getMiJardinNuevo();
        Planta p = terreno[indice];

        if (p.getTipo().equals("Vacía")) {
            List<String> semillasDisponibles = new ArrayList<>();
            estudiante.getInventarioSemillas().forEach((tipo, cantidad) -> {
                if (cantidad > 0 && !tipo.equals("Pala") && !tipo.equals("Fertilizante") && !tipo.equals("Toldo")) {
                    semillasDisponibles.add(tipo);
                }
            });

            if (semillasDisponibles.isEmpty()) {
                mostrarMensajeJuego("Mochila Vacía", "No tienes semillas. Cómpralas en la Tienda Botánica.");
                return;
            }

            ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(semillasDisponibles));
            combo.getSelectionModel().selectFirst();

            Button btnPlantar = new Button("Plantar Semilla");
            btnPlantar.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand;");

            VBox vbox = new VBox(15, new Label("Selecciona una semilla de tu inventario:"), combo, btnPlantar);
            vbox.setAlignment(Pos.CENTER);

            Platform.runLater(() -> btnPlantar.setOnAction(e -> {
                String semillaElegida = combo.getValue();
                estudiante.quitarItem(semillaElegida);
                terreno[indice] = new Planta(semillaElegida);
                refrescarTablero();
                actualizarListaInventario();
                sincronizarConNube();
                ((Stage) btnPlantar.getScene().getWindow()).close();
            }));

            crearVentanaJuego("Sembrar Nueva Planta", vbox);

        } else {
            VBox vbox = new VBox(10);
            vbox.setAlignment(Pos.CENTER);

            Button btnAccionPrimaria = new Button();
            btnAccionPrimaria.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 250; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");

            int costeAguaCalculado = 5;
            if (p.getTipo().equals("Bonsái")) costeAguaCalculado = 15;
            else if (p.getTipo().equals("Girasol")) costeAguaCalculado = 20;
            else if (p.getTipo().equals("Pino")) costeAguaCalculado = 35;

            int costeHospital = 50;
            switch (p.getTipo()) {
                case "Cactus": costeHospital = 15; break;
                case "Bonsái": costeHospital = 40; break;
                case "Girasol": costeHospital = 80; break;
                case "Pino": costeHospital = 150; break;
            }

            if (p.getEstadoSupervivencia().equals("Marchito")) {
                btnAccionPrimaria.setText("Hospital Botánico (Coste: " + costeHospital + " 💧)");
                btnAccionPrimaria.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 250; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");
            } else {
                if (p.getFase() < 2) btnAccionPrimaria.setText("Regar crecimiento (" + costeAguaCalculado + " 💧)");
                else if (p.getHidratacion() < 100) btnAccionPrimaria.setText("Mantenimiento (" + costeAguaCalculado + " 💧)");
                else btnAccionPrimaria.setText("La planta no tiene sed 🌿");
            }

            Button btnInspeccionar = new Button("Inspeccionar Planta 🔍");
            btnInspeccionar.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-pref-width: 250; -fx-padding: 10; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");

            vbox.getChildren().addAll(btnAccionPrimaria, btnInspeccionar);

            if (estudiante.getInventarioSemillas().getOrDefault("Fertilizante", 0) > 0 && p.getFase() < 2) {
                Button btnF = new Button("Usar Fertilizante 🧪");
                btnF.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-pref-width: 250; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");
                vbox.getChildren().add(btnF);
                btnF.setOnAction(e -> {
                    estudiante.quitarItem("Fertilizante"); p.setFase(2); refrescarTablero(); actualizarListaInventario(); sincronizarConNube();
                    ((Stage) btnF.getScene().getWindow()).close();
                });
            }
            if (estudiante.getInventarioSemillas().getOrDefault("Toldo", 0) > 0) {
                Button btnT = new Button("Usar Toldo ☂️");
                btnT.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-pref-width: 250; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");
                vbox.getChildren().add(btnT);
                btnT.setOnAction(e -> {
                    estudiante.quitarItem("Toldo"); p.aplicarToldoProtector(); refrescarTablero(); actualizarListaInventario(); sincronizarConNube();
                    ((Stage) btnT.getScene().getWindow()).close();
                    Platform.runLater(() -> mostrarMensajeJuego("Protegida", "La planta no tendrá sed en 3 días."));
                });
            }
            if (estudiante.getInventarioSemillas().getOrDefault("Pala", 0) > 0) {
                Button btnP = new Button("Usar Pala (Destruir) ⛏️");
                btnP.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-pref-width: 250; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");
                vbox.getChildren().add(btnP);
                btnP.setOnAction(e -> {
                    estudiante.quitarItem("Pala"); terreno[indice] = new Planta(); refrescarTablero(); actualizarListaInventario(); sincronizarConNube();
                    ((Stage) btnP.getScene().getWindow()).close();
                });
            }

            final int fCosteAguaFinal = costeAguaCalculado;

            Platform.runLater(() -> {
                btnAccionPrimaria.setOnAction(e -> {
                    // Cerramos SIEMPRE la ventana primero para que JavaFX no pete al abrir otra encima
                    ((Stage) btnAccionPrimaria.getScene().getWindow()).close();

                    if (btnAccionPrimaria.getText().contains("Hospital")) {
                        int costeHosp = Integer.parseInt(btnAccionPrimaria.getText().replaceAll("[^0-9]", ""));
                        if (estudiante.gastarAgua(costeHosp)) {
                            p.setFase(1);
                            p.regar(costeHosp);
                            refrescarTablero();
                            actualizarUI();
                            sincronizarConNube();
                            Platform.runLater(() -> mostrarMensajeJuego("Milagro", "Planta revivida con éxito."));
                        } else {
                            Platform.runLater(() -> mostrarMensajeJuego("Error", "Necesitas " + costeHosp + " de agua para el hospital."));
                        }
                    } else if (btnAccionPrimaria.getText().contains("Regar") || btnAccionPrimaria.getText().contains("Mantenimiento")) {
                        if (estudiante.gastarAgua(fCosteAguaFinal)) {
                            p.regar(fCosteAguaFinal); refrescarTablero(); actualizarUI();

                            if (!misionRiegoCompletada) {
                                misionRiegoCompletada = true;
                                if (chkMision2 != null) chkMision2.setSelected(true);
                                if (btnReclamarM2 != null && !recompensaRiegoReclamada) btnReclamarM2.setDisable(false);
                            }
                            sincronizarConNube();
                        } else {
                            Platform.runLater(() -> mostrarMensajeJuego("Error", "Necesitas " + fCosteAguaFinal + " de agua."));
                        }
                    }
                });

                btnInspeccionar.setOnAction(e -> {
                    ((Stage) btnInspeccionar.getScene().getWindow()).close();
                    Platform.runLater(() -> abrirPokedex(p));
                });
            });

            crearVentanaJuego("Gestión de Maceta", vbox);
        }
    }

    // ================== ZONA DE COMPRAS ==================

    @FXML private void comprarPala() { procesarCompraReal("Pala", 20); }
    @FXML private void comprarFertilizante() { procesarCompraReal("Fertilizante", 100); }
    @FXML private void comprarToldo() { procesarCompraReal("Toldo", 150); }
    @FXML private void comprarCactus() { procesarCompraReal("Cactus", 50); }
    @FXML private void comprarBonsai() { procesarCompraReal("Bonsái", 200); }
    @FXML private void comprarGirasol() { procesarCompraReal("Girasol", 500); }
    @FXML private void comprarPino() { procesarCompraReal("Pino", 1000); }

    private void procesarCompraReal(String tipo, int precio) {
        if (estudiante.gastarMonedas(precio)) {
            estudiante.añadirItem(tipo);
            actualizarUI();
            actualizarListaInventario();
            sincronizarConNube();
        } else {
            mostrarMensajeJuego("Oro Insuficiente", "No tienes suficiente oro para comprar: " + tipo);
        }
    }

    // ========================================================
    // MISIONES DIARIAS Y SINCRONIZACIÓN BBDD
    // ========================================================

    @FXML
    private void reclamarMision1() {
        if (misionMetaCompletada && !recompensaMetaReclamada) {
            estudiante.sumarMonedas(100);
            estudiante.registrarMetaCumplida();
            estudiante.setUltimaFechaMisionMeta(LocalDate.now().toString());
            recompensaMetaReclamada = true;
            btnReclamarM1.setText("Reclamado");
            btnReclamarM1.setDisable(true);
            chkMision1.setStyle("-fx-opacity: 0.5; -fx-strikethrough: true;");
            actualizarUI();
            sincronizarConNube();
        }
    }

    @FXML
    private void reclamarMision2() {
        if (misionRiegoCompletada && !recompensaRiegoReclamada) {
            estudiante.setGotasAgua(estudiante.getGotasAgua() + 20);
            estudiante.setUltimaFechaMisionRiego(LocalDate.now().toString());
            recompensaRiegoReclamada = true;
            btnReclamarM2.setText("Reclamado");
            btnReclamarM2.setDisable(true);
            chkMision2.setStyle("-fx-opacity: 0.5; -fx-strikethrough: true;");
            actualizarUI();
            sincronizarConNube();
        }
    }

    private void actualizarListaInventario() {
        if (listaInventario != null) {
            ObservableList<String> items = FXCollections.observableArrayList();
            estudiante.getInventarioSemillas().forEach((tipo, cantidad) -> {
                if (cantidad > 0) items.add(tipo + " (x" + cantidad + ")");
            });
            listaInventario.setItems(items);
        }
    }

    private void sincronizarConNube() {
        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            dao.guardarProgresoJardin(estudiante);
            dao.cerrarConexion();
        }).start();
    }

    // Método clave para no tirar la base de datos de Mongo. Mandamos los minutos acumulados de golpe.
    private void volcarDatosBD() {
        if (minutosPendientesDeGuardar > 0) {
            final int minutos = minutosPendientesDeGuardar;
            minutosPendientesDeGuardar = 0;
            new Thread(() -> {
                UsuarioDAO d = new UsuarioDAO();
                d.registrarSesion(estudiante.getNombre(), asignaturaActual, minutos);
                d.actualizarXP(estudiante.getNombre(), estudiante.getPuntosCrecimiento());
                d.cerrarConexion();
            }).start();
        }
    }

    // ========================================================
    // CORE TEMPORIZADOR Y ESTADÍSTICAS
    // ========================================================

    @FXML
    private void aplicarConfiguracion() {
        if (estudiando) return; // No se puede cambiar el tiempo en plena sesión

        estudiante.setMinutosEstudio(spinEstudio.getValue());
        estudiante.setMinutosDescanso(spinDescanso.getValue());
        estudiante.setMetaDiariaMinutos(spinMeta.getValue());

        new Thread(() -> {
            UsuarioDAO d = new UsuarioDAO();
            d.guardarConfiguracion(estudiante.getNombre(), estudiante.getMinutosEstudio(), estudiante.getMinutosDescanso(), estudiante.getMetaDiariaMinutos());
            d.cerrarConexion();
        }).start();

        tiempoTotalOriginal = estudiante.getMinutosEstudio() * 60;
        tiempoRestante = tiempoTotalOriginal;
        segundosEstudiadosSesion = 0;
        actualizarReloj();
        actualizarUIEstadisticasLocales();
    }

    private void actualizarUIEstadisticasLocales() {
        if (progresoDiario != null && estudiante.getMetaDiariaMinutos() > 0) {
            progresoDiario.setProgress(Math.min(1.0, (double) minutosHoyLocales / estudiante.getMetaDiariaMinutos()));
            if (lblMetaTexto != null) {
                lblMetaTexto.setText(minutosHoyLocales + " / " + estudiante.getMetaDiariaMinutos() + " min");
            }
            if (minutosHoyLocales >= estudiante.getMetaDiariaMinutos() && !misionMetaCompletada) {
                misionMetaCompletada = true;
                if (chkMision1 != null) chkMision1.setSelected(true);
                if (btnReclamarM1 != null && !recompensaMetaReclamada) btnReclamarM1.setDisable(false);
            }
        }
    }

    private void configurarReloj() {
        // El corazón del temporizador (Pomodoro)
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            tiempoRestante--;

            if (!esModoDescanso) {
                segundosEstudiadosSesion++;

                // Solo damos las recompensas cada 60 segundos (1 minuto real)
                if (segundosEstudiadosSesion % 60 == 0) {
                    estudiante.sumarMinutoEstudio();
                    minutosHoyLocales++;
                    minutosPendientesDeGuardar++;
                    actualizarUIEstadisticasLocales();
                    actualizarUI();
                }
            }
            actualizarReloj();
            if (tiempoRestante <= 0) terminarFase();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    private void terminarFase() {
        java.awt.Toolkit.getDefaultToolkit().beep();
        esModoDescanso = !esModoDescanso;

        volcarDatosBD();

        lblModo.setText(esModoDescanso ? "Modo Descanso" : "Modo Enfoque");
        btnOmitir.setVisible(esModoDescanso);
        btnOmitir.setManaged(esModoDescanso);

        tiempoTotalOriginal = (esModoDescanso ? estudiante.getMinutosDescanso() : estudiante.getMinutosEstudio()) * 60;
        tiempoRestante = tiempoTotalOriginal;
        segundosEstudiadosSesion = 0;

        actualizarColoresFase();
        actualizarReloj();
    }

    private void actualizarColoresFase() {
        // Cambia el tema a verde si es descanso y a oscuro si es estudio, simulando un Dark Mode
        Platform.runLater(() -> {
            if (tarjetaTemporizador == null) return;

            if (esModoDescanso) {
                tarjetaTemporizador.setStyle("-fx-background-color: #064e3b; -fx-padding: 40; -fx-background-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 8);");
                if (lblModo != null) lblModo.setStyle("-fx-text-fill: #6ee7b7; -fx-font-size: 16px; -fx-font-weight: bold;");
                if (progresoSesion != null) progresoSesion.setStyle("-fx-accent: #10b981; -fx-control-inner-background: #065f46; -fx-background-radius: 10;");
            } else {
                tarjetaTemporizador.setStyle("-fx-background-color: #1e293b; -fx-padding: 40; -fx-background-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 8);");
                if (lblModo != null) lblModo.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-font-weight: bold;");
                if (progresoSesion != null) progresoSesion.setStyle("-fx-accent: #3b82f6; -fx-control-inner-background: #334155; -fx-background-radius: 10;");
            }
        });
    }

    @FXML private void omitirDescanso() { if (esModoDescanso) terminarFase(); }

    @FXML private void gestionarTemporizador() {
        if (!estudiando) {
            timeline.play();
            estudiando = true;
            btnAccion.setText("⏸ Pausar");
            btnAccion.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 12 30; -fx-background-radius: 30; -fx-cursor: hand;");
            if (btnAbandonar != null) {
                btnAbandonar.setVisible(true);
                btnAbandonar.setManaged(true);
            }
        } else {
            timeline.pause();
            estudiando = false;
            volcarDatosBD();
            btnAccion.setText("▶ Reanudar");
            btnAccion.setStyle("-fx-background-color: white; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 12 30; -fx-background-radius: 30; -fx-cursor: hand;");
        }
    }

    @FXML private void abandonarSesion() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Seguro que quieres abandonar tu sesión actual?", ButtonType.YES, ButtonType.NO);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                timeline.pause();
                volcarDatosBD();
                estudiando = false;
                esModoDescanso = false;
                if (lblModo != null) lblModo.setText("Período de concentración");
                tiempoTotalOriginal = estudiante.getMinutosEstudio() * 60;
                tiempoRestante = tiempoTotalOriginal;
                segundosEstudiadosSesion = 0;

                btnAccion.setText("▶ Iniciar");
                btnAccion.setStyle("-fx-background-color: white; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 12 40; -fx-background-radius: 30; -fx-cursor: hand;");

                if (btnAbandonar != null) { btnAbandonar.setVisible(false); btnAbandonar.setManaged(false); }
                if (btnOmitir != null) { btnOmitir.setVisible(false); btnOmitir.setManaged(false); }
                actualizarColoresFase();
                actualizarReloj();
            }
        });
    }

    private void actualizarReloj() {
        int m = tiempoRestante / 60;
        int s = tiempoRestante % 60;
        if (lblTiempo != null) lblTiempo.setText(String.format("%02d:%02d", m, s));
        if (progresoSesion != null && tiempoTotalOriginal > 0) progresoSesion.setProgress((double) tiempoRestante / tiempoTotalOriginal);
    }

    public void actualizarEstadisticasDesdeBD() {
        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            List<Document> sesionesList = dao.obtenerSesiones(estudiante.getNombre());
            dao.cerrarConexion();

            int minHoy = 0, minUltimos7Dias = 0;
            Map<String, Integer> porAsignatura = new HashMap<>();
            LocalDate hoy = LocalDate.now();
            LocalDate hace7Dias = hoy.minusDays(6);
            Map<LocalDate, Integer> ultimos7DiasData = new LinkedHashMap<>();

            for (int i = 6; i >= 0; i--) ultimos7DiasData.put(hoy.minusDays(i), 0);

            for (Document d : sesionesList) {
                try {
                    Integer m = d.getInteger("minutos");
                    String fechaStr = d.getString("fecha");
                    String asig = d.getString("asignatura");
                    if (m == null || fechaStr == null || asig == null) continue;

                    LocalDate fecha = LocalDate.parse(fechaStr);
                    if (fecha.equals(hoy)) minHoy += m;
                    if (!fecha.isBefore(hace7Dias)) {
                        minUltimos7Dias += m;
                        if (ultimos7DiasData.containsKey(fecha)) ultimos7DiasData.put(fecha, ultimos7DiasData.get(fecha) + m);
                    }
                    porAsignatura.put(asig, porAsignatura.getOrDefault(asig, 0) + m);
                } catch (Exception ex) {}
            }

            // Calculamos métricas extras para las tarjetas de estadísticas
            int plantasVivas = 0;
            int oroHistorico = 0;
            for(Planta p : estudiante.getMiJardinNuevo()) {
                if(!p.getTipo().equals("Vacía") && !p.getEstadoSupervivencia().equals("Marchito")) {
                    plantasVivas++;
                }
                oroHistorico += p.getOroTotalGenerado();
            }

            final int fMinHoy = minHoy, fMinSem = minUltimos7Dias, fPlantasVivas = plantasVivas, fOroHistorico = oroHistorico;
            Platform.runLater(() -> {
                if (lblHoy != null) lblHoy.setText(fMinHoy / 60 + "h " + fMinHoy % 60 + "m");
                if (lblSemana != null) lblSemana.setText(fMinSem / 60 + "h " + fMinSem % 60 + "m");
                if (lblPlantasVivas != null) lblPlantasVivas.setText(String.valueOf(fPlantasVivas));
                if (lblOroTotalStats != null) lblOroTotalStats.setText(String.valueOf(fOroHistorico));

                if (chartAsignaturas != null) {
                    chartAsignaturas.getData().clear();
                    porAsignatura.forEach((k, v) -> chartAsignaturas.getData().add(new PieChart.Data(k, v)));
                }
                if (chartSemana != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
                    for (Map.Entry<LocalDate, Integer> entry : ultimos7DiasData.entrySet()) {
                        series.getData().add(new XYChart.Data<>(entry.getKey().format(fmt), entry.getValue()));
                    }
                    chartSemana.getData().clear();
                    chartSemana.getData().add(series);
                }
            });
        }).start();
    }

    @FXML private void abrirPerfil() {
        TabPane tabPane = tabEstadisticas.getTabPane();
        tabPane.getSelectionModel().select(tabEstadisticas);
    }

    @FXML private void agregarAsignatura() {
        String n = txtNuevaAsignatura.getText().trim();
        if (!n.isEmpty() && !misAsignaturas.contains(n)) {
            misAsignaturas.add(n);
            new Thread(() -> {
                UsuarioDAO d = new UsuarioDAO();
                d.guardarMisAsignaturas(estudiante.getNombre(), new ArrayList<>(misAsignaturas));
                d.cerrarConexion();
            }).start();
            listaAsignaturas.getSelectionModel().select(n);
            txtNuevaAsignatura.clear();
        }
    }

    @FXML private void eliminarAsignatura() {
        String s = listaAsignaturas.getSelectionModel().getSelectedItem();
        if (s != null && !s.equals("Estudio Libre")) {
            misAsignaturas.remove(s);
            new Thread(() -> {
                UsuarioDAO d = new UsuarioDAO();
                d.guardarMisAsignaturas(estudiante.getNombre(), new ArrayList<>(misAsignaturas));
                d.cerrarConexion();
            }).start();
        }
    }

    @FXML private void cerrarSesion() {
        if (timeline != null) timeline.pause();
        volcarDatosBD();
        try {
            Parent r = FXMLLoader.load(getClass().getResource("/es/dam/ousama/vista/Login.fxml"));
            Stage s = (Stage) lblUser.getScene().getWindow();
            s.setMaximized(false);
            s.setScene(new Scene(r, 450, 500));
            s.centerOnScreen();
            s.setTitle("StudyBuddy - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}