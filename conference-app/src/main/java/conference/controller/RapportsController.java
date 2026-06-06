package conference.controller;

import conference.config.DatabaseConfig;
import conference.util.SessionManager;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.ResourceBundle;

public class RapportsController implements Initializable {

    // === Labels Statistiques ===
    @FXML private Label lblTotalReservations;
    @FXML private Label lblTauxOccupation;
    @FXML private Label lblDureeMoyenne;
    @FXML private Label lblSallePlusUtilisee;
    @FXML private Label lblPeriodeAffichee;
    @FXML private Label lblTitreGraphique;
    @FXML private Label lblTitrePieChart;

    // === Filtres ===
    @FXML private DatePicker dateReference;
    @FXML private ComboBox<String> cbTypeRapport;
    @FXML private ComboBox<String> cbSalle;

    // === Graphiques ===
    @FXML private BarChart<String, Number> chartReservations;
    @FXML private CategoryAxis xAxis;
    @FXML private PieChart chartRepartitionSalles;

    // Données actuelles
    private String currentPeriodeType = "Mensuel";
    private LocalDate currentDateDebut;
    private LocalDate currentDateFin;

    // Labels en français pour les graphiques
    private final String[] joursSemaine = {"Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"};
    private final String[] moisAnnee = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dateReference.setValue(LocalDate.now());

        cbTypeRapport.setItems(FXCollections.observableArrayList(
                "Quotidien", "Hebdomadaire", "Mensuel", "Annuel"
        ));
        cbTypeRapport.setValue("Mensuel");

        chargerSalles();

        // Listeners corrigés pour synchronisation automatique
        cbSalle.valueProperty().addListener((obs, oldVal, newVal) -> {
            handleGenerer();
        });

