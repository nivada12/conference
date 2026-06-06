package conference.controller;

import conference.dao.impl.*;
import conference.model.Reservation;
import conference.model.Salle;
import conference.model.Utilisateur;
import conference.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contrôleur du tableau de bord
 */
public class DashboardController implements Initializable {

    @FXML private Label lblTotalReservations;
    @FXML private Label lblSallesDisponibles;
    @FXML private Label lblReservationsAujourdHui;
    @FXML private Label lblEquipementsDefectueux;
    @FXML private BarChart<String, Number> chartReservations;
    @FXML private PieChart chartSalles;
    @FXML private TableView<Reservation> tableProchaines;
    @FXML private TableColumn<Reservation, String> colCode;
    @FXML private TableColumn<Reservation, String> colTitre;
    @FXML private TableColumn<Reservation, String> colSalle;
    @FXML private TableColumn<Reservation, String> colDate;
    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private Label lblBienvenue;
    @FXML private TextArea inputAI;
    @FXML private TextArea outputAI;

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final SalleDAO salleDAO = new SalleDAO();
    private final EquipementDAO equipementDAO = new EquipementDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private boolean iaEnCours = false;

    private int userId;
    private boolean estAdmin;
    private boolean estResponsable;
    private boolean estUtilisateurSimple;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Récupérer infos session
        Utilisateur user = SessionManager.getInstance().getUtilisateurConnecte();
        userId = SessionManager.getInstance().getUserId();
        estAdmin = SessionManager.getInstance().estAdmin();
        estResponsable = SessionManager.getInstance().estResponsable();
        estUtilisateurSimple = SessionManager.getInstance().estUtilisateurSimple();

        // Message de bienvenue personnalisé
        String nom = user.getNomComplet();
        String roleLabel = user.getRole().getLibelle();

        if (estUtilisateurSimple) {
            lblBienvenue.setText("Bonjour, " + nom + " ! Voici votre espace personnel.");
        } else {
            lblBienvenue.setText("Bonjour, " + nom + " (" + roleLabel + ") ! Bienvenue sur le tableau de bord.");
        }

