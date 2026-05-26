package es.dam.ousama;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {

        // Cargamos el tema claro de AtlantaFX
        Application.setUserAgentStylesheet(new atlantafx.base.theme.PrimerLight().getUserAgentStylesheet());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/es/dam/ousama/vista/Login.fxml"));
        primaryStage.setTitle("StudyBuddy - Acceso");
        primaryStage.setScene(new Scene(loader.load(), 450, 500));
        primaryStage.setResizable(false);

        // Carga el icono empaquetado para que funcione en el IDE y en el futuro .exe
        try {
            javafx.scene.image.Image icono = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/planta_estatica.png"));
            primaryStage.getIcons().add(icono);
        } catch (Exception e) {
            System.out.println("Aviso: No se pudo cargar el icono empaquetado.");
        }

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}