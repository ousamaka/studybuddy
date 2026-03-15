package es.dam.ousama;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Buscamos el archivo FXML en la carpeta resources
        URL rutaFxml = getClass().getResource("/es/dam/ousama/vista/Login.fxml");

        if (rutaFxml == null) {
            throw new RuntimeException("No se ha encontrado el archivo FXML. Revisa la carpeta resources.");
        }

        FXMLLoader loader = new FXMLLoader(rutaFxml);

        primaryStage.setTitle("StudyBuddy");
        primaryStage.setScene(new Scene(loader.load(), 400, 350));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}