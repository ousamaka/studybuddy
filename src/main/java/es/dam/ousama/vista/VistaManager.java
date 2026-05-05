package es.dam.ousama.vista;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

// Gestor de vistas montado a pelo (sin FXML).
// Lo dejo así para pantallas rápidas, aunque los estilos hardcodeados son un poco dolor de cabeza para mantener.
public class VistaManager {

    // --- Componentes del Login ---
    // Los dejo en public para que el Controlador los pueda enganchar directamente sin hacer mil getters
    public TextField txtUsuario = new TextField();
    public PasswordField txtPass = new PasswordField();
    public Button btnEntrar = new Button("Acceder y Plantar");
    public Label lblError = new Label();

    // --- Componentes de la pantalla Principal ---
    public Label lblUser = new Label();
    public Label lblPuntos = new Label();
    public Label lblTiempo = new Label("25:00"); // 25 min por defecto por la técnica Pomodoro
    public Button btnAccion = new Button("Iniciar Estudio");

    // Montamos la escena de Login paso a paso
    public Scene crearEscenaLogin() {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));

        // Fondo verdecito claro para que pegue con el rollo de las plantas
        layout.setStyle("-fx-background-color: #e8f5e9;");

        Label titulo = new Label("Bienvenido a StudyBuddy");
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titulo.setStyle("-fx-text-fill: #2e7d32;");

        // Capamos el ancho de los inputs para que no se estiren deformes por toda la pantalla
        txtUsuario.setPromptText("Nombre de usuario");
        txtUsuario.setMaxWidth(200);
        txtPass.setPromptText("Contraseña");
        txtPass.setMaxWidth(200);

        btnEntrar.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        lblError.setStyle("-fx-text-fill: red;"); // Mensajes de error en rojo chillón para que se vean bien

        // Apilamos todo en el layout y pa' lante
        layout.getChildren().addAll(titulo, txtUsuario, txtPass, btnEntrar, lblError);
        return new Scene(layout, 400, 350);
    }

    // Montamos la escena del temporizador (la versión básica)
    public Scene crearEscenaPrincipal() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #ffffff;");

        lblUser.setFont(Font.font("Arial", 16));
        lblPuntos.setStyle("-fx-text-fill: #f57c00; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Le meto Courier New al reloj porque al ser monoespaciada evita que el texto baile al cambiar los segundos
        lblTiempo.setFont(Font.font("Courier New", FontWeight.BOLD, 60));
        lblTiempo.setStyle("-fx-text-fill: #388e3c;");

        btnAccion.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");

        layout.getChildren().addAll(lblUser, lblPuntos, lblTiempo, btnAccion);
        return new Scene(layout, 500, 400);
    }
}