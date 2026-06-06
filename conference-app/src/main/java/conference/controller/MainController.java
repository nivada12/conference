package conference.controller;

import conference.MainApp;
import conference.dao.impl.NotificationDAO;
import conference.dao.impl.ReservationDAO;
import conference.dao.impl.SalleDAO;
import conference.dao.impl.EquipementDAO;
import conference.model.Utilisateur;
import conference.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.scene.Node;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Contrôleur principal de l'application
 */
public class MainController implements Initializable {

    @FXML private Label lblNomUtilisateur;
    @FXML private Label lblRole;
    @FXML private Label lblDateTime;
    @FXML private Label lblNotifCount;
    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;

    // Boutons Navigation
    @FXML private Button btnDashboard;
    @FXML private Button btnReservations;
    @FXML private Button btnCalendrier;

    // Boutons Gestion
    @FXML private Button btnSalles;
    @FXML private Button btnEquipements;
    @FXML private Button btnUtilisateurs;

    // Boutons Outils
    @FXML private Button btnRapports;
    @FXML private Button btnAssistantIA;   // ← NOUVEAU
    @FXML private Button btnNotifications;
    @FXML private Button btnProfil;

    // Sections
    @FXML private Label lblGestionSection;
    @FXML private Label lblOutilsSection;
    @FXML private Separator sepGestion1;
    @FXML private Separator sepGestion2;

    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final ReservationDAO  reservationDAO  = new ReservationDAO();
    private final SalleDAO        salleDAO        = new SalleDAO();
    private final EquipementDAO   equipementDAO   = new EquipementDAO();

    private Button activeButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Utilisateur user = SessionManager.getInstance().getUtilisateurConnecte();

        if (user == null) {
            System.err.println("Utilisateur non connecté ! Redirection vers login...");
            redirectToLogin();
            return;
        }

        lblNomUtilisateur.setText(user.getNomComplet());
        lblRole.setText(user.getRole().getLibelle());

        configurerMenusParRole();
        startClock();
        updateNotificationBadge();
        startAutoRefresh();

