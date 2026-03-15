package es.dam.ousama.controlador;

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

    @FXML
    private void iniciarSesion() {
        String user = txtUsuario.getText();
        String pass = txtPass.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            lblError.setStyle("-fx-text-fill: red;");
            lblError.setText("Por favor, rellena todos los datos.");
        } else {
            try {
                // 1. Cargamos el nuevo archivo FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/es/dam/ousama/vista/Dashboard.fxml"));
                Parent root = loader.load();

                // 2. Le pasamos el nombre de usuario al nuevo controlador
                DashboardControlador controladorDashboard = loader.getController();
                controladorDashboard.initData(user);

                // 3. Obtenemos la ventana actual (Stage) y le ponemos la nueva escena
                Stage stage = (Stage) txtUsuario.getScene().getWindow();
                stage.setScene(new Scene(root, 500, 400));

            } catch (Exception e) {
                e.printStackTrace();
                lblError.setText("Error al cargar la pantalla principal.");
            }
        }
    }
}