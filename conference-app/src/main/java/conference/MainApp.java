package conference;

import conference.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Application principale - Système de Gestion de Réservation de Salle de Conférence
 */
public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        
        // Test connexion BDD
        if (!DatabaseConfig.getInstance().testConnection()) {
            showDBErrorAndExit();
            return;
        }
        
        loadScene("/fxml/Login.fxml", "Connexion - Système de Réservation", 480, 600, false);
        
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void loadScene(String fxmlPath, String title, int width, int height, boolean resizable) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root, width, height);
        
        // Charger le CSS
        String css = MainApp.class.getResource("/css/styles.css") != null 
                     ? MainApp.class.getResource("/css/styles.css").toExternalForm() : null;
        if (css != null) scene.getStylesheets().add(css);
        
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setResizable(resizable);
        primaryStage.centerOnScreen();
    }

    public static void loadMainApp() throws Exception {
        loadScene("/fxml/Main.fxml", "Système de Gestion de Réservation de Salle de Conférence", 1280, 800, true);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    private void showDBErrorAndExit() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Erreur de connexion");
        alert.setHeaderText("Impossible de se connecter à la base de données");
        alert.setContentText("""
                Vérifiez que:
                • MySQL est démarré
                • La base de données 'conference_db' existe
                • Les paramètres dans database.properties sont corrects
                
                Hôte: localhost:3306
                Base: conference_db
                """);
        alert.showAndWait();
        javafx.application.Platform.exit();
    }

    @Override
    public void stop() {
        DatabaseConfig.getInstance().closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
