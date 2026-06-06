package conference.controller;

import conference.dao.impl.ReservationDAO;
import conference.dao.impl.SalleDAO;
import conference.dao.impl.EquipementDAO;
import conference.dao.impl.NotificationDAO;
import conference.model.*;
import conference.model.Notification;
import conference.model.Reservation;
import conference.model.Salle;
import conference.util.SessionManager;
import conference.model.Equipement;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contrôleur de gestion des réservations
 */
public class ReservationsController implements Initializable {

    @FXML private TableView<Reservation> tableReservations;
    @FXML private TableColumn<Reservation, String> colCode;
    @FXML private TableColumn<Reservation, String> colTitre;
    @FXML private TableColumn<Reservation, String> colSalle;
    @FXML private TableColumn<Reservation, String> colUser;
    @FXML private TableColumn<Reservation, String> colDateDebut;
    @FXML private TableColumn<Reservation, String> colDateFin;
    @FXML private TableColumn<Reservation, String> colDuree;
    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private TableColumn<Reservation, String> colParticipants;

    // Formulaire de réservation
    @FXML private VBox formPane;
    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<Salle> cbSalle;
    @FXML private DatePicker dpDateDebut;
    @FXML private ComboBox<String> cbHeureDebut;
    @FXML private ComboBox<String> cbHeureFin;
    @FXML private Spinner<Integer> spParticipants;
    @FXML private ComboBox<Salle.Disposition> cbDisposition;
    @FXML private TextArea taCommentaires;
    @FXML private ListView<CheckBox> lvEquipements;
    @FXML private Label lblDisponibilite;
    @FXML private Button btnSauvegarder;
    @FXML private Button btnAnnulerForm;
    @FXML private Label lblFormTitre;

    // Filtres
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbFiltreStatut;
    @FXML private DatePicker dpFiltreDate;
    @FXML private Button btnNouvelleReservation;

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final SalleDAO salleDAO = new SalleDAO();
    private final EquipementDAO equipementDAO = new EquipementDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    private List<Reservation> toutesReservations = new ArrayList<>();
    private Reservation reservationEnEdition = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupForm();
        setupFiltres();
        loadReservations();

