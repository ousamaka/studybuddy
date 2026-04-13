package es.dam.ousama.controlador;

import es.dam.ousama.modelo.Estudiante;
import es.dam.ousama.modelo.dao.UsuarioDAO;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashboardControlador {

    @FXML private Label lblUser, lblTiempo, lblModo, lblTotalXP;
    @FXML private ProgressBar progresoDiario;
    @FXML private Button btnAccion;
    @FXML private Spinner<Integer> spinEstudio, spinDescanso;
    @FXML private TextField txtNuevaAsignatura;
    @FXML private ListView<String> listaAsignaturas;

    private Estudiante estudiante;
    private ObservableList<String> misAsignaturas;
    private Timeline timeline;
    private int tiempoRestante;
    private boolean estudiando = false;
    private boolean esModoDescanso = false;

    public void initData(String nombreUsuario) {
        estudiante = new Estudiante(nombreUsuario);
        lblUser.setText("Estudiante: " + nombreUsuario);

        spinEstudio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 25));
        spinDescanso.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 5));

        misAsignaturas = FXCollections.observableArrayList();
        listaAsignaturas.setItems(misAsignaturas);

        aplicarConfiguracion();
        configurarMotorReloj();
        actualizarInterfaz();
    }

    private void actualizarInterfaz() {
        lblTotalXP.setText(String.valueOf(estudiante.getPuntosCrecimiento()));
        progresoDiario.setProgress(estudiante.getPuntosCrecimiento() / 3600.0);
    }

    @FXML
    private void aplicarConfiguracion() {
        if (estudiando) return;
        esModoDescanso = false;
        lblModo.setText("Modo Enfoque");
        lblModo.getStyleClass().remove("success");
        if (!lblModo.getStyleClass().contains("accent")) lblModo.getStyleClass().add("accent");

        tiempoRestante = spinEstudio.getValue() * 60;
        actualizarReloj();
    }

    @FXML
    private void gestionarTemporizador() {
        if (!estudiando) {
            estudiando = true;
            btnAccion.setText("Pausar");
            btnAccion.getStyleClass().remove("success");
            btnAccion.getStyleClass().add("danger");
            timeline.play();
        } else {
            estudiando = false;
            btnAccion.setText("Reanudar");
            btnAccion.getStyleClass().remove("danger");
            btnAccion.getStyleClass().add("success");
            timeline.pause();
            guardarEnNube();
        }
    }

    private void configurarMotorReloj() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            tiempoRestante--;
            if (!esModoDescanso) estudiante.sumarPuntos(1);
            actualizarReloj();
            actualizarInterfaz();
            if (tiempoRestante <= 0) cambiarFase();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    private void cambiarFase() {
        timeline.pause();
        estudiando = false;
        btnAccion.setText("Iniciar");
        btnAccion.getStyleClass().remove("danger");
        btnAccion.getStyleClass().add("success");

        if (!esModoDescanso) {
            esModoDescanso = true;
            lblModo.setText("Modo Descanso");
            lblModo.getStyleClass().remove("accent");
            lblModo.getStyleClass().add("success");
            tiempoRestante = spinDescanso.getValue() * 60;
        } else {
            esModoDescanso = false;
            lblModo.setText("Modo Enfoque");
            lblModo.getStyleClass().remove("success");
            lblModo.getStyleClass().add("accent");
            tiempoRestante = spinEstudio.getValue() * 60;
        }
        actualizarReloj();
    }

    private void actualizarReloj() {
        int min = tiempoRestante / 60;
        int seg = tiempoRestante % 60;
        lblTiempo.setText(String.format("%02d:%02d", min, seg));
    }

    @FXML private void agregarAsignatura() {
        String n = txtNuevaAsignatura.getText().trim();
        if (!n.isEmpty() && !misAsignaturas.contains(n)) {
            misAsignaturas.add(n);
            txtNuevaAsignatura.clear();
        }
    }

    @FXML private void eliminarAsignatura() {
        String s = listaAsignaturas.getSelectionModel().getSelectedItem();
        if (s != null) misAsignaturas.remove(s);
    }

    private void guardarEnNube() {
        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            dao.actualizarXP(estudiante.getNombre(), estudiante.getPuntosCrecimiento());
            dao.cerrarConexion();
        }).start();
    }

    @FXML private void cerrarSesion() {
        if (timeline != null) timeline.pause();
        guardarEnNube();
        try {
            Parent r = FXMLLoader.load(getClass().getResource("/es/dam/ousama/vista/Login.fxml"));
            Stage s = (Stage) lblUser.getScene().getWindow();
            s.setScene(new Scene(r, 450, 500));
            s.setMaximized(false);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void salirApp() {
        guardarEnNube();
        Platform.exit();
        System.exit(0);
    }
}