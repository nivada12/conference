package conference.controller;

import conference.dao.impl.UtilisateurDAO;
import conference.model.Utilisateur;
import conference.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class UtilisateursController implements Initializable {

    @FXML private TableView<Utilisateur> tableUtilisateurs;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colPrenom;
    @FXML private TableColumn<Utilisateur, String> colEmail;
    @FXML private TableColumn<Utilisateur, String> colTelephone;
    @FXML private TableColumn<Utilisateur, String> colRole;
    @FXML private TableColumn<Utilisateur, String> colStatut;
    @FXML private TableColumn<Utilisateur, String> colDerniereConnexion;

    @FXML private VBox formPane;
    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private TextField tfTelephone;
    @FXML private PasswordField pfMotDePasse;
    @FXML private PasswordField pfConfirmMotDePasse;
    @FXML private ComboBox<String> cbRole;
    @FXML private CheckBox chkActif;
    @FXML private Label lblFormTitre;
    @FXML private TextField tfRecherche;

    // Labels d'erreur
    @FXML private Label lblNomError;
    @FXML private Label lblPrenomError;
    @FXML private Label lblEmailError;
    @FXML private Label lblTelephoneError;
    @FXML private Label lblPasswordError;
    @FXML private Label lblConfirmPasswordError;
    @FXML private Label lblInfo;

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private List<Utilisateur> tousLesUtilisateurs;
    private Utilisateur utilisateurEnEdition = null;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupForm();
        setupValidationListeners();
        loadUtilisateurs();
        if (formPane != null) formPane.setVisible(false);
        if (tfRecherche != null) tfRecherche.setOnKeyReleased(e -> filtrer());
    }

    private void setupTable() {
        if (colNom != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colPrenom != null) colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        if (colEmail != null) colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (colTelephone != null) colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        if (colRole != null) colRole.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getRole().getLibelle()));
        if (colStatut != null) colStatut.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().isActif() ? "✅ Actif" : "❌ Inactif"));
        if (colDerniereConnexion != null) {
            colDerniereConnexion.setCellValueFactory(cd -> {
                if (cd.getValue().getDerniereConnexion() != null) {
                    return new SimpleStringProperty(cd.getValue().getDerniereConnexion().format(dateFormatter));
                }
                return new SimpleStringProperty("Jamais");
            });
        }

        if (tableUtilisateurs != null) {
            tableUtilisateurs.setRowFactory(tv -> {
                TableRow<Utilisateur> row = new TableRow<>();
                row.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2 && !row.isEmpty()) handleEditer(row.getItem());
                });
                return row;
            });

            ContextMenu menu = new ContextMenu();
            MenuItem miEditer = new MenuItem("✏ Modifier");
            MenuItem miReinitialiserMdp = new MenuItem("🔑 Réinitialiser mot de passe");
            MenuItem miSupprimer = new MenuItem("🗑 Désactiver");

            miEditer.setOnAction(e -> {
                Utilisateur u = tableUtilisateurs.getSelectionModel().getSelectedItem();
                if (u != null) handleEditer(u);
            });

            miReinitialiserMdp.setOnAction(e -> {
                Utilisateur u = tableUtilisateurs.getSelectionModel().getSelectedItem();
                if (u != null) handleReinitialiserMotDePasse(u);
            });

            miSupprimer.setOnAction(e -> {
                Utilisateur u = tableUtilisateurs.getSelectionModel().getSelectedItem();
                if (u != null) handleSupprimer(u);
            });

            menu.getItems().addAll(miEditer, miReinitialiserMdp, new SeparatorMenuItem(), miSupprimer);
            tableUtilisateurs.setContextMenu(menu);
        }
    }

    private void setupForm() {
        if (cbRole != null) {
            cbRole.getItems().addAll("ADMIN", "RESPONSABLE", "UTILISATEUR");
            cbRole.setValue("UTILISATEUR");
        }
        if (chkActif != null) chkActif.setSelected(true);
    }

    private void setupValidationListeners() {
        // Validation en temps réel
        tfNom.textProperty().addListener((obs, old, val) -> validateNom());
        tfPrenom.textProperty().addListener((obs, old, val) -> validatePrenom());
        tfEmail.textProperty().addListener((obs, old, val) -> validateEmail());
        tfTelephone.textProperty().addListener((obs, old, val) -> validateTelephone());
        pfMotDePasse.textProperty().addListener((obs, old, val) -> validatePassword());
        pfConfirmMotDePasse.textProperty().addListener((obs, old, val) -> validateConfirmPassword());
    }

    private boolean validateNom() {
        String nom = tfNom.getText().trim();
        if (nom.isEmpty()) {
            showError(lblNomError, "Le nom est obligatoire");
            return false;
        }
        if (nom.length() < 2) {
            showError(lblNomError, "Le nom doit contenir au moins 2 caractères");
            return false;
        }
        clearError(lblNomError);
        return true;
    }

    private boolean validatePrenom() {
        String prenom = tfPrenom.getText().trim();
        if (prenom.isEmpty()) {
            showError(lblPrenomError, "Le prénom est obligatoire");
            return false;
        }
        if (prenom.length() < 2) {
            showError(lblPrenomError, "Le prénom doit contenir au moins 2 caractères");
            return false;
        }
        clearError(lblPrenomError);
        return true;
    }

    private boolean validateEmail() {
        String email = tfEmail.getText().trim();
        if (email.isEmpty()) {
            showError(lblEmailError, "L'email est obligatoire");
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!Pattern.matches(emailRegex, email)) {
            showError(lblEmailError, "Email invalide (ex: nom@domaine.com)");
            return false;
        }
        clearError(lblEmailError);
        return true;
    }

    private boolean validateTelephone() {
        String telephone = tfTelephone.getText().trim();
        if (telephone.isEmpty()) {
            clearError(lblTelephoneError);
            return true; // Téléphone optionnel
        }
        String phoneRegex = "^[+][0-9]{10,15}$|^[0-9]{8,15}$";
        if (!Pattern.matches(phoneRegex, telephone)) {
            showError(lblTelephoneError, "Téléphone invalide (ex: +21612345678 ou 12345678)");
            return false;
        }
        clearError(lblTelephoneError);
        return true;
    }

    private boolean validatePassword() {
        String password = pfMotDePasse.getText();

        // En modification, si le champ est vide, c'est optionnel
        if (utilisateurEnEdition != null && (password == null || password.isEmpty())) {
            clearError(lblPasswordError);
            return true;
        }

        // Nouvel utilisateur ou changement de mot de passe
        if (password == null || password.isEmpty()) {
            showError(lblPasswordError, "Le mot de passe est obligatoire");
            return false;
        }
        if (password.length() < 4) {
            showError(lblPasswordError, "Le mot de passe doit contenir au moins 4 caractères");
            return false;
        }
        clearError(lblPasswordError);
        return true;
    }

    private boolean validateConfirmPassword() {
        String password = pfMotDePasse.getText();
        String confirm = pfConfirmMotDePasse.getText();

        // En modification, si les deux sont vides, c'est optionnel
        if (utilisateurEnEdition != null && (password == null || password.isEmpty()) && (confirm == null || confirm.isEmpty())) {
            clearError(lblConfirmPasswordError);
            return true;
        }

        // Nouvel utilisateur ou changement de mot de passe
        if (!password.equals(confirm)) {
            showError(lblConfirmPasswordError, "Les mots de passe ne correspondent pas");
            return false;
        }
        clearError(lblConfirmPasswordError);
        return true;
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
    }

    private void clearError(Label label) {
        label.setText("");
        label.setVisible(false);
    }

    private void clearAllErrors() {
        clearError(lblNomError);
        clearError(lblPrenomError);
        clearError(lblEmailError);
        clearError(lblTelephoneError);
        clearError(lblPasswordError);
        clearError(lblConfirmPasswordError);
        if (lblInfo != null) lblInfo.setVisible(false);
    }

    private boolean validateForm() {
        clearAllErrors();
        return validateNom() && validatePrenom() && validateEmail() &&
                validateTelephone() && validatePassword() && validateConfirmPassword();
    }

    private void loadUtilisateurs() {
        tousLesUtilisateurs = utilisateurDAO.findAll();
        if (tableUtilisateurs != null) tableUtilisateurs.getItems().setAll(tousLesUtilisateurs);
    }

    private void filtrer() {
        String recherche = tfRecherche.getText().toLowerCase().trim();
        if (recherche.isEmpty()) {
            tableUtilisateurs.getItems().setAll(tousLesUtilisateurs);
        } else {
            tableUtilisateurs.getItems().setAll(
                    tousLesUtilisateurs.stream()
                            .filter(u -> u.getNom().toLowerCase().contains(recherche) ||
                                    u.getPrenom().toLowerCase().contains(recherche) ||
                                    u.getEmail().toLowerCase().contains(recherche))
                            .toList()
            );
        }
    }

    @FXML
    private void handleNouvelUtilisateur() {
        utilisateurEnEdition = null;
        clearForm();
        clearAllErrors();
        if (lblFormTitre != null) lblFormTitre.setText("Nouvel Utilisateur");
        if (formPane != null) formPane.setVisible(true);
        pfMotDePasse.setDisable(false);
        pfConfirmMotDePasse.setDisable(false);
        if (lblInfo != null) {
            lblInfo.setText("💡 Le mot de passe est obligatoire pour un nouvel utilisateur");
            lblInfo.setVisible(true);
        }
    }

    private void handleEditer(Utilisateur u) {
        utilisateurEnEdition = u;
        clearAllErrors();
        if (lblFormTitre != null) lblFormTitre.setText("Modifier: " + u.getNom() + " " + u.getPrenom());
        if (tfNom != null) tfNom.setText(u.getNom());
        if (tfPrenom != null) tfPrenom.setText(u.getPrenom());
        if (tfEmail != null) tfEmail.setText(u.getEmail());
        if (tfTelephone != null) tfTelephone.setText(u.getTelephone());
        if (cbRole != null) cbRole.setValue(u.getRole().name());
        if (chkActif != null) chkActif.setSelected(u.isActif());

        // En modification, les champs mot de passe sont optionnels
        if (pfMotDePasse != null) pfMotDePasse.clear();
        if (pfConfirmMotDePasse != null) pfConfirmMotDePasse.clear();

        if (formPane != null) formPane.setVisible(true);
        if (lblInfo != null) {
            lblInfo.setText("💡 Laissez vide pour garder l'ancien mot de passe");
            lblInfo.setVisible(true);
        }
    }

    private void handleReinitialiserMotDePasse(Utilisateur u) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Réinitialiser mot de passe");
        dialog.setHeaderText("Utilisateur: " + u.getNom() + " " + u.getPrenom());
        dialog.setContentText("Nouveau mot de passe (minimum 4 caractères):");

        dialog.showAndWait().ifPresent(nouveauMdp -> {
            if (nouveauMdp != null && !nouveauMdp.trim().isEmpty()) {
                if (nouveauMdp.length() >= 4) {
                    if (utilisateurDAO.updateMotDePasse(u.getId(), nouveauMdp)) {
                        SessionManager.showInfo("Succès", "Mot de passe réinitialisé avec succès !");
                    } else {
                        SessionManager.showError("Erreur", "Impossible de réinitialiser le mot de passe.");
                    }
                } else {
                    SessionManager.showError("Erreur", "Le mot de passe doit contenir au moins 4 caractères.");
                }
            }
        });
    }

    private void handleSupprimer(Utilisateur u) {
        if (SessionManager.showConfirm("Désactiver l'utilisateur", "Désactiver l'utilisateur \"" + u.getNom() + " " + u.getPrenom() + "\" ?")) {
            if (utilisateurDAO.delete(u.getId())) {
                SessionManager.showInfo("Succès", "Utilisateur désactivé.");
                loadUtilisateurs();
            }
        }
    }

    @FXML
    private void handleSauvegarder() {
        // Validation du formulaire
        if (!validateForm()) {
            SessionManager.showWarning("Validation", "Veuillez corriger les erreurs dans le formulaire.");
            return;
        }

        String nom = tfNom.getText().trim();
        String prenom = tfPrenom.getText().trim();
        String email = tfEmail.getText().trim();
        String telephone = tfTelephone.getText().trim();
        String motDePasse = pfMotDePasse.getText();
        String role = cbRole.getValue();
        boolean actif = chkActif.isSelected();

        // Vérifier si l'email existe déjà (sauf pour l'utilisateur en cours d'édition)
        if (utilisateurEnEdition == null || !utilisateurEnEdition.getEmail().equals(email)) {
            if (utilisateurDAO.emailExists(email)) {
                showError(lblEmailError, "Cet email est déjà utilisé par un autre compte");
                SessionManager.showWarning("Validation", "Cet email est déjà utilisé.");
                return;
            }
        }

        Utilisateur u = utilisateurEnEdition != null ? utilisateurEnEdition : new Utilisateur();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setTelephone(telephone);
        u.setRole(Utilisateur.Role.valueOf(role));
        u.setActif(actif);

        boolean succes;

        if (utilisateurEnEdition != null) {
            // Modification
            succes = utilisateurDAO.update(u);

            // Si un nouveau mot de passe a été saisi, le mettre à jour
            if (motDePasse != null && !motDePasse.isEmpty()) {
                succes = utilisateurDAO.updateMotDePasse(u.getId(), motDePasse);
                if (succes) {
                    SessionManager.showInfo("Succès", "Utilisateur modifié avec son nouveau mot de passe.");
                }
            } else {
                if (succes) {
                    SessionManager.showInfo("Succès", "Utilisateur modifié (mot de passe inchangé).");
                }
            }
        } else {
            // Nouvel utilisateur
            u.setMotDePasse(motDePasse);
            succes = utilisateurDAO.save(u);
            if (succes) {
                SessionManager.showInfo("Succès", "Utilisateur créé avec succès !");
            }
        }

        if (succes) {
            if (formPane != null) formPane.setVisible(false);
            loadUtilisateurs();
            clearForm();
        } else {
            SessionManager.showError("Erreur", "Impossible de sauvegarder l'utilisateur.");
        }
    }

    @FXML
    private void handleAnnulerForm() {
        if (formPane != null) formPane.setVisible(false);
        clearForm();
        clearAllErrors();
    }

    @FXML
    private void handleRafraichir() {
        loadUtilisateurs();
    }

    private void clearForm() {
        if (tfNom != null) tfNom.clear();
        if (tfPrenom != null) tfPrenom.clear();
        if (tfEmail != null) tfEmail.clear();
        if (tfTelephone != null) tfTelephone.clear();
        if (pfMotDePasse != null) pfMotDePasse.clear();
        if (pfConfirmMotDePasse != null) pfConfirmMotDePasse.clear();
        if (cbRole != null) cbRole.setValue("UTILISATEUR");
        if (chkActif != null) chkActif.setSelected(true);
        utilisateurEnEdition = null;
    }
}