package es.dam.ousama.controlador;

import es.dam.ousama.modelo.*;
import es.dam.ousama.modelo.dao.UsuarioDAO;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.util.Duration;
import org.bson.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DashboardControlador {
    @FXML private Label lblUser, lblTiempo, lblModo, lblTotalXP, lblMetaTexto, lblHoy, lblSemana, lblAsignaturaActiva;
    @FXML private ProgressBar progresoSesion, progresoDiario;
    @FXML private Button btnAccion, btnOmitir, btnAbandonar;
    @FXML private Spinner<Integer> spinEstudio, spinDescanso, spinMeta;
    @FXML private ListView<String> listaAsignaturas;
    @FXML private TextField txtNuevaAsignatura;
    @FXML private PieChart chartAsignaturas;
    @FXML private BarChart<String, Number> chartSemana;
    @FXML private Tab tabEstadisticas;
    @FXML private VBox tarjetaTemporizador;

    // --- NUEVOS ELEMENTOS DE LA TIENDA ---
    @FXML private Label lblMonedas;
    @FXML private ListView<String> listaInventario;

    private Estudiante estudiante;
    private ObservableList<String> misAsignaturas;
    private Timeline timeline;

    private int tiempoRestante, tiempoTotalOriginal;
    private int segundosEstudiadosSesion = 0;
    private int minutosHoyLocales = 0;

    private boolean estudiando = false, esModoDescanso = false;
    private String asignaturaActual = "Estudio Libre";
    private boolean bloqueandoLista = false;

    public void initData(String u) {
        estudiante = new Estudiante(u);

        if (listaAsignaturas != null) {
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

            listaAsignaturas.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (bloqueandoLista) return;

                if ((estudiando || tiempoRestante < tiempoTotalOriginal) && !esModoDescanso) {
                    bloqueandoLista = true;
                    Platform.runLater(() -> {
                        listaAsignaturas.getSelectionModel().select(oldVal);
                        mostrarAlerta("Sesión en curso", "No puedes cambiar de asignatura mientras estudias.", "Termina o usa el botón 'Abandonar' para cambiar de materia.");
                        bloqueandoLista = false;
                    });
                    return;
                }

                asignaturaActual = (newVal != null) ? newVal : "Estudio Libre";
                if (lblAsignaturaActiva != null) lblAsignaturaActiva.setText("Registrando: " + asignaturaActual);
            });
        }

        if (tabEstadisticas != null) {
            tabEstadisticas.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) actualizarEstadisticasDesdeBD();
            });
        }

        cargarDatosYConfigurarUI(u);
    }

    private void cargarDatosYConfigurarUI(String username) {
        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            Estudiante estBD = dao.obtenerDatosUsuario(username);
            List<String> misGuardadas = dao.obtenerMisAsignaturas(username);

            int minHoyBD = 0;
            LocalDate hoy = LocalDate.now();
            List<Document> historialSesiones = dao.obtenerSesiones(username);
            boolean necesitaMigracion = false;

            for (Document d : historialSesiones) {
                if (d.getString("fecha") != null && d.getString("fecha").equals(hoy.toString())) {
                    minHoyBD += d.getInteger("minutos", 0);
                }
                String asigHistorial = d.getString("asignatura");
                if (asigHistorial != null && !asigHistorial.equals("Estudio Libre") && !misGuardadas.contains(asigHistorial)) {
                    misGuardadas.add(asigHistorial);
                    necesitaMigracion = true;
                }
            }
            if (necesitaMigracion) dao.guardarMisAsignaturas(username, misGuardadas);

            minutosHoyLocales = minHoyBD;
            dao.cerrarConexion();

            Platform.runLater(() -> {
                if (estBD != null) {
                    estudiante.setPuntosCrecimiento(estBD.getPuntosCrecimiento());
                    estudiante.setMonedasXP(estBD.getMonedasXP()); // Cargamos saldo
                    estudiante.setMinutosEstudio(estBD.getMinutosEstudio());
                    estudiante.setMinutosDescanso(estBD.getMinutosDescanso());
                    estudiante.setMetaDiariaMinutos(estBD.getMetaDiariaMinutos());
                }

                if (lblUser != null) lblUser.setText("Usuario: " + username + " (Nivel " + estudiante.getNivel() + ")");
                if (lblTotalXP != null) lblTotalXP.setText(estudiante.getPuntosCrecimiento() + " XP");
                if (lblMonedas != null) lblMonedas.setText(estudiante.getMonedasXP() + " XP");

                if (spinEstudio != null) spinEstudio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, estudiante.getMinutosEstudio()));
                if (spinDescanso != null) spinDescanso.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, estudiante.getMinutosDescanso()));
                if (spinMeta != null) spinMeta.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 480, estudiante.getMetaDiariaMinutos()));

                if (misAsignaturas != null) {
                    misAsignaturas.setAll(misGuardadas);
                    if (!misAsignaturas.contains("Estudio Libre")) misAsignaturas.add(0, "Estudio Libre");
                    listaAsignaturas.getSelectionModel().selectFirst();
                }

                actualizarListaInventario();
                configurarReloj();
                tiempoTotalOriginal = estudiante.getMinutosEstudio() * 60;
                tiempoRestante = tiempoTotalOriginal;
                actualizarColoresFase();
                actualizarReloj();
                actualizarUIEstadisticasLocales();
            });
        }).start();
    }

    @FXML
    private void aplicarConfiguracion() {
        if (estudiando) return;

        estudiante.setMinutosEstudio(spinEstudio.getValue());
        estudiante.setMinutosDescanso(spinDescanso.getValue());
        estudiante.setMetaDiariaMinutos(spinMeta.getValue());

        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            dao.guardarConfiguracion(estudiante.getNombre(), estudiante.getMinutosEstudio(), estudiante.getMinutosDescanso(), estudiante.getMetaDiariaMinutos());
            dao.cerrarConexion();
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
            if (lblMetaTexto != null) lblMetaTexto.setText(minutosHoyLocales + " / " + estudiante.getMetaDiariaMinutos() + " min");
        }
    }

    private void configurarReloj() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            tiempoRestante--;

            if (!esModoDescanso) {
                estudiante.sumarPuntos(1);
                segundosEstudiadosSesion++;

                if (segundosEstudiadosSesion % 60 == 0) {
                    minutosHoyLocales++;
                    actualizarUIEstadisticasLocales();
                    guardarMinutoEnBD();
                }

                if (lblTotalXP != null) lblTotalXP.setText(estudiante.getPuntosCrecimiento() + " XP");
                if (lblMonedas != null) lblMonedas.setText(estudiante.getMonedasXP() + " XP");
                if (lblUser != null) lblUser.setText("Usuario: " + estudiante.getNombre() + " (Nivel " + estudiante.getNivel() + ")");
            }

            actualizarReloj();
            if (tiempoRestante <= 0) terminarFase();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    private void guardarMinutoEnBD() {
        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            dao.registrarSesion(estudiante.getNombre(), asignaturaActual, 1);
            dao.actualizarXP(estudiante.getNombre(), estudiante.getPuntosCrecimiento());
            dao.cerrarConexion();
        }).start();
    }

    private void terminarFase() {
        java.awt.Toolkit.getDefaultToolkit().beep();

        if (!esModoDescanso) {
            esModoDescanso = true;
            if (btnOmitir != null) { btnOmitir.setVisible(true); btnOmitir.setManaged(true); }
            if (lblModo != null) lblModo.setText("Modo Descanso");
            tiempoTotalOriginal = estudiante.getMinutosDescanso() * 60;
        } else {
            esModoDescanso = false;
            if (btnOmitir != null) { btnOmitir.setVisible(false); btnOmitir.setManaged(false); }
            if (lblModo != null) lblModo.setText("Modo Enfoque");
            tiempoTotalOriginal = estudiante.getMinutosEstudio() * 60;
        }

        tiempoRestante = tiempoTotalOriginal;
        segundosEstudiadosSesion = 0;

        actualizarColoresFase();
        actualizarReloj();
    }

    private void actualizarColoresFase() {
        Platform.runLater(() -> {
            if (tarjetaTemporizador == null) return;

            if (esModoDescanso) {
                tarjetaTemporizador.setStyle("-fx-padding: 40; -fx-background-color: #ecfdf5; -fx-effect: dropshadow(three-pass-box, rgba(16,185,129,0.3), 20, 0, 0, 10); -fx-background-radius: 20; -fx-border-color: #a7f3d0; -fx-border-width: 2; -fx-border-radius: 18;");
                if (lblModo != null) lblModo.setStyle("-fx-font-size: 26px; -fx-text-fill: #059669; -fx-font-weight: bold;");
                if (lblTiempo != null) lblTiempo.setStyle("-fx-font-size: 150px; -fx-font-weight: bold; -fx-text-fill: #064e3b;");
                if (lblAsignaturaActiva != null) lblAsignaturaActiva.setStyle("-fx-font-size: 16px; -fx-text-fill: #10b981;");
                if (progresoSesion != null) progresoSesion.setStyle("-fx-accent: #10b981; -fx-control-inner-background: #d1fae5;");
            } else {
                tarjetaTemporizador.setStyle("-fx-padding: 40; -fx-background-color: #eff6ff; -fx-effect: dropshadow(three-pass-box, rgba(59,130,246,0.3), 20, 0, 0, 10); -fx-background-radius: 20; -fx-border-color: #93c5fd; -fx-border-width: 2; -fx-border-radius: 18;");
                if (lblModo != null) lblModo.setStyle("-fx-font-size: 26px; -fx-text-fill: #2563eb; -fx-font-weight: bold;");
                if (lblTiempo != null) lblTiempo.setStyle("-fx-font-size: 150px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
                if (lblAsignaturaActiva != null) lblAsignaturaActiva.setStyle("-fx-font-size: 16px; -fx-text-fill: #3b82f6;");
                if (progresoSesion != null) progresoSesion.setStyle("-fx-accent: #3b82f6; -fx-control-inner-background: #dbeafe;");
            }
        });
    }

    @FXML private void omitirDescanso() { if (esModoDescanso) terminarFase(); }

    @FXML private void gestionarTemporizador() {
        if (!estudiando) {
            timeline.play();
            estudiando = true;
            btnAccion.setText("Pausar");
            btnAccion.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 22px; -fx-padding: 15 50; -fx-background-radius: 30; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(245,158,11,0.4), 10, 0, 0, 4);");

            if (btnAbandonar != null) {
                btnAbandonar.setVisible(true);
                btnAbandonar.setManaged(true);
            }
        } else {
            timeline.pause();
            estudiando = false;
            btnAccion.setText("Reanudar");
            btnAccion.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-size: 22px; -fx-padding: 15 50; -fx-background-radius: 30; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(34,197,94,0.4), 10, 0, 0, 4);");
        }
    }

    @FXML private void abandonarSesion() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Abandonar Sesión");
        alert.setHeaderText("¿Seguro que quieres abandonar la sesión actual?");
        alert.setContentText("El tiempo que llevas estudiado ya está a salvo en tu progreso.");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                timeline.pause();
                estudiando = false;
                esModoDescanso = false;
                if (lblModo != null) lblModo.setText("Modo Enfoque");

                tiempoTotalOriginal = estudiante.getMinutosEstudio() * 60;
                tiempoRestante = tiempoTotalOriginal;
                segundosEstudiadosSesion = 0;

                btnAccion.setText("Iniciar Estudio");
                btnAccion.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-size: 22px; -fx-padding: 15 50; -fx-background-radius: 30; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(34,197,94,0.4), 10, 0, 0, 4);");

                if (btnAbandonar != null) { btnAbandonar.setVisible(false); btnAbandonar.setManaged(false); }
                if (btnOmitir != null) { btnOmitir.setVisible(false); btnOmitir.setManaged(false); }

                actualizarColoresFase();
                actualizarReloj();
            }
        });
    }

    private void mostrarAlerta(String titulo, String cabecera, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(cabecera);
        alert.setContentText(mensaje);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    private void actualizarReloj() {
        int m = tiempoRestante / 60, s = tiempoRestante % 60;
        if (lblTiempo != null) lblTiempo.setText(String.format("%02d:%02d", m, s));
        if (progresoSesion != null && tiempoTotalOriginal > 0) {
            progresoSesion.setProgress((double) tiempoRestante / tiempoTotalOriginal);
        }
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
                        if (ultimos7DiasData.containsKey(fecha)) {
                            ultimos7DiasData.put(fecha, ultimos7DiasData.get(fecha) + m);
                        }
                    }
                    porAsignatura.put(asig, porAsignatura.getOrDefault(asig, 0) + m);
                } catch (Exception ex) {}
            }

            final int fMinHoy = minHoy, fMinSem = minUltimos7Dias;
            Platform.runLater(() -> {
                if (lblHoy != null) lblHoy.setText(fMinHoy / 60 + "h " + fMinHoy % 60 + "m");
                if (lblSemana != null) lblSemana.setText(fMinSem / 60 + "h " + fMinSem % 60 + "m");

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

    private void sincronizarMisAsignaturasConBD() {
        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            dao.guardarMisAsignaturas(estudiante.getNombre(), new ArrayList<>(misAsignaturas));
            dao.cerrarConexion();
        }).start();
    }

    @FXML private void agregarAsignatura() {
        if (txtNuevaAsignatura == null || misAsignaturas == null) return;
        String n = txtNuevaAsignatura.getText().trim();
        if (!n.isEmpty() && !misAsignaturas.contains(n)) {
            misAsignaturas.add(n);
            sincronizarMisAsignaturasConBD();
            listaAsignaturas.getSelectionModel().select(n);
            txtNuevaAsignatura.clear();
        }
    }

    @FXML private void eliminarAsignatura() {
        if (listaAsignaturas == null || misAsignaturas == null) return;
        String seleccionada = listaAsignaturas.getSelectionModel().getSelectedItem();
        if (seleccionada != null && !seleccionada.equals("Estudio Libre")) {
            misAsignaturas.remove(seleccionada);
            sincronizarMisAsignaturasConBD();
        }
    }

    // --- MÉTODOS DE LA TIENDA Y JARDÍN ---

    @FXML
    private void comprarCactus() { procesarCompra("Cactus", 50); }

    @FXML
    private void comprarBonsai() { procesarCompra("Bonsái", 200); }

    private void procesarCompra(String tipo, int precio) {
        if (estudiante.gastarMonedas(precio)) {
            estudiante.añadirSemilla(tipo);
            if (lblMonedas != null) lblMonedas.setText(estudiante.getMonedasXP() + " XP");
            actualizarListaInventario();
            sincronizarMonedasConNube();
        } else {
            mostrarAlerta("Saldo insuficiente", "No tienes suficiente XP para esta semilla.", "¡Sigue estudiando para conseguir más!");
        }
    }

    private void actualizarListaInventario() {
        if (listaInventario != null) {
            ObservableList<String> items = FXCollections.observableArrayList();
            estudiante.getInventarioSemillas().forEach((tipo, cantidad) -> {
                items.add(tipo + " (x" + cantidad + ")");
            });
            listaInventario.setItems(items);
        }
    }

    private void sincronizarMonedasConNube() {
        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            dao.actualizarMonedasYXP(estudiante.getNombre(), estudiante.getMonedasXP(), estudiante.getPuntosCrecimiento());
            dao.cerrarConexion();
        }).start();
    }

    @FXML private void cerrarSesion() {
        if (timeline != null) timeline.pause();
        try {
            Parent r = FXMLLoader.load(getClass().getResource("/es/dam/ousama/vista/Login.fxml"));
            Stage s = (Stage) lblUser.getScene().getWindow();
            s.setMaximized(false);
            s.setScene(new Scene(r, 450, 500));
            s.centerOnScreen();
            s.setTitle("StudyBuddy - Login");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void salirApp() {
        if (timeline != null) timeline.pause();
        Platform.exit();
        System.exit(0);
    }
}