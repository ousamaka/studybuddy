package es.dam.ousama.controlador;

import es.dam.ousama.modelo.Estudiante;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class DashboardControlador {

    @FXML private Label lblUser;
    @FXML private Label lblPuntos;
    @FXML private Label lblTiempo;
    @FXML private Button btnAccion;

    private Estudiante estudiante;
    private int tiempoRestante = 1500; // 25 min
    private boolean estudiando = false;
    private Thread hiloTemporizador;

    // Este método lo llamaremos desde el Login para pasarle el nombre del usuario
    public void initData(String nombreUsuario) {
        estudiante = new Estudiante(nombreUsuario);
        lblUser.setText("Estudiante: " + estudiante.getNombre());
        lblPuntos.setText("Puntos de crecimiento: " + estudiante.getPuntosCrecimiento());
    }

    @FXML
    private void gestionarTemporizador() {
        if (!estudiando) {
            estudiando = true;
            btnAccion.setText("Pausar Estudio");
            btnAccion.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-size: 16px;");

            hiloTemporizador = new Thread(() -> {
                while (estudiando && tiempoRestante > 0) {
                    try {
                        Thread.sleep(1000);
                        tiempoRestante--;
                        estudiante.sumarPuntos(1);

                        // Actualizar la interfaz gráfica de forma segura
                        Platform.runLater(() -> {
                            int minutos = tiempoRestante / 60;
                            int segundos = tiempoRestante % 60;
                            lblTiempo.setText(String.format("%02d:%02d", minutos, segundos));
                            lblPuntos.setText("Puntos de crecimiento: " + estudiante.getPuntosCrecimiento());
                        });
                    } catch (InterruptedException ex) {
                        break; // Salimos del bucle si nos interrumpen
                    }
                }
            });
            hiloTemporizador.setDaemon(true);
            hiloTemporizador.start();

        } else {
            estudiando = false;
            if (hiloTemporizador != null) {
                hiloTemporizador.interrupt(); // Pausamos el hilo de forma limpia
            }
            btnAccion.setText("Reanudar Estudio");
            btnAccion.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-size: 16px;");
        }
    }
}