        loadStats();
        loadChartReservations();
        loadChartSalles();
        loadProchainesReservations();
    }

    /**
     * Charge les statistiques selon le rôle
     */
    private void loadStats() {
        if (estUtilisateurSimple) {
            // 👤 UTILISATEUR : Voir uniquement SES stats
            int mesReservations = reservationDAO.countByUtilisateur(userId);
            int mesReservationsAujourdHui = reservationDAO.countByUtilisateurAujourdHui(userId);

            lblTotalReservations.setText(String.valueOf(mesReservations));
            lblReservationsAujourdHui.setText(String.valueOf(mesReservationsAujourdHui));

            // Masquer les stats non pertinentes ou les adapter
            lblSallesDisponibles.setText("—"); // Ou nombre de salles utilisées par l'utilisateur
            lblEquipementsDefectueux.setText("—"); // Non visible pour utilisateur simple

        } else {
            // 👑 ADMIN / RESPONSABLE : Voir toutes les stats globales
            int totalRes = reservationDAO.countTotal();
            int totalSalles = salleDAO.countTotal();
            int resAujourdHui = reservationDAO.countAujourdHui();
            int eqDefectueux = equipementDAO.countDefectueux();

            lblTotalReservations.setText(String.valueOf(totalRes));
            lblSallesDisponibles.setText(String.valueOf(totalSalles));
            lblReservationsAujourdHui.setText(String.valueOf(resAujourdHui));
            lblEquipementsDefectueux.setText(String.valueOf(eqDefectueux));
        }
    }

    /**
     * Charge le graphique des réservations selon le rôle
     */
    private void loadChartReservations() {
        int annee = LocalDateTime.now().getYear();
        List<Object[]> data;

        if (estUtilisateurSimple) {
            // 👤 Stats personnelles par mois
            data = reservationDAO.getReservationsParMoisByUtilisateur(userId, annee);
        } else {
            // 👑 Stats globales par mois
            data = reservationDAO.getReservationsParMois(annee);
        }

        String[] mois = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(estUtilisateurSimple ? "Mes réservations " + annee : "Réservations " + annee);

        // Initialiser à 0 pour tous les mois
        int[] counts = new int[12];
        for (Object[] row : data) {
            int moisIdx = (int) row[0] - 1;
            counts[moisIdx] = (int) row[1];
        }

        for (int i = 0; i < 12; i++) {
            series.getData().add(new XYChart.Data<>(mois[i], counts[i]));
        }

        chartReservations.getData().clear();
        chartReservations.getData().add(series);
        chartReservations.setLegendVisible(false);
    }

    /**
     * Charge le graphique des salles selon le rôle
     */
    private void loadChartSalles() {
        List<Object[]> stats;

        if (estUtilisateurSimple) {
            // 👤 Salles que l'utilisateur a réservées
            stats = reservationDAO.getStatistiquesParSalleByUtilisateur(userId);
        } else {
            // 👑 Stats globales par salle
            stats = reservationDAO.getStatistiquesParSalle();
        }

        chartSalles.getData().clear();

        if (stats.isEmpty()) {
            chartSalles.getData().add(new PieChart.Data("Aucune donnée", 1));
            return;
        }

        for (Object[] row : stats) {
            String nomSalle = (String) row[0];
            int total = (int) row[1];
            if (total > 0) {
                chartSalles.getData().add(new PieChart.Data(nomSalle + " (" + total + ")", total));
            }
        }

        if (chartSalles.getData().isEmpty()) {
            chartSalles.getData().add(new PieChart.Data("Aucune réservation", 1));
        }
    }

    /**
     * Charge les prochaines réservations selon le rôle
     */
    private void loadProchainesReservations() {
        List<Reservation> reservations;

        if (estUtilisateurSimple) {
            // 👤 Uniquement MES réservations
            reservations = reservationDAO.findByUtilisateur(userId);
        } else {
            // 👑 Toutes les réservations (Admin/Responsable)
            reservations = reservationDAO.findAllComplet();
        }

        // Filtrer les réservations à venir (max 10)
        List<Reservation> prochaines = new ArrayList<>();
        LocalDateTime maintenant = LocalDateTime.now();

        for (Reservation r : reservations) {
            if (r.getDateDebut() != null && r.getDateDebut().isAfter(maintenant) &&
                    r.getStatut() != Reservation.StatutReservation.ANNULE) {
                prochaines.add(r);
                if (prochaines.size() >= 10) break;
            }
        }

        // Configurer les colonnes
        colCode.setCellValueFactory(new PropertyValueFactory<>("codeReservation"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));

        colSalle.setCellValueFactory(cd -> {
            Salle s = cd.getValue().getSalle();
            return new javafx.beans.property.SimpleStringProperty(s != null ? s.getNom() : "N/A");
        });

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colDate.setCellValueFactory(cd -> {
            LocalDateTime dt = cd.getValue().getDateDebut();
            return new javafx.beans.property.SimpleStringProperty(dt != null ? dt.format(fmt) : "");
        });

        colStatut.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatut().getLibelle()));

        // Coloriser les statuts
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Reservation r = getTableView().getItems().get(getIndex());
                    if (r != null) {
                        String color = r.getStatut().getCouleur();
                        setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tableProchaines.getItems().setAll(prochaines);
    }

    @FXML
    private void envoyerQuestionIA() {
        String question = inputAI.getText().trim();
        if (question.isEmpty() || iaEnCours) return;

        iaEnCours = true;
        outputAI.setText("⏳ L'assistant réfléchit...");

        // Appel IA en arrière-plan
        conference.controller.AssistantIAController
                .questionRapide(question)
                .thenAccept(reponse ->
                        javafx.application.Platform.runLater(() -> {
                            outputAI.setText(reponse);
                            iaEnCours = false;
                        })
                );
    }

    /**
     * Efface les zones de saisie et de réponse IA du Dashboard
     * (appelé par onAction="#clearAI" dans Dashboard.fxml)
     */
    @FXML
    private void clearAI() {
        inputAI.clear();
        outputAI.clear();
        iaEnCours = false;
    }

}