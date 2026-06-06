package conference.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle Salle de Conférence
 */
public class Salle {
    private int id;
    private String nom;
    private String description;
    private int capacite;
    private String localisation;
    private String etage;
    private double superficie;
    private Disposition dispositionDefaut;
    private boolean actif;
    private String imagePath;
    private LocalDateTime dateCreation;
    private List<Equipement> equipements;

    public enum Disposition {
        THEATRE("Théâtre"),
        CLASSE("En classe"),
        CONFERENCE("Conférence"),
        COCKTAIL("Cocktail/Debout"),
        CABARET("Cabaret");
        
        private final String libelle;
        Disposition(String libelle) { this.libelle = libelle; }
        public String getLibelle() { return libelle; }
        
        @Override
        public String toString() { return libelle; }
    }

    public Salle() {
        this.equipements = new ArrayList<>();
    }

    public Salle(int id, String nom, int capacite, String localisation) {
        this();
        this.id = id; this.nom = nom;
        this.capacite = capacite; this.localisation = localisation;
    }

    @Override
    public String toString() {
        return nom + " (Cap: " + capacite + ") - " + localisation;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }
    public String getEtage() { return etage; }
    public void setEtage(String etage) { this.etage = etage; }
    public double getSuperficie() { return superficie; }
    public void setSuperficie(double superficie) { this.superficie = superficie; }
    public Disposition getDispositionDefaut() { return dispositionDefaut; }
    public void setDispositionDefaut(Disposition dispositionDefaut) { this.dispositionDefaut = dispositionDefaut; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public List<Equipement> getEquipements() { return equipements; }
    public void setEquipements(List<Equipement> equipements) { this.equipements = equipements; }
}