        if (formPane != null) formPane.setVisible(false);
    }

    private void setupTable() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        colCode.setCellValueFactory(new PropertyValueFactory<>("codeReservation"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));

        colSalle.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getSalle() != null ? cd.getValue().getSalle().getNom() : "N/A"));

        colUser.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getUtilisateur() != null ?
                        cd.getValue().getUtilisateur().getNomComplet() : "N/A"));

        colDateDebut.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDateDebut() != null ?
                        cd.getValue().getDateDebut().format(fmt) : ""));

        colDateFin.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDateFin() != null ?
                        cd.getValue().getDateFin().format(fmt) : ""));

        colDuree.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDureeFormatee()));

        colParticipants.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getNombreParticipants())));

        // Colonne statut colorée
        colStatut.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStatut().getLibelle()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableView().getItems().isEmpty() || getIndex() >= getTableView().getItems().size()) {
                    setText(null); setStyle(""); return;
                }
                setText(item);
                Reservation r = getTableView().getItems().get(getIndex());
                if (r != null) {
                    setStyle("-fx-text-fill: " + r.getStatut().getCouleur() + "; -fx-font-weight: bold;");
                }
            }
        });

        // Double-clic pour éditer
        tableReservations.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditer(row.getItem());
                }
            });
            return row;
        });

        // ============================================================
        // ✅ MENU SIMPLIFIÉ : Seulement Confirmer et Annuler
        // ============================================================
        ContextMenu menu = new ContextMenu();
        MenuItem miVoir = new MenuItem("👁 Voir les détails");
        MenuItem miEditer = new MenuItem("✏ Modifier");
        MenuItem miConfirmer = new MenuItem("✅ Confirmer");
        MenuItem miAnnuler = new MenuItem("❌ Annuler");

        miVoir.setOnAction(e -> {
            Reservation r = tableReservations.getSelectionModel().getSelectedItem();
            if (r != null) handleEditer(r);
        });
        miEditer.setOnAction(e -> {
            Reservation r = tableReservations.getSelectionModel().getSelectedItem();
            if (r != null) handleEditer(r);
        });
        miConfirmer.setOnAction(e -> {
            Reservation r = tableReservations.getSelectionModel().getSelectedItem();
            if (r != null) handleConfirmer(r);
        });
        miAnnuler.setOnAction(e -> {
            Reservation r = tableReservations.getSelectionModel().getSelectedItem();
            if (r != null) handleAnnuler(r);
        });

        // ✅ Afficher/masquer selon le statut actuel ET le rôle
        menu.setOnShowing(e -> {
            Reservation r = tableReservations.getSelectionModel().getSelectedItem();
            if (r != null) {
                Reservation.StatutReservation statut = r.getStatut();
                boolean estAdminOuResponsable = SessionManager.getInstance().estAdmin()
                        || SessionManager.getInstance().estResponsable();

                miVoir.setVisible(true);
                miEditer.setVisible(true);

                // Seulement Confirmer et Annuler
                miConfirmer.setVisible(statut != Reservation.StatutReservation.CONFIRME && estAdminOuResponsable);
                miAnnuler.setVisible(statut != Reservation.StatutReservation.ANNULE && estAdminOuResponsable);
            }
        });

        menu.getItems().addAll(miVoir, miEditer, new SeparatorMenuItem(), miConfirmer, miAnnuler);
        tableReservations.setContextMenu(menu);
    }

    private void setupForm() {
        if (cbSalle == null) return;

        // Charger les salles
        cbSalle.getItems().setAll(salleDAO.findActives());

        // Heures disponibles (7h à 22h par tranches de 30min)
        List<String> heures = new ArrayList<>();
        for (int h = 7; h <= 22; h++) {
            heures.add(String.format("%02d:00", h));
            if (h < 22) heures.add(String.format("%02d:30", h));
        }
        cbHeureDebut.getItems().setAll(heures);
        cbHeureFin.getItems().setAll(heures);
        cbHeureDebut.setValue("09:00");
        cbHeureFin.setValue("10:00");

        // Dispositions
        cbDisposition.getItems().setAll(Salle.Disposition.values());
        cbDisposition.setValue(Salle.Disposition.CONFERENCE);

        // Participants
        SpinnerValueFactory<Integer> svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 10);
        spParticipants.setValueFactory(svf);

        // Date par défaut = demain
        dpDateDebut.setValue(LocalDate.now().plusDays(1));

        // Vérifier disponibilité quand salle/date/heure changent
        cbSalle.setOnAction(e -> verifierDisponibilite());
        dpDateDebut.setOnAction(e -> verifierDisponibilite());
        cbHeureDebut.setOnAction(e -> verifierDisponibilite());
        cbHeureFin.setOnAction(e -> verifierDisponibilite());

        // Charger équipements
        loadEquipements(null);
    }

    private void loadEquipements(Integer salleId) {
        if (lvEquipements == null) return;
        lvEquipements.getItems().clear();
        List<Equipement> equipements = equipementDAO.findActifs();
        for (Equipement eq : equipements) {
            if (eq.getEtat() == Equipement.EtatEquipement.BON) {
                CheckBox cb = new CheckBox(eq.getNom() + " (" + eq.getType().getLibelle() + ")");
                cb.setUserData(eq);
                lvEquipements.getItems().add(cb);
            }
        }
    }

    private void verifierDisponibilite() {
        if (lblDisponibilite == null || cbSalle.getValue() == null || dpDateDebut.getValue() == null) return;

        try {
            LocalDateTime debut = getDateTimeDebut();
            LocalDateTime fin = getDateTimeFin();
            if (debut == null || fin == null || !fin.isAfter(debut)) {
                lblDisponibilite.setText("⚠ L'heure de fin doit être après l'heure de début");
                lblDisponibilite.setStyle("-fx-text-fill: orange;");
                return;
            }

            Salle salle = cbSalle.getValue();
            boolean dispo = salleDAO.isDisponible(salle.getId(),
                    Timestamp.valueOf(debut), Timestamp.valueOf(fin),
                    reservationEnEdition != null ? reservationEnEdition.getId() : null);

            if (dispo) {
                lblDisponibilite.setText("✅ Salle disponible");
                lblDisponibilite.setStyle("-fx-text-fill: #4CAF50;");
            } else {
                lblDisponibilite.setText("❌ Salle non disponible pour ce créneau");
                lblDisponibilite.setStyle("-fx-text-fill: #F44336;");
            }
        } catch (Exception e) {
            lblDisponibilite.setText("");
        }
    }

    private LocalDateTime getDateTimeDebut() {
        if (dpDateDebut.getValue() == null || cbHeureDebut.getValue() == null) return null;
        String[] parts = cbHeureDebut.getValue().split(":");
        return LocalDateTime.of(dpDateDebut.getValue(), LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
    }

    private LocalDateTime getDateTimeFin() {
        if (dpDateDebut.getValue() == null || cbHeureFin.getValue() == null) return null;
        String[] parts = cbHeureFin.getValue().split(":");
        return LocalDateTime.of(dpDateDebut.getValue(), LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
    }

    /**
     * ✅ Filtres avec seulement 2 statuts : Confirmé et Annulé
     */
    private void setupFiltres() {
        if (cbFiltreStatut == null) return;

        // Seulement "Tous" + les 2 statuts utilisés
        cbFiltreStatut.getItems().addAll("Tous", "Confirmé", "Annulé");
        cbFiltreStatut.setValue("Tous");

        cbFiltreStatut.setOnAction(e -> appliquerFiltres());
        if (tfRecherche != null) tfRecherche.setOnKeyReleased(e -> appliquerFiltres());
        if (dpFiltreDate != null) dpFiltreDate.setOnAction(e -> appliquerFiltres());
    }

    /**
     * ✅ Filtre par statut avec comparaison exacte
     */
    private void appliquerFiltres() {
        List<Reservation> filtrees = new ArrayList<>(toutesReservations);

        // Filtre par recherche texte
        String recherche = tfRecherche != null ? tfRecherche.getText().toLowerCase().trim() : "";
        if (!recherche.isEmpty()) {
            filtrees.removeIf(r -> {
                boolean matchTitre = r.getTitre() != null && r.getTitre().toLowerCase().contains(recherche);
                boolean matchCode = r.getCodeReservation() != null && r.getCodeReservation().toLowerCase().contains(recherche);
                boolean matchSalle = r.getSalle() != null && r.getSalle().getNom().toLowerCase().contains(recherche);
                boolean matchUser = r.getUtilisateur() != null && r.getUtilisateur().getNomComplet().toLowerCase().contains(recherche);
                return !matchTitre && !matchCode && !matchSalle && !matchUser;
            });
        }

        // Filtre par statut
        String filtreStatut = cbFiltreStatut != null ? cbFiltreStatut.getValue() : "Tous";
        if (filtreStatut != null && !"Tous".equals(filtreStatut)) {
            filtrees.removeIf(r -> {
                String statutReservation = r.getStatut() != null ? r.getStatut().getLibelle() : "";
                return !statutReservation.equalsIgnoreCase(filtreStatut);
            });
        }

        // Filtre par date
        LocalDate filtreDate = dpFiltreDate != null ? dpFiltreDate.getValue() : null;
        if (filtreDate != null) {
            filtrees.removeIf(r -> r.getDateDebut() == null || !r.getDateDebut().toLocalDate().equals(filtreDate));
        }

        tableReservations.getItems().setAll(filtrees);
    }

    /**
     * ✅ Admin et Responsable voient TOUTES les réservations
     *    Utilisateur normal ne voit que SES réservations
     */
    private void loadReservations() {
        SessionManager session = SessionManager.getInstance();

        if (session.estAdmin() || session.estResponsable()) {
            toutesReservations = reservationDAO.findAll();
        } else {
            toutesReservations = reservationDAO.findByUtilisateur(session.getUserId());
        }

        tableReservations.getItems().setAll(toutesReservations);
    }

    @FXML
    private void handleNouvelleReservation() {
        reservationEnEdition = null;
        clearForm();
        if (lblFormTitre != null) lblFormTitre.setText("Nouvelle Réservation");
        if (formPane != null) formPane.setVisible(true);
    }

    private void handleEditer(Reservation r) {
        reservationEnEdition = r;
        if (lblFormTitre != null) lblFormTitre.setText("Modifier Réservation: " + r.getCodeReservation());

        if (tfTitre != null) tfTitre.setText(r.getTitre());
        if (taDescription != null) taDescription.setText(r.getDescription());
        if (taCommentaires != null) taCommentaires.setText(r.getCommentaires());

        if (r.getSalle() != null && cbSalle != null) {
            cbSalle.getItems().stream()
                    .filter(s -> s.getId() == r.getSalle().getId())
                    .findFirst().ifPresent(cbSalle::setValue);
        }

        if (r.getDateDebut() != null && dpDateDebut != null) {
            dpDateDebut.setValue(r.getDateDebut().toLocalDate());
            cbHeureDebut.setValue(r.getDateDebut().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (r.getDateFin() != null && cbHeureFin != null) {
            cbHeureFin.setValue(r.getDateFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        if (spParticipants != null) spParticipants.getValueFactory().setValue(r.getNombreParticipants());
        if (cbDisposition != null && r.getDisposition() != null) cbDisposition.setValue(r.getDisposition());

        if (formPane != null) formPane.setVisible(true);
        verifierDisponibilite();
    }

    private void handleAnnuler(Reservation r) {
        if (SessionManager.showConfirm("Annuler la réservation",
                "Voulez-vous annuler la réservation \"" + r.getTitre() + "\" ?")) {
            if (reservationDAO.annuler(r.getId())) {
                Notification notif = new Notification(r.getUtilisateurId(), r.getId(),
                        Notification.TypeNotification.ANNULATION,
                        "Réservation Annulée",
                        "Votre réservation \"" + r.getTitre() + "\" a été annulée.");
                notificationDAO.save(notif);
                SessionManager.showInfo("Succès", "Réservation annulée avec succès.");
                loadReservations();
            } else {
                SessionManager.showError("Erreur", "Impossible d'annuler la réservation.");
            }
        }
    }

    private void handleConfirmer(Reservation r) {
        if (reservationDAO.updateStatut(r.getId(), Reservation.StatutReservation.CONFIRME)) {
            Notification notif = new Notification(r.getUtilisateurId(), r.getId(),
                    Notification.TypeNotification.CONFIRMATION,
                    "Réservation Confirmée",
                    "Votre réservation \"" + r.getTitre() + "\" a été confirmée.");
            notificationDAO.save(notif);
            SessionManager.showInfo("Succès", "Réservation confirmée.");
            loadReservations();
        }
    }

    @FXML
    private void handleSauvegarder() {
        if (!validerFormulaire()) return;

        LocalDateTime debut = getDateTimeDebut();
        LocalDateTime fin = getDateTimeFin();
        Salle salle = cbSalle.getValue();

        boolean dispo = salleDAO.isDisponible(salle.getId(),
                Timestamp.valueOf(debut), Timestamp.valueOf(fin),
                reservationEnEdition != null ? reservationEnEdition.getId() : null);

        if (!dispo) {
            SessionManager.showError("Salle non disponible", "La salle est déjà réservée pour ce créneau.");
            return;
        }

        Reservation r = reservationEnEdition != null ? reservationEnEdition : new Reservation();
        r.setTitre(tfTitre.getText().trim());
        r.setDescription(taDescription != null ? taDescription.getText() : "");
        r.setSalleId(salle.getId());
        r.setDateDebut(debut);
        r.setDateFin(fin);
        r.setNombreParticipants(spParticipants.getValue());
        r.setDisposition(cbDisposition.getValue());
        r.setCommentaires(taCommentaires != null ? taCommentaires.getText() : "");
        r.setUtilisateurId(SessionManager.getInstance().getUserId());
        r.setStatut(Reservation.StatutReservation.CONFIRME);

        List<Equipement> equipementsChoisis = new ArrayList<>();
        if (lvEquipements != null) {
            for (CheckBox cb : lvEquipements.getItems()) {
                if (cb.isSelected()) {
                    Equipement eq = (Equipement) cb.getUserData();
                    eq.setQuantiteReservee(1);
                    equipementsChoisis.add(eq);
                }
            }
        }
        r.setEquipements(equipementsChoisis);

        boolean succes;
        if (reservationEnEdition != null) {
            succes = reservationDAO.update(r);
        } else {
            succes = reservationDAO.save(r);
            if (succes) {
                Notification notif = new Notification(r.getUtilisateurId(), r.getId(),
                        Notification.TypeNotification.CONFIRMATION,
                        "Réservation Créée",
                        "Votre réservation \"" + r.getTitre() + "\" (" + r.getCodeReservation() + ") a été créée avec succès pour le " +
                                debut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " de " +
                                cbHeureDebut.getValue() + " à " + cbHeureFin.getValue() + ".");
                notificationDAO.save(notif);
            }
        }

        if (succes) {
            SessionManager.showInfo("Succès", reservationEnEdition != null ?
                    "Réservation modifiée avec succès." : "Réservation créée avec succès!");
            if (formPane != null) formPane.setVisible(false);
            loadReservations();
        } else {
            SessionManager.showError("Erreur", "Impossible de sauvegarder la réservation.");
        }
    }

    private boolean validerFormulaire() {
        if (tfTitre == null || tfTitre.getText().trim().isEmpty()) {
            SessionManager.showError("Validation", "Le titre est obligatoire.");
            return false;
        }
        if (cbSalle == null || cbSalle.getValue() == null) {
            SessionManager.showError("Validation", "Veuillez sélectionner une salle.");
            return false;
        }
        if (dpDateDebut == null || dpDateDebut.getValue() == null) {
            SessionManager.showError("Validation", "Veuillez sélectionner une date.");
            return false;
        }
        LocalDateTime debut = getDateTimeDebut();
        LocalDateTime fin = getDateTimeFin();
        if (debut == null || fin == null || !fin.isAfter(debut)) {
            SessionManager.showError("Validation", "L'heure de fin doit être après l'heure de début.");
            return false;
        }
        if (debut.isBefore(LocalDateTime.now()) && reservationEnEdition == null) {
            SessionManager.showError("Validation", "Impossible de réserver dans le passé.");
            return false;
        }
        Salle salle = cbSalle.getValue();
        if (spParticipants != null && spParticipants.getValue() > salle.getCapacite()) {
            SessionManager.showWarning("Capacité dépassée",
                    "Le nombre de participants (" + spParticipants.getValue() +
                            ") dépasse la capacité de la salle (" + salle.getCapacite() + ").\nVoulez-vous continuer ?");
        }
        return true;
    }

    private void clearForm() {
        if (tfTitre != null) tfTitre.clear();
        if (taDescription != null) taDescription.clear();
        if (taCommentaires != null) taCommentaires.clear();
        if (cbSalle != null) cbSalle.setValue(null);
        if (dpDateDebut != null) dpDateDebut.setValue(LocalDate.now().plusDays(1));
        if (cbHeureDebut != null) cbHeureDebut.setValue("09:00");
        if (cbHeureFin != null) cbHeureFin.setValue("10:00");
        if (spParticipants != null) spParticipants.getValueFactory().setValue(10);
        if (cbDisposition != null) cbDisposition.setValue(Salle.Disposition.CONFERENCE);
        if (lblDisponibilite != null) lblDisponibilite.setText("");
        if (lvEquipements != null) lvEquipements.getItems().forEach(cb -> cb.setSelected(false));
    }

    @FXML
    private void handleAnnulerForm() {
        if (formPane != null) formPane.setVisible(false);
        reservationEnEdition = null;
    }

    @FXML
    private void handleRafraichir() {
        loadReservations();
    }

    @FXML
    private void handleEffacerFiltres() {
        if (tfRecherche != null) tfRecherche.clear();
        if (cbFiltreStatut != null) cbFiltreStatut.setValue("Tous");
        if (dpFiltreDate != null) dpFiltreDate.setValue(null);
        appliquerFiltres();
    }
}