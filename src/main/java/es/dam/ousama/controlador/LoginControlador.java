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

/**
 * Controlador de la pantalla de inicio de sesión y registro.
 */
public class LoginControlador {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPass;
    @FXML private Label lblError;

    @FXML
    private void iniciarSesion() {
        String user = txtUsuario.getText();
        String pass = txtPass.getText();

        // Evitamos hacer peticiones a la BD si los campos están vacíos
        if (user.isEmpty() || pass.isEmpty()) {
            actualizarMensaje("Rellena todos los campos", "danger");
            return;
        }

        actualizarMensaje("Conectando a la nube...", "accent");

        // Hilo secundario para que la interfaz no se congele durante la conexión a Mongo
        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            Estudiante est = dao.login(user, pass);

            // Volvemos al hilo principal para tocar elementos de JavaFX
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

    @FXML
    private void registrarUsuario() {
        String user = txtUsuario.getText();
        String pass = txtPass.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            actualizarMensaje("Rellena todos los campos", "danger");
            return;
        }

        actualizarMensaje("Creando cuenta...", "accent");

        // Mismo sistema de hilos que en el login para evitar bloqueos visuales
        new Thread(() -> {
            UsuarioDAO dao = new UsuarioDAO();
            boolean exito = dao.registrar(user, pass);

            Platform.runLater(() -> {
                if (exito) {
                    actualizarMensaje("¡Cuenta creada! Ya puedes iniciar sesión", "success");
                    txtPass.clear(); // Limpiamos la contraseña por seguridad
                } else {
                    actualizarMensaje("El nombre de usuario ya existe", "danger");
                }
                dao.cerrarConexion();
            });
        }).start();
    }

    private void irAlDashboard(String nombre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/es/dam/ousama/vista/Dashboard.fxml"));
            Parent root = loader.load();

            // Inyectamos el nombre de usuario al controlador del dashboard antes de mostrar la escena
            DashboardControlador dash = loader.getController();
            dash.initData(nombre);

            Stage stage = (Stage) txtUsuario.getScene().getWindow();

            // Pequeño apaño para que JavaFX recalcule bien la ventana maximizada
            // Si no se quita el maximizado primero, a veces arrastra bordes raros en Windows
            stage.setMaximized(false);
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.setMaximized(true);
            stage.setTitle("StudyBuddy - Dashboard");

        } catch (Exception e) {
            e.printStackTrace();
            actualizarMensaje("Error al cargar la aplicación", "danger");
        }
    }

    /**
     * Método auxiliar para cambiar textos y colores del label de notificaciones dinámicamente.
     */
    private void actualizarMensaje(String texto, String clase) {
        lblError.getStyleClass().removeAll("danger", "accent", "success");
        lblError.getStyleClass().add(clase);
        lblError.setText(texto);
    }
}