        loadPage("Dashboard");
        setActiveButton(btnDashboard);
    }

    // ── Redirection Login ─────────────────────────────────────────────────────

    private void redirectToLogin() {
        try {
            MainApp.loadScene("/fxml/Login.fxml", "Connexion - Système de Réservation", 480, 600, false);
        } catch (Exception e) {
            System.err.println("Erreur redirection login: " + e.getMessage());
        }
    }

    // ── Configuration menus par rôle ──────────────────────────────────────────

    private void configurerMenusParRole() {
        SessionManager session = SessionManager.getInstance();

        if (session.estAdmin()) {
            // ADMIN : tout visible
            return;
        }

        if (session.estResponsable()) {
            // RESPONSABLE : masquer uniquement Utilisateurs
            masquerBouton(btnUtilisateurs);
            return;
        }

        if (session.estUtilisateurSimple()) {
            // UTILISATEUR SIMPLE : masquer Gestion + Rapports
            // L'Assistant IA reste visible pour tous les rôles
            masquerBouton(btnSalles);
            masquerBouton(btnEquipements);
            masquerBouton(btnUtilisateurs);
            masquerBouton(btnRapports);
            masquerNode(sepGestion1);
            masquerNode(lblGestionSection);
            masquerNode(sepGestion2);
            // NB : lblOutilsSection et btnAssistantIA restent visibles
        }
    }

    private void masquerBouton(Button btn) {
        if (btn != null) {
            btn.setVisible(false);
            btn.setManaged(false);
        }
    }

    private void masquerNode(Node node) {
        if (node != null) {
            node.setVisible(false);
            node.setManaged(false);
        }
    }

    // ── Horloge & Refresh ─────────────────────────────────────────────────────

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                lblDateTime.setText(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
        ));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void startAutoRefresh() {
        Timeline refresh = new Timeline(new KeyFrame(Duration.minutes(1), e ->
                updateNotificationBadge()
        ));
        refresh.setCycleCount(Timeline.INDEFINITE);
        refresh.play();
    }

    private void updateNotificationBadge() {
        SessionManager session = SessionManager.getInstance();

        if (!session.estConnecte()) {
            lblNotifCount.setVisible(false);
            return;
        }

        int userId = session.getUserId();
        if (userId < 0) {
            lblNotifCount.setVisible(false);
            return;
        }

        try {
            int count = notificationDAO.countNonLues(userId);
            if (count > 0) {
                lblNotifCount.setText(String.valueOf(count));
                lblNotifCount.setVisible(true);
            } else {
                lblNotifCount.setVisible(false);
            }
        } catch (Exception e) {
            System.err.println("Erreur notification: " + e.getMessage());
            lblNotifCount.setVisible(false);
        }
    }

    // ── Chargement de page ────────────────────────────────────────────────────

    private void loadPage(String pageName) {
        try {
            String fxmlPath = "/fxml/" + pageName + ".fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            if (loader.getLocation() == null) {
                throw new IOException("Fichier FXML non trouvé: " + fxmlPath);
            }

            Parent page = loader.load();
            contentArea.getChildren().setAll(page);

        } catch (IOException e) {
            System.err.println("Erreur chargement page " + pageName + ": " + e.getMessage());
            Label placeholder = new Label(
                    "Module " + pageName + "\n(En développement ou erreur de chargement)\n\n" + e.getMessage()
            );
            placeholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-alignment: center;");
            contentArea.getChildren().setAll(placeholder);
        }
    }

    private void setActiveButton(Button btn) {
        if (btn == null) return;
        if (activeButton != null) {
            activeButton.getStyleClass().remove("sidebar-btn-active");
        }
        btn.getStyleClass().add("sidebar-btn-active");
        activeButton = btn;
    }

    // ── Handlers Navigation ───────────────────────────────────────────────────

    @FXML private void handleDashboard() {
        loadPage("Dashboard");
        setActiveButton(btnDashboard);
    }

    @FXML private void handleReservations() {
        loadPage("Reservations");
        setActiveButton(btnReservations);
    }

    @FXML private void handleCalendrier() {
        loadPage("Calendrier");
        setActiveButton(btnCalendrier);
    }

    // ── Handlers Gestion ──────────────────────────────────────────────────────

    @FXML private void handleSalles() {
        if (hasPermission("SALLES")) {
            loadPage("Salles");
            setActiveButton(btnSalles);
        }
    }

    @FXML private void handleEquipements() {
        if (hasPermission("EQUIPEMENTS")) {
            loadPage("Equipements");
            setActiveButton(btnEquipements);
        }
    }

    @FXML private void handleUtilisateurs() {
        if (hasPermission("UTILISATEURS")) {
            loadPage("Utilisateurs");
            setActiveButton(btnUtilisateurs);
        }
    }

    // ── Handlers Outils ───────────────────────────────────────────────────────

    @FXML private void handleRapports() {
        if (hasPermission("RAPPORTS")) {
            loadPage("Rapports");
            setActiveButton(btnRapports);
        }
    }

    @FXML
    private void handleAssistantIA() {
        loadPage("AssistantIA");
        setActiveButton(btnAssistantIA);
    }

    @FXML private void handleNotifications() {
        loadPage("Notifications");
        setActiveButton(btnNotifications);
        lblNotifCount.setVisible(false);
    }

    @FXML private void handleProfil() {
        loadPage("Profil");
        setActiveButton(btnProfil);
    }

    @FXML
    private void handleDeconnexion() {
        if (SessionManager.showConfirm("Déconnexion", "Voulez-vous vraiment vous déconnecter ?")) {
            SessionManager.getInstance().deconnecter();
            redirectToLogin();
        }
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private boolean hasPermission(String permission) {
        boolean allowed = SessionManager.getInstance().hasPermission(permission);
        if (!allowed) {
            SessionManager.showWarning("Accès refusé", "Vous n'avez pas les permissions nécessaires.");
        }
        return allowed;
    }
}