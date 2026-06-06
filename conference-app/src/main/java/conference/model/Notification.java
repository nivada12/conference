package conference.model;

import java.time.LocalDateTime;

/**
 * Modèle Notification
 */
public class Notification {
    private int id;
    private int utilisateurId;
    private Integer reservationId;
    private TypeNotification type;
    private String titre;
    private String message;
    private boolean lu;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateLecture;

    public enum TypeNotification {
        CONFIRMATION("Confirmation", "✅"),
        RAPPEL("Rappel", "⏰"),
        ANNULATION("Annulation", "❌"),
        ALERTE("Alerte", "⚠️"),
        FIN_SESSION("Fin de session", "🔔");
        
        private final String libelle;
        private final String icone;
        
        TypeNotification(String libelle, String icone) {
            this.libelle = libelle;
            this.icone = icone;
        }
        
        public String getLibelle() { return libelle; }
        public String getIcone() { return icone; }
        
        @Override
        public String toString() { return libelle; }
    }

    public Notification() {}

    public Notification(int utilisateurId, Integer reservationId, TypeNotification type, String titre, String message) {
        this.utilisateurId = utilisateurId;
        this.reservationId = reservationId;
        this.type = type;
        this.titre = titre;
        this.message = message;
        this.lu = false;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }
    public Integer getReservationId() { return reservationId; }
    public void setReservationId(Integer reservationId) { this.reservationId = reservationId; }
    public TypeNotification getType() { return type; }
    public void setType(TypeNotification type) { this.type = type; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
    public LocalDateTime getDateLecture() { return dateLecture; }
    public void setDateLecture(LocalDateTime dateLecture) { this.dateLecture = dateLecture; }
}
