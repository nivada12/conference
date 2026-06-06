package conference.model;

import java.time.LocalDate;

/**
 * Modèle Équipement
 */
public class Equipement {
    private int id;
    private String nom;
    private String description;
    private TypeEquipement type;
    private int quantiteTotale;
    private int quantiteDisponible;
    private EtatEquipement etat;
    private String numeroSerie;
    private LocalDate dateAcquisition;
    private boolean actif;
    private int quantiteReservee; // Pour l'affichage lors d'une réservation

    public enum TypeEquipement {
        AUDIO("Audio"),
        VIDEO("Vidéo"),
        INFORMATIQUE("Informatique"),
        MOBILIER("Mobilier"),
        AUTRE("Autre");
        
        private final String libelle;
        TypeEquipement(String libelle) { this.libelle = libelle; }
        public String getLibelle() { return libelle; }
        
        @Override
        public String toString() { return libelle; }
    }

    public enum EtatEquipement {
        BON("Bon état"),
        DEFECTUEUX("Défectueux"),
        EN_REPARATION("En réparation");
        
        private final String libelle;
        EtatEquipement(String libelle) { this.libelle = libelle; }
        public String getLibelle() { return libelle; }
        
        @Override
        public String toString() { return libelle; }
    }

    public Equipement() {}

    public Equipement(int id, String nom, TypeEquipement type) {
        this.id = id; this.nom = nom; this.type = type;
    }

    @Override
    public String toString() {
        return nom + " (" + type.getLibelle() + ") - Dispo: " + quantiteDisponible + "/" + quantiteTotale;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TypeEquipement getType() { return type; }
    public void setType(TypeEquipement type) { this.type = type; }
    public int getQuantiteTotale() { return quantiteTotale; }
    public void setQuantiteTotale(int quantiteTotale) { this.quantiteTotale = quantiteTotale; }
    public int getQuantiteDisponible() { return quantiteDisponible; }
    public void setQuantiteDisponible(int quantiteDisponible) { this.quantiteDisponible = quantiteDisponible; }
    public EtatEquipement getEtat() { return etat; }
    public void setEtat(EtatEquipement etat) { this.etat = etat; }
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public LocalDate getDateAcquisition() { return dateAcquisition; }
    public void setDateAcquisition(LocalDate dateAcquisition) { this.dateAcquisition = dateAcquisition; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public int getQuantiteReservee() { return quantiteReservee; }
    public void setQuantiteReservee(int quantiteReservee) { this.quantiteReservee = quantiteReservee; }
}