        cbTypeRapport.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleTypeChange();
            }
        });

        dateReference.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleGenerer();
            }
        });

        calculerPeriode();
        chargerTout();
    }

    @FXML
    private void handleDateChange() {
        handleGenerer();
    }

    @FXML
    private void handleTypeChange() {
        currentPeriodeType = cbTypeRapport.getValue();
        calculerPeriode();
        chargerTout();
    }

    @FXML
    private void handleGenerer() {
        calculerPeriode();
        chargerTout();
    }

    private void calculerPeriode() {
        LocalDate ref = dateReference.getValue();
        if (ref == null) ref = LocalDate.now();

        switch (currentPeriodeType) {
            case "Quotidien":
                currentDateDebut = ref;
                currentDateFin = ref;
                lblPeriodeAffichee.setText("Journée du " + ref.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                lblTitreGraphique.setText("Réservations par heure");
                xAxis.setLabel("Heure de la journée");
                break;

            case "Hebdomadaire":
                WeekFields wf = WeekFields.of(Locale.FRANCE);
                currentDateDebut = ref.with(wf.dayOfWeek(), 1L);
                currentDateFin = ref.with(wf.dayOfWeek(), 7L);
                lblPeriodeAffichee.setText("Semaine du " + currentDateDebut.format(DateTimeFormatter.ofPattern("dd/MM")) +
                        " au " + currentDateFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                lblTitreGraphique.setText("Réservations par jour de la semaine");
                xAxis.setLabel("Jour de la semaine");
                break;

            case "Annuel":
                currentDateDebut = LocalDate.of(ref.getYear(), 1, 1);
                currentDateFin = LocalDate.of(ref.getYear(), 12, 31);
                lblPeriodeAffichee.setText("Année " + ref.getYear());
                lblTitreGraphique.setText("Réservations par mois");
                xAxis.setLabel("Mois");
                break;

            case "Mensuel":
            default:
                currentDateDebut = ref.withDayOfMonth(1);
                currentDateFin = ref.withDayOfMonth(ref.lengthOfMonth());
                lblPeriodeAffichee.setText("Mois de " + ref.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
                lblTitreGraphique.setText("Réservations par jour");
                xAxis.setLabel("Jour du mois");
                break;
        }
    }

    private void chargerTout() {
        chargerStatistiques();
        chargerGraphiqueBarres();
        chargerGraphiqueCamembert();
    }

    private void chargerSalles() {
        ObservableList<String> salles = FXCollections.observableArrayList();
        salles.add("Toutes les salles");

        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            String sql = "SELECT nom FROM salles WHERE actif = TRUE ORDER BY nom";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                salles.add(rs.getString("nom"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        cbSalle.setItems(salles);
        cbSalle.setValue("Toutes les salles");
    }

    private void chargerStatistiques() {
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {

            String salleWhereClause = buildSalleWhereClause();
            String selectedSalle = cbSalle.getValue();

            // Total des réservations
            String sql = """
                SELECT COUNT(*) AS total 
                FROM reservations r
                JOIN salles s ON r.salle_id = s.id
                WHERE r.statut NOT IN ('ANNULE')
                AND DATE(r.date_debut) BETWEEN ? AND ?
                %s
            """.formatted(salleWhereClause);

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentDateDebut.toString());
            ps.setString(2, currentDateFin.toString());
            applySalleFilter(ps, selectedSalle, 3);

            ResultSet rs = ps.executeQuery();
            int total = 0;
            if (rs.next()) {
                total = rs.getInt("total");
                lblTotalReservations.setText(String.valueOf(total));
            }

            // Durée moyenne
            sql = """
                SELECT COALESCE(AVG(TIMESTAMPDIFF(HOUR, date_debut, date_fin)), 0) AS moyenne 
                FROM reservations r
                JOIN salles s ON r.salle_id = s.id
                WHERE r.statut NOT IN ('ANNULE')
                AND DATE(r.date_debut) BETWEEN ? AND ?
                %s
            """.formatted(salleWhereClause);

            ps = conn.prepareStatement(sql);
            ps.setString(1, currentDateDebut.toString());
            ps.setString(2, currentDateFin.toString());
            applySalleFilter(ps, selectedSalle, 3);

            rs = ps.executeQuery();
            if (rs.next()) {
                int moyenne = (int) Math.round(rs.getDouble("moyenne"));
                lblDureeMoyenne.setText(moyenne + " h");
            } else {
                lblDureeMoyenne.setText("0 h");
            }

            // Salle la plus utilisée
            if (selectedSalle == null || selectedSalle.equals("Toutes les salles")) {
                sql = """
                    SELECT s.nom, COUNT(*) AS total
                    FROM reservations r
                    JOIN salles s ON r.salle_id = s.id
                    WHERE r.statut NOT IN ('ANNULE')
                    AND DATE(r.date_debut) BETWEEN ? AND ?
                    GROUP BY s.id, s.nom
                    ORDER BY total DESC
                    LIMIT 1
                """;

                ps = conn.prepareStatement(sql);
                ps.setString(1, currentDateDebut.toString());
                ps.setString(2, currentDateFin.toString());
                rs = ps.executeQuery();

                if (rs.next()) {
                    lblSallePlusUtilisee.setText(rs.getString("nom"));
                } else {
                    lblSallePlusUtilisee.setText("—");
                }
            } else {
                lblSallePlusUtilisee.setText(selectedSalle);
            }

            // Taux d'occupation
            sql = "SELECT COUNT(*) AS nb_salles FROM salles WHERE actif = TRUE";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            if (rs.next()) {
                int nbSalles = rs.getInt("nb_salles");
                long joursPeriode = currentDateDebut.until(currentDateFin).getDays() + 1;
                int creneauxPossibles = nbSalles * (int) joursPeriode * 8;

                double taux = creneauxPossibles > 0 ? (total * 100.0 / creneauxPossibles) : 0;
                lblTauxOccupation.setText(String.format("%.1f %%", Math.min(taux, 100)));
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblTotalReservations.setText("0");
            lblDureeMoyenne.setText("0 h");
            lblSallePlusUtilisee.setText("—");
            lblTauxOccupation.setText("0 %");
        }
    }

    private void chargerGraphiqueBarres() {
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {

            Platform.runLater(() -> {
                chartReservations.getData().clear();
                chartReservations.layout();
            });

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Réservations");

            String sql;
            String salleWhereClause = buildSalleWhereClause();
            String selectedSalle = cbSalle.getValue();
            PreparedStatement ps;
            ResultSet rs;

            switch (currentPeriodeType) {
                case "Quotidien":
                    sql = """
                        SELECT HOUR(date_debut) AS heure, COUNT(*) AS total
                        FROM reservations r
                        JOIN salles s ON r.salle_id = s.id
                        WHERE r.statut NOT IN ('ANNULE')
                        AND DATE(r.date_debut) = ?
                        %s
                        GROUP BY HOUR(date_debut)
                        ORDER BY heure
                    """.formatted(salleWhereClause);

                    ps = conn.prepareStatement(sql);
                    ps.setString(1, currentDateDebut.toString());
                    applySalleFilter(ps, selectedSalle, 2);

                    rs = ps.executeQuery();
                    while (rs.next()) {
                        int heure = rs.getInt("heure");
                        series.getData().add(new XYChart.Data<>(
                                heure + "h",
                                rs.getInt("total")
                        ));
                    }
                    break;

                case "Hebdomadaire":
                    sql = """
                        SELECT DAYOFWEEK(date_debut) AS jour, COUNT(*) AS total
                        FROM reservations r
                        JOIN salles s ON r.salle_id = s.id
                        WHERE r.statut NOT IN ('ANNULE')
                        AND DATE(r.date_debut) BETWEEN ? AND ?
                        %s
                        GROUP BY DAYOFWEEK(date_debut)
                        ORDER BY jour
                    """.formatted(salleWhereClause);

                    ps = conn.prepareStatement(sql);
                    ps.setString(1, currentDateDebut.toString());
                    ps.setString(2, currentDateFin.toString());
                    applySalleFilter(ps, selectedSalle, 3);

                    rs = ps.executeQuery();
                    while (rs.next()) {
                        int jourIndex = rs.getInt("jour") - 1;
                        if (jourIndex >= 0 && jourIndex < joursSemaine.length) {
                            series.getData().add(new XYChart.Data<>(
                                    joursSemaine[jourIndex],
                                    rs.getInt("total")
                            ));
                        }
                    }
                    break;

                case "Annuel":
                    sql = """
                        SELECT MONTH(date_debut) AS mois, COUNT(*) AS total
                        FROM reservations r
                        JOIN salles s ON r.salle_id = s.id
                        WHERE r.statut NOT IN ('ANNULE')
                        AND YEAR(r.date_debut) = ?
                        %s
                        GROUP BY MONTH(date_debut)
                        ORDER BY mois
                    """.formatted(salleWhereClause);

                    ps = conn.prepareStatement(sql);
                    ps.setInt(1, currentDateDebut.getYear());
                    applySalleFilter(ps, selectedSalle, 2);

                    rs = ps.executeQuery();
                    while (rs.next()) {
                        int moisIndex = rs.getInt("mois") - 1;
                        if (moisIndex >= 0 && moisIndex < moisAnnee.length) {
                            series.getData().add(new XYChart.Data<>(
                                    moisAnnee[moisIndex],
                                    rs.getInt("total")
                            ));
                        }
                    }
                    break;

                case "Mensuel":
                default:
                    sql = """
                        SELECT DAY(date_debut) AS jour, COUNT(*) AS total
                        FROM reservations r
                        JOIN salles s ON r.salle_id = s.id
                        WHERE r.statut NOT IN ('ANNULE')
                        AND DATE(r.date_debut) BETWEEN ? AND ?
                        %s
                        GROUP BY DAY(date_debut)
                        ORDER BY jour
                    """.formatted(salleWhereClause);

                    ps = conn.prepareStatement(sql);
                    ps.setString(1, currentDateDebut.toString());
                    ps.setString(2, currentDateFin.toString());
                    applySalleFilter(ps, selectedSalle, 3);

                    rs = ps.executeQuery();
                    while (rs.next()) {
                        series.getData().add(new XYChart.Data<>(
                                String.valueOf(rs.getInt("jour")),
                                rs.getInt("total")
                        ));
                    }
                    break;
            }

            XYChart.Series<String, Number> finalSeries = series;
            Platform.runLater(() -> {
                chartReservations.getData().add(finalSeries);
                chartReservations.layout();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void chargerGraphiqueCamembert() {
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            String selectedSalle = cbSalle.getValue();

            Platform.runLater(() -> {
                chartRepartitionSalles.getData().clear();
                chartRepartitionSalles.layout();
            });

            if (selectedSalle != null && !selectedSalle.equals("Toutes les salles")) {
                lblTitrePieChart.setText("Répartition des réservations - " + selectedSalle);

                String sql;
                PreparedStatement ps;

                switch (currentPeriodeType) {
                    case "Quotidien":
                        // 🔧 CORRECTION : Sélectionner uniquement HOUR(date_debut) et COUNT,
                        // le label est construit en Java
                        sql = """
                            SELECT HOUR(date_debut) AS heure, COUNT(*) AS total
                            FROM reservations r
                            JOIN salles s ON r.salle_id = s.id
                            WHERE r.statut NOT IN ('ANNULE')
                            AND s.nom = ?
                            AND DATE(r.date_debut) = ?
                            GROUP BY HOUR(date_debut)
                            ORDER BY HOUR(date_debut)
                        """;
                        ps = conn.prepareStatement(sql);
                        ps.setString(1, selectedSalle);
                        ps.setString(2, currentDateDebut.toString());

                        ResultSet rsQuotidien = ps.executeQuery();
                        while (rsQuotidien.next()) {
                            int heure = rsQuotidien.getInt("heure");
                            pieData.add(new PieChart.Data(
                                    heure + "h",
                                    rsQuotidien.getInt("total")
                            ));
                        }
                        break;

                    case "Hebdomadaire":
                        // 🔧 CORRECTION : Sélectionner uniquement DAYOFWEEK et COUNT
                        sql = """
                            SELECT DAYOFWEEK(date_debut) AS jour, COUNT(*) AS total
                            FROM reservations r
                            JOIN salles s ON r.salle_id = s.id
                            WHERE r.statut NOT IN ('ANNULE')
                            AND s.nom = ?
                            AND DATE(r.date_debut) BETWEEN ? AND ?
                            GROUP BY DAYOFWEEK(date_debut)
                            ORDER BY DAYOFWEEK(date_debut)
                        """;
                        ps = conn.prepareStatement(sql);
                        ps.setString(1, selectedSalle);
                        ps.setString(2, currentDateDebut.toString());
                        ps.setString(3, currentDateFin.toString());

                        ResultSet rsHebdo = ps.executeQuery();
                        while (rsHebdo.next()) {
                            int jourIndex = rsHebdo.getInt("jour") - 1;
                            if (jourIndex >= 0 && jourIndex < joursSemaine.length) {
                                pieData.add(new PieChart.Data(
                                        joursSemaine[jourIndex],
                                        rsHebdo.getInt("total")
                                ));
                            }
                        }
                        break;

                    case "Annuel":
                        // 🔧 CORRECTION : Sélectionner uniquement MONTH et COUNT
                        sql = """
                            SELECT MONTH(date_debut) AS mois, COUNT(*) AS total
                            FROM reservations r
                            JOIN salles s ON r.salle_id = s.id
                            WHERE r.statut NOT IN ('ANNULE')
                            AND s.nom = ?
                            AND YEAR(r.date_debut) = ?
                            GROUP BY MONTH(date_debut)
                            ORDER BY MONTH(date_debut)
                        """;
                        ps = conn.prepareStatement(sql);
                        ps.setString(1, selectedSalle);
                        ps.setInt(2, currentDateDebut.getYear());

                        ResultSet rsAnnuel = ps.executeQuery();
                        while (rsAnnuel.next()) {
                            int moisIndex = rsAnnuel.getInt("mois") - 1;
                            if (moisIndex >= 0 && moisIndex < moisAnnee.length) {
                                pieData.add(new PieChart.Data(
                                        moisAnnee[moisIndex],
                                        rsAnnuel.getInt("total")
                                ));
                            }
                        }
                        break;

                    default: // Mensuel
                        // 🔧 CORRECTION : Sélectionner uniquement DATE(date_debut) et COUNT
                        sql = """
                            SELECT DATE(date_debut) AS date_jour, COUNT(*) AS total
                            FROM reservations r
                            JOIN salles s ON r.salle_id = s.id
                            WHERE r.statut NOT IN ('ANNULE')
                            AND s.nom = ?
                            AND DATE(r.date_debut) BETWEEN ? AND ?
                            GROUP BY DATE(date_debut)
                            ORDER BY DATE(date_debut)
                            LIMIT 15
                        """;
                        ps = conn.prepareStatement(sql);
                        ps.setString(1, selectedSalle);
                        ps.setString(2, currentDateDebut.toString());
                        ps.setString(3, currentDateFin.toString());

                        ResultSet rsMensuel = ps.executeQuery();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
                        while (rsMensuel.next()) {
                            String dateStr = rsMensuel.getString("date_jour");
                            LocalDate date = LocalDate.parse(dateStr);
                            pieData.add(new PieChart.Data(
                                    date.format(formatter),
                                    rsMensuel.getInt("total")
                            ));
                        }
                        break;
                }

                if (pieData.isEmpty()) {
                    pieData.add(new PieChart.Data("Aucune réservation", 1));
                }

            } else {
                lblTitrePieChart.setText("Répartition par salle");

                String sql = """
                    SELECT s.nom, COUNT(*) AS total
                    FROM reservations r
                    JOIN salles s ON r.salle_id = s.id
                    WHERE r.statut NOT IN ('ANNULE')
                    AND DATE(r.date_debut) BETWEEN ? AND ?
                    GROUP BY s.nom
                    ORDER BY total DESC
                """;

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, currentDateDebut.toString());
                ps.setString(2, currentDateFin.toString());
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    pieData.add(new PieChart.Data(
                            rs.getString("nom"),
                            rs.getInt("total")
                    ));
                }

                if (pieData.isEmpty()) {
                    pieData.add(new PieChart.Data("Aucune réservation", 1));
                }
            }

            Platform.runLater(() -> {
                chartRepartitionSalles.setData(pieData);
                chartRepartitionSalles.layout();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildSalleWhereClause() {
        String selectedSalle = cbSalle.getValue();
        if (selectedSalle == null || selectedSalle.equals("Toutes les salles")) {
            return "";
        }
        return " AND s.nom = ?";
    }

    private void applySalleFilter(PreparedStatement ps, String selectedSalle, int startIndex) throws Exception {
        if (selectedSalle != null && !selectedSalle.equals("Toutes les salles")) {
            ps.setString(startIndex, selectedSalle);
        }
    }

    // ===== EXPORT PDF =====

    @FXML
    private void handleExportPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );
        fileChooser.setInitialFileName("rapport_" + currentPeriodeType.toLowerCase() + "_" +
                currentDateDebut.format(DateTimeFormatter.ISO_DATE) + ".pdf");

        File file = fileChooser.showSaveDialog(chartRepartitionSalles.getScene().getWindow());

        if (file != null) {
            try {
                exportToPDF(file.getAbsolutePath());
                SessionManager.showInfo("Export PDF", "Rapport PDF exporté avec succès !");
            } catch (Exception e) {
                SessionManager.showError("Erreur Export PDF", e.getMessage());
            }
        }
    }

    private void exportToPDF(String filePath) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new java.io.FileOutputStream(filePath));
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(26, 35, 126));
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);
        Font statsFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(255, 111, 0));

        document.add(new Paragraph("Rapport de Réservations", titleFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Généré le: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont));
        document.add(new Paragraph(lblPeriodeAffichee.getText(), normalFont));
        document.add(new Paragraph("Salle: " + cbSalle.getValue(), normalFont));
        document.add(new Paragraph(" "));

        PdfPTable statsTable = new PdfPTable(3);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingAfter(20);

        String[][] stats = {
                {"Réservations totales", lblTotalReservations.getText()},
                {"Durée moyenne", lblDureeMoyenne.getText()},
                {"Taux d'occupation", lblTauxOccupation.getText()}
        };

        for (String[] stat : stats) {
            PdfPCell cell = new PdfPCell(new Paragraph(stat[0], headerFont));
            cell.setBackgroundColor(new BaseColor(26, 35, 126));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            statsTable.addCell(cell);

            cell = new PdfPCell(new Paragraph(stat[1], statsFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(10);
            statsTable.addCell(cell);

            cell = new PdfPCell(new Paragraph(""));
            cell.setBorder(PdfPCell.NO_BORDER);
            statsTable.addCell(cell);
        }

        document.add(statsTable);
        document.add(new Paragraph("Salle la plus utilisée: " + lblSallePlusUtilisee.getText(), normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Les graphiques sont disponibles dans l'application.", normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("ConférenceApp - Système de Gestion de Réservation",
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC)));

        document.close();
    }

    @FXML
    private void handleExportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv")
        );
        fileChooser.setInitialFileName("rapport_" + currentPeriodeType.toLowerCase() + "_" +
                currentDateDebut.format(DateTimeFormatter.ISO_DATE) + ".csv");

        File file = fileChooser.showSaveDialog(chartRepartitionSalles.getScene().getWindow());

        if (file != null) {
            try (FileWriter fw = new FileWriter(file);
                 PrintWriter pw = new PrintWriter(fw)) {

                pw.println("Statistique,Valeur");
                pw.println("Periode," + lblPeriodeAffichee.getText());
                pw.println("Salle," + cbSalle.getValue());
                pw.println("Total reservations," + lblTotalReservations.getText());
                pw.println("Duree moyenne," + lblDureeMoyenne.getText());
                pw.println("Taux occupation," + lblTauxOccupation.getText());
                pw.println("Salle plus utilisee," + lblSallePlusUtilisee.getText());

                SessionManager.showInfo("Export CSV", "Données exportées avec succès !");

            } catch (Exception e) {
                SessionManager.showError("Erreur Export CSV", e.getMessage());
            }
        }
    }

    @FXML
    private void handleImprimer() {
        imprimerRapport();
    }

    private void imprimerRapport() {
        SwingUtilities.invokeLater(() -> {
            try {
                JTextArea textArea = new JTextArea();
                textArea.setFont(new java.awt.Font("Courier New", java.awt.Font.PLAIN, 10));

                StringBuilder sb = new StringBuilder();
                sb.append("=================================================\n");
                sb.append("         RAPPORT DE RÉSERVATIONS\n");
                sb.append("=================================================\n\n");
                sb.append("Généré le: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
                sb.append(lblPeriodeAffichee.getText()).append("\n");
                sb.append("Salle: ").append(cbSalle.getValue()).append("\n\n");

                sb.append("--- STATISTIQUES GLOBALES ---\n");
                sb.append("Total réservations: ").append(lblTotalReservations.getText()).append("\n");
                sb.append("Durée moyenne: ").append(lblDureeMoyenne.getText()).append("\n");
                sb.append("Taux d'occupation: ").append(lblTauxOccupation.getText()).append("\n");
                sb.append("Salle la plus utilisée: ").append(lblSallePlusUtilisee.getText()).append("\n\n");

                sb.append("Les graphiques sont disponibles dans l'application.\n\n");
                sb.append("=================================================\n");

                textArea.setText(sb.toString());
                textArea.print();

                Platform.runLater(() -> SessionManager.showInfo("Impression", "Impression terminée !"));

            } catch (PrinterException e) {
                Platform.runLater(() -> SessionManager.showError("Erreur d'impression", e.getMessage()));
            }
        });
    }
}