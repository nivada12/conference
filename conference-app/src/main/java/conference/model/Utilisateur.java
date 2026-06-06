package conference.model;

import java.time.LocalDateTime;

/**
 * Modèle Utilisateur
 */
public class Utilisateur {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String telephone;
    private Role role;
    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime derniereConnexion;

    public enum Role {
        ADMIN("Administrateur"),
        RESPONSABLE("Responsable"),
        UTILISATEUR("Utilisateur");
        
        private final String libelle;
        Role(String libelle) { this.libelle = libelle; }
        public String getLibelle() { return libelle; }
        
        @Override
        public String toString() { return libelle; }
    }

    public Utilisateur() {}
    
    public Utilisateur(int id, String nom, String prenom, String email, Role role) {
        this.id = id; this.nom = nom; this.prenom = prenom;
        this.email = email; this.role = role;
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public LocalDateTime getDerniereConnexion() { return derniereConnexion; }
    public void setDerniereConnexion(LocalDateTime derniereConnexion) { this.derniereConnexion = derniereConnexion; }
    
    @Override
    public String toString() { return getNomComplet() + " (" + email + ")"; }
}
