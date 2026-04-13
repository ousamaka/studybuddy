package es.dam.ousama.controlador;

import es.dam.ousama.modelo.Estudiante;
import es.dam.ousama.modelo.dao.UsuarioDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginControlador {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPass;
    @FXML private Label lblError;

    // --- BOTÓN INICIAR SESIÓN ---
    @FXML
    private void iniciarSesion() {
        String user = txtUsuario.getText();
        String pass = txtPass.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            actualizarMensaje("Rellena todos los campos", "danger");
            return;
        }

        actualizarMensaje("Conectando a la nube...", "accent");

        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            Estudiante est = dao.login(user, pass);
            Platform.runLater(() -> {
                if (est != null) {
                    irAlDashboard(est.getNombre());
                } else {
                    actualizarMensaje("Usuario o contraseña incorrectos", "danger");
                }
                dao.cerrarConexion();
            });
        }).start();
    }

    // --- BOTÓN REGISTRARSE (Este es el que te faltaba) ---
    @FXML
    private void registrarUsuario() {
        String user = txtUsuario.getText();
        String pass = txtPass.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            actualizarMensaje("Rellena todos los campos", "danger");
            return;
        }

        actualizarMensaje("Creando cuenta...", "accent");

        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            boolean exito = dao.registrar(user, pass);
            Platform.runLater(() -> {
                if (exito) {
                    actualizarMensaje("¡Cuenta creada! Ya puedes iniciar sesión", "success");
                    txtPass.clear(); // Borramos la clave por seguridad
                } else {
                    actualizarMensaje("El nombre de usuario ya existe", "danger");
                }
                dao.cerrarConexion();
            });
        }).start();
    }

    // --- UTILIDADES ---
    private void irAlDashboard(String nombre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/es/dam/ousama/vista/Dashboard.fxml"));
            Parent root = loader.load();
            DashboardControlador dash = loader.getController();
            dash.initData(nombre);

            Stage stage = (Stage) txtUsuario.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.setMaximized(true); // PANTALLA COMPLETA AUTOMÁTICA
            stage.setTitle("StudyBuddy - Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
            actualizarMensaje("Error al cargar la aplicación", "danger");
        }
    }

    private void actualizarMensaje(String texto, String clase) {
        lblError.getStyleClass().removeAll("danger", "accent", "success");
        lblError.getStyleClass().add(clase);
        lblError.setText(texto);
    }
}