package conference.controller;

import conference.dao.impl.SalleDAO;
import conference.model.Salle;
import conference.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur de gestion des salles
 */
public class SallesController implements Initializable {

    @FXML private TableView<Salle> tableSalles;
    @FXML private TableColumn<Salle, String> colNom;
    @FXML private TableColumn<Salle, Integer> colCapacite;
    @FXML private TableColumn<Salle, String> colLocalisation;
    @FXML private TableColumn<Salle, String> colEtage;
    @FXML private TableColumn<Salle, String> colDisposition;
    @FXML private TableColumn<Salle, String> colStatut;
    
    @FXML private VBox formPane;
    @FXML private TextField tfNom;
    @FXML private TextArea taDescription;
    @FXML private TextField tfCapacite;
    @FXML private TextField tfLocalisation;
    @FXML private TextField tfEtage;
    @FXML private TextField tfSuperficie;
    @FXML private ComboBox<Salle.Disposition> cbDisposition;
    @FXML private CheckBox chkActif;
    @FXML private Label lblFormTitre;
    
    @FXML private TextField tfRecherche;

    private final SalleDAO salleDAO = new SalleDAO();
    private List<Salle> toutesLesSalles;
    private Salle salleEnEdition = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupForm();
        loadSalles();
        if (formPane != null) formPane.setVisible(false);
        if (tfRecherche != null) tfRecherche.setOnKeyReleased(e -> filtrer());
    }

    private void setupTable() {
        if (colNom != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colCapacite != null) colCapacite.setCellValueFactory(new PropertyValueFactory<>("capacite"));
        if (colLocalisation != null) colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        if (colEtage != null) colEtage.setCellValueFactory(new PropertyValueFactory<>("etage"));
        if (colDisposition != null) colDisposition.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getDispositionDefaut() != null ?
                cd.getValue().getDispositionDefaut().getLibelle() : ""));
        if (colStatut != null) colStatut.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().isActif() ? "✅ Active" : "❌ Inactive"));
        
        if (tableSalles != null) {
            tableSalles.setRowFactory(tv -> {
                TableRow<Salle> row = new TableRow<>();
                row.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2 && !row.isEmpty()) handleEditer(row.getItem());
                });
                return row;
            });
            
            ContextMenu menu = new ContextMenu();
            MenuItem miEditer = new MenuItem("✏ Modifier");
            MenuItem miSupprimer = new MenuItem("🗑 Désactiver");
            miEditer.setOnAction(e -> { Salle s = tableSalles.getSelectionModel().getSelectedItem(); if (s != null) handleEditer(s); });
            miSupprimer.setOnAction(e -> { Salle s = tableSalles.getSelectionModel().getSelectedItem(); if (s != null) handleSupprimer(s); });
            menu.getItems().addAll(miEditer, new SeparatorMenuItem(), miSupprimer);
            tableSalles.setContextMenu(menu);
        }
    }

    private void setupForm() {
        if (cbDisposition != null) {
            cbDisposition.getItems().setAll(Salle.Disposition.values());
            cbDisposition.setValue(Salle.Disposition.CONFERENCE);
        }
        if (chkActif != null) chkActif.setSelected(true);
    }

    private void loadSalles() {
        toutesLesSalles = salleDAO.findAll();
        if (tableSalles != null) tableSalles.getItems().setAll(toutesLesSalles);
    }

    private void filtrer() {
        String recherche = tfRecherche.getText().toLowerCase().trim();
        if (recherche.isEmpty()) {
            tableSalles.getItems().setAll(toutesLesSalles);
        } else {
            tableSalles.getItems().setAll(
                toutesLesSalles.stream()
                    .filter(s -> s.getNom().toLowerCase().contains(recherche) ||
                                 (s.getLocalisation() != null && s.getLocalisation().toLowerCase().contains(recherche)))
                    .toList()
            );
        }
    }

    @FXML private void handleNouvelleSalle() {
        salleEnEdition = null;
        clearForm();
        if (lblFormTitre != null) lblFormTitre.setText("Nouvelle Salle");
        if (formPane != null) formPane.setVisible(true);
    }

    private void handleEditer(Salle s) {
        salleEnEdition = s;
        if (lblFormTitre != null) lblFormTitre.setText("Modifier: " + s.getNom());
        if (tfNom != null) tfNom.setText(s.getNom());
        if (taDescription != null) taDescription.setText(s.getDescription());
        if (tfCapacite != null) tfCapacite.setText(String.valueOf(s.getCapacite()));
        if (tfLocalisation != null) tfLocalisation.setText(s.getLocalisation());
        if (tfEtage != null) tfEtage.setText(s.getEtage());
        if (tfSuperficie != null) tfSuperficie.setText(String.valueOf(s.getSuperficie()));
        if (cbDisposition != null) cbDisposition.setValue(s.getDispositionDefaut());
        if (chkActif != null) chkActif.setSelected(s.isActif());
        if (formPane != null) formPane.setVisible(true);
    }

    private void handleSupprimer(Salle s) {
        if (SessionManager.showConfirm("Désactiver la salle", "Désactiver la salle \"" + s.getNom() + "\" ?")) {
            if (salleDAO.delete(s.getId())) {
                SessionManager.showInfo("Succès", "Salle désactivée.");
                loadSalles();
            }
        }
    }

    @FXML private void handleSauvegarder() {
        if (tfNom == null || tfNom.getText().trim().isEmpty()) {
            SessionManager.showError("Validation", "Le nom de la salle est obligatoire."); return;
        }
        int capacite = 0;
        try { capacite = Integer.parseInt(tfCapacite != null ? tfCapacite.getText() : "0"); }
        catch (NumberFormatException e) { SessionManager.showError("Validation", "Capacité invalide."); return; }
        
        Salle s = salleEnEdition != null ? salleEnEdition : new Salle();
        s.setNom(tfNom.getText().trim());
        s.setDescription(taDescription != null ? taDescription.getText() : "");
        s.setCapacite(capacite);
        s.setLocalisation(tfLocalisation != null ? tfLocalisation.getText() : "");
        s.setEtage(tfEtage != null ? tfEtage.getText() : "");
        try { s.setSuperficie(Double.parseDouble(tfSuperficie != null ? tfSuperficie.getText() : "0")); } catch (Exception ignored) {}
        if (cbDisposition != null) s.setDispositionDefaut(cbDisposition.getValue());
        s.setActif(chkActif == null || chkActif.isSelected());
        
        boolean succes = salleEnEdition != null ? salleDAO.update(s) : salleDAO.save(s);
        if (succes) {
            SessionManager.showInfo("Succès", salleEnEdition != null ? "Salle modifiée." : "Salle créée.");
            if (formPane != null) formPane.setVisible(false);
            loadSalles();
        } else {
            SessionManager.showError("Erreur", "Impossible de sauvegarder la salle.");
        }
    }

    @FXML private void handleAnnulerForm() { if (formPane != null) formPane.setVisible(false); }
    @FXML private void handleRafraichir() { loadSalles(); }

    private void clearForm() {
        if (tfNom != null) tfNom.clear();
        if (taDescription != null) taDescription.clear();
        if (tfCapacite != null) tfCapacite.clear();
        if (tfLocalisation != null) tfLocalisation.clear();
        if (tfEtage != null) tfEtage.clear();
        if (tfSuperficie != null) tfSuperficie.clear();
        if (cbDisposition != null) cbDisposition.setValue(Salle.Disposition.CONFERENCE);
        if (chkActif != null) chkActif.setSelected(true);
    }
}
