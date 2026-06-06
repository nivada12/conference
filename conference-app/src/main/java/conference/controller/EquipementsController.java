package conference.controller;

import conference.dao.impl.EquipementDAO;
import conference.model.Equipement;
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

public class EquipementsController implements Initializable {

    @FXML private TableView<Equipement> tableEquipements;
    @FXML private TableColumn<Equipement, String> colNom;
    @FXML private TableColumn<Equipement, String> colType;
    @FXML private TableColumn<Equipement, String> colEtat;
    @FXML private TableColumn<Equipement, Integer> colQteTotale;
    @FXML private TableColumn<Equipement, Integer> colQteDisponible;
    @FXML private TableColumn<Equipement, String> colSerie;
    
    @FXML private VBox formPane;
    @FXML private TextField tfNom;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<Equipement.TypeEquipement> cbType;
    @FXML private ComboBox<Equipement.EtatEquipement> cbEtat;
    @FXML private TextField tfQteTotale;
    @FXML private TextField tfQteDisponible;
    @FXML private TextField tfNumeroSerie;
    @FXML private Label lblFormTitre;
    @FXML private ComboBox<String> cbFiltreType;
    @FXML private TextField tfRecherche;

    private final EquipementDAO equipementDAO = new EquipementDAO();
    private List<Equipement> tousEquipements;
    private Equipement equipementEnEdition = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupForm();
        setupFiltres();
        loadEquipements();
        if (formPane != null) formPane.setVisible(false);
    }

    private void setupTable() {
        if (colNom != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colType != null) colType.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getType() != null ? cd.getValue().getType().getLibelle() : ""));
        if (colEtat != null) {
            colEtat.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getEtat() != null ? cd.getValue().getEtat().getLibelle() : ""));
            colEtat.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || getIndex() >= getTableView().getItems().size()) { setText(null); setStyle(""); return; }
                    setText(item);
                    Equipement eq = getTableView().getItems().get(getIndex());
                    if (eq.getEtat() == Equipement.EtatEquipement.BON) setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    else if (eq.getEtat() == Equipement.EtatEquipement.DEFECTUEUX) setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;");
                }
            });
        }
        if (colQteTotale != null) colQteTotale.setCellValueFactory(new PropertyValueFactory<>("quantiteTotale"));
        if (colQteDisponible != null) colQteDisponible.setCellValueFactory(new PropertyValueFactory<>("quantiteDisponible"));
        if (colSerie != null) colSerie.setCellValueFactory(new PropertyValueFactory<>("numeroSerie"));
        
        if (tableEquipements != null) {
            tableEquipements.setRowFactory(tv -> {
                TableRow<Equipement> row = new TableRow<>();
                row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) handleEditer(row.getItem()); });
                return row;
            });
            
            ContextMenu menu = new ContextMenu();
            MenuItem miEditer = new MenuItem("✏ Modifier");
            MenuItem miSignaler = new MenuItem("⚠ Signaler défaut");
            miEditer.setOnAction(e -> { Equipement eq = tableEquipements.getSelectionModel().getSelectedItem(); if (eq != null) handleEditer(eq); });
            miSignaler.setOnAction(e -> { Equipement eq = tableEquipements.getSelectionModel().getSelectedItem(); if (eq != null) handleSignalerDefaut(eq); });
            menu.getItems().addAll(miEditer, new SeparatorMenuItem(), miSignaler);
            tableEquipements.setContextMenu(menu);
        }
    }

    private void setupForm() {
        if (cbType != null) { cbType.getItems().setAll(Equipement.TypeEquipement.values()); cbType.setValue(Equipement.TypeEquipement.VIDEO); }
        if (cbEtat != null) { cbEtat.getItems().setAll(Equipement.EtatEquipement.values()); cbEtat.setValue(Equipement.EtatEquipement.BON); }
    }

    private void setupFiltres() {
        if (cbFiltreType != null) {
            cbFiltreType.getItems().add("Tous les types");
            for (Equipement.TypeEquipement t : Equipement.TypeEquipement.values()) cbFiltreType.getItems().add(t.getLibelle());
            cbFiltreType.setValue("Tous les types");
            cbFiltreType.setOnAction(e -> filtrer());
        }
        if (tfRecherche != null) tfRecherche.setOnKeyReleased(e -> filtrer());
    }

    private void filtrer() {
        String recherche = tfRecherche != null ? tfRecherche.getText().toLowerCase().trim() : "";
        String typeFiltre = cbFiltreType != null ? cbFiltreType.getValue() : "Tous les types";
        
        tableEquipements.getItems().setAll(tousEquipements.stream()
            .filter(eq -> recherche.isEmpty() || eq.getNom().toLowerCase().contains(recherche))
            .filter(eq -> "Tous les types".equals(typeFiltre) || (eq.getType() != null && eq.getType().getLibelle().equals(typeFiltre)))
            .toList());
    }

    private void loadEquipements() {
        tousEquipements = equipementDAO.findAll();
        if (tableEquipements != null) tableEquipements.getItems().setAll(tousEquipements);
    }

    @FXML private void handleNouvelEquipement() {
        equipementEnEdition = null; clearForm();
        if (lblFormTitre != null) lblFormTitre.setText("Nouvel Équipement");
        if (formPane != null) formPane.setVisible(true);
    }

    private void handleEditer(Equipement eq) {
        equipementEnEdition = eq;
        if (lblFormTitre != null) lblFormTitre.setText("Modifier: " + eq.getNom());
        if (tfNom != null) tfNom.setText(eq.getNom());
        if (taDescription != null) taDescription.setText(eq.getDescription());
        if (cbType != null) cbType.setValue(eq.getType());
        if (cbEtat != null) cbEtat.setValue(eq.getEtat());
        if (tfQteTotale != null) tfQteTotale.setText(String.valueOf(eq.getQuantiteTotale()));
        if (tfQteDisponible != null) tfQteDisponible.setText(String.valueOf(eq.getQuantiteDisponible()));
        if (tfNumeroSerie != null) tfNumeroSerie.setText(eq.getNumeroSerie());
        if (formPane != null) formPane.setVisible(true);
    }

    private void handleSignalerDefaut(Equipement eq) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Signaler un défaut");
        dialog.setHeaderText("Équipement: " + eq.getNom());
        dialog.setContentText("Description du problème:");
        dialog.showAndWait().ifPresent(desc -> {
            if (desc.trim().isEmpty()) { SessionManager.showError("Erreur", "Description obligatoire."); return; }
            if (equipementDAO.signalerDefaut(eq.getId(), SessionManager.getInstance().getUserId(), null, desc)) {
                SessionManager.showInfo("Signalement", "Défaut signalé avec succès.");
                loadEquipements();
            }
        });
    }

    @FXML private void handleSauvegarder() {
        if (tfNom == null || tfNom.getText().trim().isEmpty()) { SessionManager.showError("Validation", "Nom obligatoire."); return; }
        
        Equipement eq = equipementEnEdition != null ? equipementEnEdition : new Equipement();
        eq.setNom(tfNom.getText().trim());
        eq.setDescription(taDescription != null ? taDescription.getText() : "");
        if (cbType != null) eq.setType(cbType.getValue());
        if (cbEtat != null) eq.setEtat(cbEtat.getValue());
        try { eq.setQuantiteTotale(Integer.parseInt(tfQteTotale != null ? tfQteTotale.getText() : "1")); } catch (Exception ignored) { eq.setQuantiteTotale(1); }
        try { eq.setQuantiteDisponible(Integer.parseInt(tfQteDisponible != null ? tfQteDisponible.getText() : "1")); } catch (Exception ignored) { eq.setQuantiteDisponible(1); }
        if (tfNumeroSerie != null) eq.setNumeroSerie(tfNumeroSerie.getText());
        eq.setActif(true);
        
        boolean succes = equipementEnEdition != null ? equipementDAO.update(eq) : equipementDAO.save(eq);
        if (succes) { SessionManager.showInfo("Succès", "Équipement sauvegardé."); if (formPane != null) formPane.setVisible(false); loadEquipements(); }
        else SessionManager.showError("Erreur", "Impossible de sauvegarder l'équipement.");
    }

    @FXML private void handleAnnulerForm() { if (formPane != null) formPane.setVisible(false); }
    @FXML private void handleRafraichir() { loadEquipements(); }

    private void clearForm() {
        if (tfNom != null) tfNom.clear();
        if (taDescription != null) taDescription.clear();
        if (cbType != null) cbType.setValue(Equipement.TypeEquipement.VIDEO);
        if (cbEtat != null) cbEtat.setValue(Equipement.EtatEquipement.BON);
        if (tfQteTotale != null) tfQteTotale.setText("1");
        if (tfQteDisponible != null) tfQteDisponible.setText("1");
        if (tfNumeroSerie != null) tfNumeroSerie.clear();
    }
}
