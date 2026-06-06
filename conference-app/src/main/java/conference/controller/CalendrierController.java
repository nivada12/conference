package conference.controller;

import conference.dao.impl.ReservationDAO;
import conference.model.Reservation;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class CalendrierController implements Initializable {

    @FXML private Label lblMoisAnnee;
    @FXML private GridPane grilleCal;
    @FXML private VBox listeResJour;
    @FXML private Label lblDateSelectionnee;

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private YearMonth moisAffiche;
    private final DateTimeFormatter fmtHeure = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        moisAffiche = YearMonth.now();
        afficherCalendrier();
    }

    private void afficherCalendrier() {
        if (lblMoisAnnee != null) {
            String moisNom = moisAffiche.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
            lblMoisAnnee.setText(moisNom.substring(0,1).toUpperCase() + moisNom.substring(1) + " " + moisAffiche.getYear());
        }
        if (grilleCal == null) return;
        grilleCal.getChildren().clear();

        // En-têtes
        String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(jours[i]);
            lbl.setMaxWidth(Double.MAX_VALUE);
            boolean weekend = i >= 5;
            lbl.setStyle("-fx-alignment: center; -fx-font-weight: bold; -fx-font-size: 12px;" +
                "-fx-text-fill: " + (weekend ? "#EF5350" : "#1A237E") + ";" +
                "-fx-padding: 8; -fx-background-color: #F0F4FF;");
            GridPane.setHgrow(lbl, Priority.ALWAYS);
            grilleCal.add(lbl, i, 0);
        }

        // Réservations du mois
        List<Reservation> reservations = reservationDAO.findByMois(moisAffiche.getYear(), moisAffiche.getMonthValue());
        Map<LocalDate, List<Reservation>> parJour = new HashMap<>();
        for (Reservation r : reservations) {
            if (r.getDateDebut() != null) {
                parJour.computeIfAbsent(r.getDateDebut().toLocalDate(), k -> new ArrayList<>()).add(r);
            }
        }

        LocalDate premierJour = moisAffiche.atDay(1);
        int decalage = premierJour.getDayOfWeek().getValue() - 1;
        int nbJours = moisAffiche.lengthOfMonth();
        LocalDate today = LocalDate.now();

        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = moisAffiche.atDay(jour);
            int col = (decalage + jour - 1) % 7;
            int row = (decalage + jour - 1) / 7 + 1;

            VBox cellule = new VBox(2);
            cellule.setPadding(new Insets(4));
            cellule.setMinHeight(70);
            cellule.setMaxWidth(Double.MAX_VALUE);

            boolean estAujourdhui = date.equals(today);
            boolean estWeekend = col >= 5;
            List<Reservation> resJour = parJour.getOrDefault(date, new ArrayList<>());

            String bgColor = estAujourdhui ? "#1565C0" : (estWeekend ? "#FFF3F3" : "white");
            String borderColor = estAujourdhui ? "#0D47A1" : "#E0E0E0";
            cellule.setStyle("-fx-background-color: " + bgColor + ";" +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 0.5;" +
                "-fx-background-radius: 4; -fx-cursor: hand;");

            Label lblJour = new Label(String.valueOf(jour));
            lblJour.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;" +
                "-fx-text-fill: " + (estAujourdhui ? "white" : (estWeekend ? "#EF5350" : "#212121")) + ";");
            cellule.getChildren().add(lblJour);

            // Afficher les réservations (max 2)
            int max = Math.min(resJour.size(), 2);
            for (int k = 0; k < max; k++) {
                Reservation r = resJour.get(k);
                Label lblRes = new Label("● " + r.getTitre());
                lblRes.setStyle("-fx-font-size: 9px; -fx-text-fill: " +
                    (estAujourdhui ? "rgba(255,255,255,0.9)" : r.getStatut().getCouleur()) + ";" +
                    "-fx-background-color: " + (estAujourdhui ? "rgba(255,255,255,0.2)" : "#F0F4FF") + ";" +
                    "-fx-background-radius: 3; -fx-padding: 1 4;");
                lblRes.setMaxWidth(Double.MAX_VALUE);
                lblRes.setEllipsisString("...");
                cellule.getChildren().add(lblRes);
            }
            if (resJour.size() > 2) {
                Label plus = new Label("+" + (resJour.size() - 2) + " autres");
                plus.setStyle("-fx-font-size: 9px; -fx-text-fill: " + (estAujourdhui ? "white" : "#1976D2") + ";");
                cellule.getChildren().add(plus);
            }

            // Clic pour voir détails
            final LocalDate dateFinal = date;
            final List<Reservation> resFinal = resJour;
            cellule.setOnMouseClicked(e -> afficherDetailsJour(dateFinal, resFinal));
            cellule.setOnMouseEntered(e -> {
                if (!dateFinal.equals(today))
                    cellule.setStyle(cellule.getStyle().replace("white", "#F0F4FF").replace("#FFF3F3", "#FFE0E0"));
            });
            cellule.setOnMouseExited(e -> {
                if (!dateFinal.equals(today)) {
                    String bg = estWeekend ? "#FFF3F3" : "white";
                    cellule.setStyle("-fx-background-color: " + bg + ";" +
                        "-fx-border-color: #E0E0E0; -fx-border-width: 0.5;" +
                        "-fx-background-radius: 4; -fx-cursor: hand;");
                }
            });

            GridPane.setHgrow(cellule, Priority.ALWAYS);
            grilleCal.add(cellule, col, row);
        }
    }

    private void afficherDetailsJour(LocalDate date, List<Reservation> reservations) {
        if (lblDateSelectionnee != null) {
            lblDateSelectionnee.setText(date.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH)));
        }
        if (listeResJour == null) return;
        listeResJour.getChildren().clear();

        if (reservations.isEmpty()) {
            Label lbl = new Label("Aucune réservation ce jour");
            lbl.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 13px; -fx-padding: 10;");
            listeResJour.getChildren().add(lbl);
            return;
        }

        for (Reservation r : reservations) {
            VBox card = new VBox(4);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 8;" +
                "-fx-border-left-color: " + r.getStatut().getCouleur() + ";" +
                "-fx-border-left-width: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);");

            Label titre = new Label(r.getTitre());
            titre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1A237E;");

            String heures = (r.getDateDebut() != null ? r.getDateDebut().format(fmtHeure) : "") +
                " → " + (r.getDateFin() != null ? r.getDateFin().format(fmtHeure) : "");
            Label heure = new Label("⏰ " + heures + "  (" + r.getDureeFormatee() + ")");
            heure.setStyle("-fx-font-size: 11px; -fx-text-fill: #546E7A;");

            Label salle = new Label("🚪 " + (r.getSalle() != null ? r.getSalle().getNom() : "N/A"));
            salle.setStyle("-fx-font-size: 11px; -fx-text-fill: #546E7A;");

            Label participants = new Label("👥 " + r.getNombreParticipants() + " participants");
            participants.setStyle("-fx-font-size: 11px; -fx-text-fill: #546E7A;");

            Label statut = new Label(r.getStatut().getLibelle());
            statut.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " +
                r.getStatut().getCouleur() + "; -fx-background-color: " +
                r.getStatut().getCouleur() + "22; -fx-background-radius: 10; -fx-padding: 2 8;");

            card.getChildren().addAll(titre, heure, salle, participants, statut);
            VBox.setMargin(card, new Insets(0, 0, 6, 0));
            listeResJour.getChildren().add(card);
        }
    }

    @FXML private void handleMoisPrecedent() {
        moisAffiche = moisAffiche.minusMonths(1);
        afficherCalendrier();
    }

    @FXML private void handleMoisSuivant() {
        moisAffiche = moisAffiche.plusMonths(1);
        afficherCalendrier();
    }

    @FXML private void handleAujourdhui() {
        moisAffiche = YearMonth.now();
        afficherCalendrier();
    }
}
