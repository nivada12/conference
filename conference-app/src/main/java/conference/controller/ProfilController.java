package conference.controller;

import conference.dao.impl.UtilisateurDAO;
import conference.model.Utilisateur;
import conference.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.mindrot.jbcrypt.BCrypt;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfilController implements Initializable {

    @FXML private TextField tfNom, tfPrenom, tfEmail, tfTelephone;
    @FXML private PasswordField pfActuel, pfNouveau, pfConfirmer;
    @FXML private Label lblRole;

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        if (u != null) {
            if (tfNom != null) tfNom.setText(u.getNom());
            if (tfPrenom != null) tfPrenom.setText(u.getPrenom());
            if (tfEmail != null) tfEmail.setText(u.getEmail());
            if (tfTelephone != null) tfTelephone.setText(u.getTelephone());
            if (lblRole != null) lblRole.setText(u.getRole().getLibelle());
        }
    }

    @FXML private void handleSauvegarderProfil() {
        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        if (u == null) return;
        u.setNom(tfNom != null ? tfNom.getText().trim() : u.getNom());
        u.setPrenom(tfPrenom != null ? tfPrenom.getText().trim() : u.getPrenom());
        u.setEmail(tfEmail != null ? tfEmail.getText().trim() : u.getEmail());
        u.setTelephone(tfTelephone != null ? tfTelephone.getText() : u.getTelephone());
        if (utilisateurDAO.update(u)) SessionManager.showInfo("Succès", "Profil mis à jour avec succès.");
        else SessionManager.showError("Erreur", "Impossible de mettre à jour le profil.");
    }

    @FXML private void handleChangerMotDePasse() {
        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        if (u == null) return;
        String actuel = pfActuel != null ? pfActuel.getText() : "";
        String nouveau = pfNouveau != null ? pfNouveau.getText() : "";
        String confirmer = pfConfirmer != null ? pfConfirmer.getText() : "";
        if (actuel.isEmpty() || nouveau.isEmpty() || confirmer.isEmpty()) {
            SessionManager.showError("Validation", "Tous les champs sont obligatoires."); return;
        }
        if (!nouveau.equals(confirmer)) {
            SessionManager.showError("Validation", "Les nouveaux mots de passe ne correspondent pas."); return;
        }
        if (nouveau.length() < 6) {
            SessionManager.showError("Validation", "Le mot de passe doit contenir au moins 6 caractères."); return;
        }
        // Vérifier l'ancien mot de passe
        Utilisateur uFrais = utilisateurDAO.findById(u.getId());
        if (uFrais == null || !BCrypt.checkpw(actuel, uFrais.getMotDePasse())) {
            SessionManager.showError("Erreur", "Mot de passe actuel incorrect."); return;
        }
        if (utilisateurDAO.updateMotDePasse(u.getId(), nouveau)) {
            SessionManager.showInfo("Succès", "Mot de passe changé avec succès.");
            if (pfActuel != null) pfActuel.clear();
            if (pfNouveau != null) pfNouveau.clear();
            if (pfConfirmer != null) pfConfirmer.clear();
        } else SessionManager.showError("Erreur", "Impossible de changer le mot de passe.");
    }
}
