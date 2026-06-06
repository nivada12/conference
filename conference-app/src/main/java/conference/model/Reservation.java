package conference.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle Réservation
 */
public class Reservation {
    private int id;
    private int utilisateurId;
    private int salleId;
    private String titre;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private int nombreParticipants;
    private Salle.Disposition disposition;
    private StatutReservation statut;
    private String commentaires;
    private String codeReservation;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
    // Relations joinées
    private Salle salle;
    private Utilisateur utilisateur;
    private List<Equipement> equipements;

    public enum StatutReservation {
        EN_ATTENTE("En attente", "#FFA500"),
        CONFIRME("Confirmé", "#4CAF50"),
        ANNULE("Annulé", "#F44336"),
        TERMINE("Terminé", "#9E9E9E");
        
        private final String libelle;
        private final String couleur;
        
        StatutReservation(String libelle, String couleur) {
            this.libelle = libelle;
            this.couleur = couleur;
        }
        
        public String getLibelle() { return libelle; }
        public String getCouleur() { return couleur; }
        
        @Override
        public String toString() { return libelle; }
    }

    public Reservation() {
        this.equipements = new ArrayList<>();
        this.statut = StatutReservation.EN_ATTENTE;
    }

    public long getDureeEnMinutes() {
        if (dateDebut != null && dateFin != null) {
            return ChronoUnit.MINUTES.between(dateDebut, dateFin);
        }
        return 0;
    }

    public String getDureeFormatee() {
        long minutes = getDureeEnMinutes();
        long heures = minutes / 60;
        long mins = minutes % 60;
        if (heures > 0) {
            return heures + "h" + (mins > 0 ? String.format("%02d", mins) : "00");
        }
        return mins + " min";
    }

    public boolean isEnConflitAvec(Reservation autre) {
        if (this.salleId != autre.salleId) return false;
        return !(this.dateFin.isBefore(autre.dateDebut) || this.dateDebut.isAfter(autre.dateFin));
    }

    @Override
    public String toString() {
        return codeReservation + " - " + titre;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }
    public int getSalleId() { return salleId; }
    public void setSalleId(int salleId) { this.salleId = salleId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    public int getNombreParticipants() { return nombreParticipants; }
    public void setNombreParticipants(int nombreParticipants) { this.nombreParticipants = nombreParticipants; }
    public Salle.Disposition getDisposition() { return disposition; }
    public void setDisposition(Salle.Disposition disposition) { this.disposition = disposition; }
    public StatutReservation getStatut() { return statut; }
    public void setStatut(StatutReservation statut) { this.statut = statut; }
    public String getCommentaires() { return commentaires; }
    public void setCommentaires(String commentaires) { this.commentaires = commentaires; }
    public String getCodeReservation() { return codeReservation; }
    public void setCodeReservation(String codeReservation) { this.codeReservation = codeReservation; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
    public Salle getSalle() { return salle; }
    public void setSalle(Salle salle) { this.salle = salle; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }
    public List<Equipement> getEquipements() { return equipements; }
    public void setEquipements(List<Equipement> equipements) { this.equipements = equipements; }
}
