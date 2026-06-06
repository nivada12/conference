package conference.controller;

import conference.MainApp;
import conference.dao.impl.UtilisateurDAO;
import conference.model.Utilisateur;
import conference.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur de la page de connexion
 */
public class LoginController implements Initializable {

    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private Button btnConnexion;
    @FXML private Label lblError;
    @FXML private VBox loginBox;
    @FXML private ProgressIndicator progressIndicator;

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setVisible(false);
        progressIndicator.setVisible(false);
        
        // Animation d'entrée
        FadeTransition ft = new FadeTransition(Duration.millis(800), loginBox);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
        
        // Appuyer sur Entrée pour se connecter
        pfPassword.setOnAction(e -> handleConnexion());
        tfEmail.setOnAction(e -> pfPassword.requestFocus());
        
        // Pré-remplir pour démo
        tfEmail.setText("amine@email.com");
        pfPassword.setText("0000");
    }

    @FXML
    private void handleConnexion() {
        String email = tfEmail.getText().trim();
        String password = pfPassword.getText();
        
        lblError.setVisible(false);
        
        // Validation basique
        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez saisir votre email et mot de passe.");
            return;
        }
        
        if (!email.contains("@")) {
            showError("Adresse email invalide.");
            return;
        }
        
        // Afficher le loader
        progressIndicator.setVisible(true);
        btnConnexion.setDisable(true);
        
        // Authentification dans un thread séparé pour ne pas bloquer l'UI
        javafx.concurrent.Task<Utilisateur> task = new javafx.concurrent.Task<>() {
            @Override
            protected Utilisateur call() {
                return utilisateurDAO.authentifier(email, password);
            }
        };
        
        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            btnConnexion.setDisable(false);
            Utilisateur user = task.getValue();
            if (user != null) {
                SessionManager.getInstance().connecter(user);
                try {
                    MainApp.loadMainApp();
                } catch (Exception ex) {
                    showError("Erreur lors du chargement de l'application: " + ex.getMessage());
                }
            } else {
                showError("Email ou mot de passe incorrect.");
                pfPassword.clear();
                pfPassword.requestFocus();
                // Shake animation
                shakeAnimation();
            }
        });
        
        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            btnConnexion.setDisable(false);
            showError("Erreur de connexion à la base de données.");
        });
        
        new Thread(task).start();
    }

    private void showError(String message) {
        lblError.setText("⚠ " + message);
        lblError.setVisible(true);
        
        FadeTransition ft = new FadeTransition(Duration.millis(300), lblError);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void shakeAnimation() {
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(80), loginBox);
        tt.setByX(10);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.play();
    }

    @FXML
    private void handleQuitter() {
        javafx.application.Platform.exit();
    }
}
