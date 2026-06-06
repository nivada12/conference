package conference.util;

import conference.model.Utilisateur;
import conference.model.Utilisateur.Role;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Gestionnaire de session utilisateur (Singleton)
 */
public class SessionManager {

    private static SessionManager instance;
    private Utilisateur utilisateurConnecte;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void connecter(Utilisateur utilisateur) {
        this.utilisateurConnecte = utilisateur;
    }

    public void deconnecter() {
        this.utilisateurConnecte = null;
    }

    public Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public boolean estConnecte() {
        return utilisateurConnecte != null;
    }

    // ===== VÉRIFICATION DES RÔLES =====

    public boolean estAdmin() {
        return estConnecte() && utilisateurConnecte.getRole() == Role.ADMIN;
    }

    public boolean estResponsable() {
        return estConnecte() && utilisateurConnecte.getRole() == Role.RESPONSABLE;
    }

    public boolean estUtilisateurSimple() {
        return estConnecte() && utilisateurConnecte.getRole() == Role.UTILISATEUR;
    }

    // ===== VÉRIFICATION DES PERMISSIONS =====

    public boolean hasPermission(String permission) {
        if (!estConnecte()) return false;

        Role role = utilisateurConnecte.getRole();

        switch (permission) {
            case "SALLES":
            case "EQUIPEMENTS":
                // Admin et Responsable peuvent gérer
                return role == Role.ADMIN || role == Role.RESPONSABLE;

            case "UTILISATEURS":
                // Seul l'admin peut gérer les utilisateurs
                return role == Role.ADMIN;

            case "RAPPORTS":
                // Admin et Responsable peuvent voir les rapports
                return role == Role.ADMIN || role == Role.RESPONSABLE;

            case "TOUTES_RESERVATIONS":
                // Admin et Responsable voient tout
                return role == Role.ADMIN || role == Role.RESPONSABLE;

            case "MODIFIER_TOUTES_RESERVATIONS":
                // Seul l'admin peut tout modifier
                return role == Role.ADMIN;

            default:
                return true; // Par défaut, autoriser
        }
    }

    public int getUserId() {
        return estConnecte() ? utilisateurConnecte.getId() : -1;
    }

    // ===== MÉTHODES UTILITAIRES ALERTES =====

    public static void showInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showError(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showWarning(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean showConfirm(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}